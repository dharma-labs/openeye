package com.example.openmind.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.openmind.BuildConfig
import com.example.openmind.MainActivity
import com.example.openmind.R
import com.example.openmind.data.AppRepository
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.ScreenTimeMeter
import com.example.openmind.data.Settings
import com.example.openmind.data.Surface
import com.example.openmind.data.UsageStatsRepository
import com.example.openmind.domain.BypassStore
import com.example.openmind.domain.RuleEngine
import com.example.openmind.domain.Voice
import com.example.openmind.ui.BlockActivity
import java.time.LocalDate
import java.util.ArrayDeque

/**
 * Two-layer shield plus a live counter.
 *
 * 1. Hiding: covers Reels / Explore / Shorts navigation items and YouTube Shorts shelves with
 *    opaque accessibility overlays measured from the live node tree.
 * 2. Blocking: if a surface is opened another way, presses Back and shows the block screen.
 * 3. Counting: while a vertical video surface is active, counts time and estimated views and
 *    shows a small non-touchable HUD in the top corner.
 */
class GuardAccessibilityService : AccessibilityService() {

    private lateinit var overlays: HidingOverlayController
    private lateinit var hud: WatchHudController
    private lateinit var nudge: WatchNudgeController
    private lateinit var tracker: WatchSessionTracker

    private val handler = Handler(Looper.getMainLooper())
    private var tickerRunning = false

    private var currentPackage: String? = null
    private var currentActivityClass: String? = null
    private var foregroundPlatform: String? = null
    private var lastEvaluation = 0L
    private var lastSurfaceId: String? = null
    private var lastSurfaceSeen = 0L
    private var lastLoggedTargets: String? = null
    private var signatureLabels: Set<String>? = null
    private var signatureRow: Int? = null
    private var signatureSurface: String? = null
    private var hudDay = -1L
    private var hudCount = 0
    private var hudMillis = 0L
    private var nudgeAtMillis = 0L
    private var nudgeCursor = 0
    private var screenMinutes = 0
    private var screenMinutesAt = 0L
    private var screenMinutesPlatform: String? = null
    private var focusLostAt = 0L
    private val intrusions = ArrayDeque<Long>()
    private var handsOffUntil = 0L
    private val lastBlockAt = mutableMapOf<String, Long>()
    private val lastLimitCheckAt = mutableMapOf<String, Long>()
    private val lastLimitBlockAt = mutableMapOf<String, Long>()
    private val warnedLimits = mutableSetOf<String>()
    private var warnedDay = -1L
    private lateinit var usageRepository: UsageStatsRepository
    private var ambient: AmbientTimerController? = null
    private var ambientRunning = false
    private var ambientUsageOk = false
    private var ambientUsageCheckedAt = 0L
    private var ambientCheckedAt = 0L
    private var ambientPending = false

    private val ambientRetry = object : Runnable {
        override fun run() {
            ambientPending = false
            refreshAmbient(force = true)
        }
    }

    /**
     * The corner timer redraws once a minute because that is how often its only digit changes, and
     * it is not posted at all while the timer is off screen. Nothing here polls a feed.
     */
    private val ambientTick = object : Runnable {
        override fun run() {
            if (!ambientRunning) return
            refreshAmbient(force = true)
            handler.postDelayed(this, AMBIENT_TICK_MS)
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    ScreenTimeMeter.onScreenOn()
                    handler.post { refreshAmbient(force = true) }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Count up to here, then stop. A dark screen has nothing to show and nothing to
                    // measure, so the minute loop goes away with it.
                    ScreenTimeMeter.onScreenOff()
                    handler.post { setAmbientDown() }
                }
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!tickerRunning) return
            // One window pass per second, shared by everything below. Asking the window manager
            // twice on the same tick is the kind of work that shows up as battery drain.
            val owner = if (foregroundPlatform == null) null else foregroundWindowPackage()
            if (foregroundPlatform == null || feedStillOwnsTheScreen(owner)) {
                if (tracker.active) {
                    verifySurface(owner)
                    if (tracker.active) tracker.tick()
                }
                refreshHud()
                maybeNudge()
            }
            if (tickerRunning) handler.postDelayed(this, TICK_MS)
        }
    }

    /**
     * Once a second, confirm the shielded app is still the window on screen.
     *
     * A clip can play for a minute without the app emitting a single accessibility event, and an app
     * switch sometimes emits none that reaches us either. Asking the window manager instead of
     * waiting for an event is what lets continuous watching stay one continuous session - and what
     * takes the counter off the screen the moment something else is in front.
     */
    private fun feedStillOwnsTheScreen(owner: String?): Boolean {
        val platform = foregroundPlatform ?: run { focusLostAt = 0L; return true }
        // A feed can be entered without a window-state event reaching us - resuming from our own
        // block screen does exactly that. Deriving the package from the platform keeps the counter
        // alive instead of freezing on a stale focus reading.
        val pkg = currentPackage ?: PlatformCatalog.packages.entries
            .firstOrNull { it.value == platform }?.key ?: run { focusLostAt = 0L; return true }
        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) {
            leaveTargetApp()
            return false
        }
        if (owner == null || owner == pkg) {
            focusLostAt = 0L
            return true
        }
        hud.hide()
        nudge.removeNow()
        overlays.clear()
        val now = SystemClock.elapsedRealtime()
        if (focusLostAt == 0L) focusLostAt = now
        if (now - focusLostAt < FOCUS_LOST_MS) return false
        if (tracker.active) {
            // Close the session but keep it resumable: the seconds another window borrowed are not
            // counted as watching, and if the feed takes the screen back the numbers continue.
            tracker.leave()
            return false
        }
        if (tracker.holdOpen(now)) return false
        leaveTargetApp()
        return false
    }

    /**
     * Once a second, ask the window tree which screen is actually up.
     *
     * Reaching the home feed by pressing Back can leave the app with no further events to evaluate,
     * and the session would go on counting minutes the user never spent in a clip feed. The tree is
     * the only witness when the app has gone quiet.
     */
    private fun verifySurface(owner: String?) {
        val platformId = foregroundPlatform ?: return
        val pkg = currentPackage ?: return
        // While the app is talking to us the event path already measures the screen four or five
        // times a second. Reading the whole tree again on the same tick is work for nothing.
        val wallNow = System.currentTimeMillis()
        if (wallNow - lastEvaluation < VERIFY_AFTER_QUIET_MS) return
        val root = activeRootFor(pkg, null) ?: return
        val nodes = collectNodes(root)
        val pager = hasClipPager(nodes)
        val surfaceId = detectSurface(platformId, null, nodes, pager)
        if (surfaceId != null) {
            lastSurfaceId = surfaceId
            lastSurfaceSeen = wallNow
            return
        }
        if (feedClosed(pkg, owner, nodes, pager, wallNow)) {
            if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "leave surface (tick)")
            tracker.leave()
            lastSurfaceId = null
        }
    }

    /**
     * The package owning the screen, ignoring system panels.
     *
     * The volume slider, the shade and the screen-recorder dot are windows too, but they are never
     * an application window and never own the screen. Treating one of them as the foreground is what
     * made a volume key look like leaving the app and restarted the counter. Null means the query
     * itself failed, which is treated as "still there": losing a session on an OEM quirk would be
     * worse than the second it costs.
     */
    private fun foregroundWindowPackage(): String? = runCatching {
        val apps = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val window = apps.firstOrNull { it.isActive || it.isFocused } ?: apps.maxByOrNull { it.layer }
        window?.root?.packageName?.toString()
    }.getOrNull()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlays = HidingOverlayController(this, getSystemService(WindowManager::class.java))
        hud = WatchHudController(this, getSystemService(WindowManager::class.java))
        nudge = WatchNudgeController(this, getSystemService(WindowManager::class.java))
        tracker = WatchSessionTracker { slice ->
            AppRepository.get(this).addWatch(
                platformId = slice.platformId,
                surfaceId = slice.surfaceId,
                count = slice.count,
                millis = slice.millis,
                sessions = slice.sessions,
                longestSessionMs = slice.longestSessionMs,
                touches = slice.touches,
                hour = slice.hour,
            )
            // The home-screen widget reads the same records; a flush is the moment its minute
            // actually changes, so it waits for nothing. Nothing else forces an update, and with no
            // widget placed this returns after one cheap lookup.
            com.example.openmind.widget.OpeneyeWidget.updateAll(this, force = true)
        }
        usageRepository = UsageStatsRepository(this)
        ambient = AmbientTimerController(this, getSystemService(WindowManager::class.java))
        // The screen state is what the whole-phone number is measured against, so the meter has to
        // know which side of it we are on from the first second, not from the next unlock.
        if (getSystemService(PowerManager::class.java)?.isInteractive == true) ScreenTimeMeter.onScreenOn()
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::overlays.isInitialized) overlays.clear()
        if (::hud.isInitialized) hud.hide()
        if (::nudge.isInitialized) nudge.removeNow()
        // Rotation moves the status bar inset the corner timer is measured from. Down, then back
        // up at the new size on the next window event rather than leaving it over the notch.
        setAmbientDown()
        lastEvaluation = 0L
    }

    override fun onDestroy() {
        leaveTargetApp()
        setAmbientDown()
        runCatching { unregisterReceiver(screenStateReceiver) }
        ambient = null
        if (::overlays.isInitialized) overlays.clear()
        if (instance === this) instance = null
        super.onDestroy()
    }

    /**
     * The system asks for an interrupt when its own event queue backs up - a volume-key burst does
     * that. It plays no part in what we measure, and tearing the session down here is what made the
     * counter vanish and start again while the feed was still on screen. We generate no sound, so
     * there is nothing to stop.
     */
    override fun onInterrupt() {
        if (BuildConfig.DEBUG) Log.d("OpeneyeFocus", "interrupt ignored")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::overlays.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        val type = event.eventType

        // Every app that takes the screen, ours included, is a reason to put the corner timer up or
        // take it down. One call, decided from the window that actually owns the screen.
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) refreshAmbient()

        // Our own overlay windows emit events. Ignore them, except when the block screen opens.
        if (pkg == packageName) {
            val cls = event.className?.toString().orEmpty()
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (cls.contains("BlockActivity")) {
                    if (::tracker.isInitialized) tracker.leave()
                    foregroundPlatform = null
                    stopTicker()
                    if (::hud.isInitialized) hud.hide()
                    if (::nudge.isInitialized) nudge.removeNow()
                    if (::overlays.isInitialized) overlays.clear()
                }
                // Coming back to our own screens is leaving the feed. Without this the HUD from
                // YouTube stays painted over the app, which reads as a stuck overlay.
                if (cls.contains("MainActivity")) leaveTargetApp()
            }
            return
        }

        if (pkg !in PlatformCatalog.packages) {
            if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            if (foregroundPlatform == null) return
            // A panel painted over the feed - the volume HUD, the shade, the screen-recorder dot -
            // reports a window change without owning the screen. Ending the session here is what
            // made the counter restart on every volume key.
            val owner = foregroundWindowPackage()
            if (owner == null || owner == currentPackage) return
            // Something else owns the window now. Take our layers off it at once; the 1 s check
            // decides whether the session is really over or this was only a transition.
            hud.hide()
            nudge.removeNow()
            overlays.clear()
            if (focusLostAt == 0L) focusLostAt = SystemClock.elapsedRealtime()
            return
        }

        val platformId = PlatformCatalog.packages[pkg] ?: return
        // Events keep arriving from an app that is playing in the background, so coming back is
        // proved by the window rather than by the arrival of an event.
        if (focusLostAt != 0L && foregroundWindowPackage() == pkg) focusLostAt = 0L
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentPackage = pkg
            currentActivityClass = event.className?.toString()
            if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "window class=${event.className}")
        }
        foregroundPlatform = platformId
        startTicker()

        val now = System.currentTimeMillis()
        // Count taps before throttling so quick taps are not dropped. The tracker ignores them
        // outside a live session, so no grace window is needed here.
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED || type == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            tracker.noteTouch()
        }
        val windowChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (!windowChange && now - lastEvaluation < EVALUATION_INTERVAL_MS) return
        lastEvaluation = now

        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) {
            leaveTargetApp()
            return
        }

        val repo = AppRepository.get(this)
        val settings = repo.settings.value
        if (!settings.masterEnabled) {
            overlays.clear()
            tracker.leave()
            // Switching the shield off must not leave a session open to be resumed a moment later.
            tracker.clearHold()
            foregroundPlatform = null
            stopTicker()
            hud.hide()
            nudge.removeNow()
            return
        }

        if (!settings.trackingEnabled) {
            tracker.leave()
            tracker.clearHold()
            stopTicker()
            hud.hide()
        }

        val root = activeRootFor(pkg, event.source)
        if (root == null) {
            // No tree means nothing to measure this frame. Keep the session: on devices where an
            // overlay or the launcher answers for the active window, aborting here stopped the
            // counter from ever moving. The 1 s focus check closes it if the app really is gone.
            refreshHud()
            return
        }

        val nodes = collectNodes(root)
        val hideTargets = buildHideTargets(platformId, settings, nodes)
        if (BuildConfig.DEBUG) {
            val signature = hideTargets.joinToString("|") { "${it.key}@${it.bounds}" }
            if (signature != lastLoggedTargets) {
                Log.d("OpeneyeHiding", if (hideTargets.isEmpty()) "no targets for $platformId" else signature)
                lastLoggedTargets = signature
            }
        }
        overlays.sync(hideTargets)

        if (enforceDailyLimit(platformId, settings, now)) return

        val pager = hasClipPager(nodes)
        val surfaceId = detectSurface(platformId, event, nodes, pager)
        if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "detect class=$currentActivityClass event=${event.className} -> $surfaceId")
        if (surfaceId != null) {
            lastSurfaceId = surfaceId
            lastSurfaceSeen = now
            tracker.enter(platformId, surfaceId)
        } else if (tracker.active && lastSurfaceId != null &&
            feedClosed(pkg, foregroundWindowPackage(), nodes, pager, now)
        ) {
            if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "leave surface")
            tracker.leave()
            lastSurfaceId = null
        }

        val activeSurface = surfaceId ?: lastSurfaceId?.takeIf { tracker.active }

        // While the keyboard is up the visible sheet is comments, not the feed: its scrolling must
        // not be counted as paging.
        if (activeSurface != null && tracker.active && !imeVisible()) {
            if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && isPagerScroll(event)) {
                tracker.noteScroll()
            }
            maybeCountContentChange(activeSurface, nodes)
        }

        if (surfaceId != null) enforce(platformId, surfaceId, settings, now)

        refreshHud()
    }

    /**
     * Whether the clip feed is provably gone, as opposed to merely hard to name.
     *
     * A frame that fails detection is not evidence of leaving - that is what an app update does to
     * an activity class name, and it used to restart the counter mid-scroll. Only focus moving away,
     * the bottom bar coming back, or a tree with no pager in it ends the session.
     */
    private fun feedClosed(
        pkg: String,
        owner: String?,
        nodes: List<AccessibilityNodeInfo>,
        pager: Boolean,
        now: Long,
    ): Boolean {
        val stale = now - lastSurfaceSeen
        if (stale <= SURFACE_GRACE_MS) return false
        if (owner != null && owner != pkg) return true
        if (hasBottomLabel(nodes, "home")) return true
        return !pager && stale > SURFACE_STALE_MS
    }

    // ---------------------------------------------------------------- hiding

    private fun buildHideTargets(
        platformId: String,
        settings: Settings,
        nodes: List<AccessibilityNodeInfo>,
    ): List<HideTarget> {
        val config = settings.platforms[platformId] ?: return emptyList()
        if (!config.enabled) return emptyList()
        if (System.currentTimeMillis() < handsOffUntil) return emptyList()

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val systemNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val night = when (settings.themeMode) {
            "light" -> false
            "dark" -> true
            else -> systemNight
        }
        val coverColor = if (night) Color.rgb(10, 10, 11) else Color.WHITE

        fun shielded(surfaceId: String): Boolean {
            val mode = config.surfaces[surfaceId]?.mode ?: BlockMode.OFF
            if (mode == BlockMode.OFF) return false
            return BypassStore.until(platformId, surfaceId) == null
        }

        val targets = mutableListOf<HideTarget>()
        val platform = PlatformCatalog.byId(platformId) ?: return emptyList()

        // The entry point itself: cover the Reels / Explore / Shorts button in the bottom bar.
        platform.surfaces.forEach { surface ->
            if (!surface.hideNav) return@forEach
            if (surface.navTokens.isEmpty() && surface.navIdHints.isEmpty()) return@forEach
            if (!shielded(surface.id)) return@forEach
            val best = nodes.asSequence()
                .filter { node ->
                    surface.navTokens.any { navLabelMatches(node, it) } ||
                        surface.navIdHints.any { navIdMatches(node, it, screenHeight) }
                }
                .mapNotNull { navItemBounds(it, screenWidth, screenHeight) }
                .maxByOrNull { it.width().toLong() * it.height() }
            if (best != null) {
                targets += HideTarget(
                    key = "${platformId}_${surface.id}_nav",
                    bounds = best,
                    color = coverColor,
                )
            }
        }

        // Horizontal clip shelves rendered inside the home feed.
        platform.surfaces.filter { it.shelf }.forEach { surface ->
            if (!shielded(surface.id)) return@forEach
            val alreadyFullScreen = surface.classHints.any {
                currentActivityClass?.lowercase()?.contains(it) == true
            }
            if (alreadyFullScreen) return@forEach
            val shelf = nodes.asSequence()
                .filter { shelfLabel(it, surface) || shelfIdMatches(it, surface, screenHeight) }
                .mapNotNull { shelfBounds(it, screenWidth, screenHeight) }
                .maxByOrNull { it.height() }
            if (shelf != null) {
                targets += HideTarget(
                    key = "${platformId}_${surface.id}_shelf",
                    bounds = shelf,
                    color = coverColor,
                )
            }
        }

        return targets
    }

    /** More forgiving than a caption match: short labels such as "Reels, tab 3 of 5". */
    private fun navLabelMatches(node: AccessibilityNodeInfo, token: String): Boolean {
        val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            ?: return false
        if (!label.contains(token)) return false
        if (label.length > token.length + 24) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.width() > 0 && rect.height() > 0
    }

    private fun labelMatches(node: AccessibilityNodeInfo, token: String): Boolean {
        val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            ?: return false
        if (!label.startsWith(token)) return false
        if (label.length > token.length && label[token.length].isLetterOrDigit()) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.width() > 0 && rect.height() > 0
    }

    private fun shelfLabel(node: AccessibilityNodeInfo, surface: Surface): Boolean {
        val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            ?: return false
        return surface.navTokens.any { it == label || "$it shelf" == label }
    }

    private fun navIdMatches(node: AccessibilityNodeInfo, hint: String, screenHeight: Int): Boolean {
        val id = node.viewIdResourceName?.lowercase() ?: return false
        if (id.isBlank() || !id.contains(hint)) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.bottom >= screenHeight - dp(210) &&
            rect.height() in dp(24)..dp(180) &&
            rect.width() > 0
    }

    private fun shelfIdMatches(node: AccessibilityNodeInfo, surface: Surface, screenHeight: Int): Boolean {
        val id = node.viewIdResourceName?.lowercase() ?: return false
        if (id.isBlank() || surface.idHints.none { id.contains(it) }) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.bottom <= screenHeight - dp(150)
    }

    /** Climbs from a label/icon to the complete tab item, never to the whole bar. */
    private fun navItemBounds(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
    ): Rect? {
        val minWidth = dp(28f)
        val maxWidth = (screenWidth * 0.42f).toInt()
        val minHeight = dp(32f)
        val maxHeight = dp(170f)
        val bottomZone = screenHeight - dp(210)

        var current: AccessibilityNodeInfo? = node
        var best: Rect? = null
        var bestScore = -1L
        var depth = 0
        while (current != null && depth < 7) {
            val rect = Rect()
            current.getBoundsInScreen(rect)
            val inBottom = rect.bottom >= bottomZone && rect.bottom <= screenHeight
            val sizeOk = rect.width() in minWidth..maxWidth && rect.height() in minHeight..maxHeight
            val notWholeBar = rect.top < screenHeight - dp(20f)
            if (inBottom && sizeOk && notWholeBar) {
                val score = rect.width().toLong() * rect.height() +
                    if (current.isClickable) CLICKABLE_BONUS else 0L
                if (score > bestScore) {
                    bestScore = score
                    best = Rect(rect)
                }
            }
            current = current.parent
            depth++
        }
        return best
    }

    private fun shelfBounds(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
    ): Rect? {
        var current = node.parent
        var best: Rect? = null
        var bestArea = -1L
        var depth = 0
        while (current != null && depth < 9) {
            val rect = Rect()
            current.getBoundsInScreen(rect)
            val wideEnough = rect.width() >= screenWidth * 0.82f
            val tallEnough = rect.height() >= dp(200f)
            val notFullScreen = rect.height() <= screenHeight * 0.62f
            val belowChrome = rect.top >= dp(30f)
            if (wideEnough && tallEnough && notFullScreen && belowChrome) {
                val area = rect.width().toLong() * rect.height()
                if (area > bestArea) {
                    bestArea = area
                    best = Rect(rect)
                }
            }
            current = current.parent
            depth++
        }
        return best
    }

    /**
     * Extra view signal for apps whose ViewPager never emits a usable scroll delta:
     * a material change in visible captions/usernames, or a changed collection row, means a new
     * item is on screen. Guarded by a 3 s cooldown inside the tracker.
     */
    private fun maybeCountContentChange(surfaceId: String, nodes: List<AccessibilityNodeInfo>) {
        if (signatureSurface != surfaceId) {
            signatureSurface = surfaceId
            signatureLabels = null
            signatureRow = null
        }
        val labels = visibleLabels(nodes)
        val row = nodes.asSequence()
            .mapNotNull { it.collectionItemInfo?.rowIndex?.takeIf { index -> index >= 0 } }
            .maxOrNull()

        val previousLabels = signatureLabels
        if (previousLabels == null) {
            signatureLabels = labels
        } else if (labels != null && labelsChanged(previousLabels, labels)) {
            tracker.noteSignature()
            signatureLabels = labels
        }
        val previousRow = signatureRow
        if (previousRow != null && row != null && row != previousRow) {
            tracker.noteSignature()
        }
        signatureRow = row
    }

    private fun visibleLabels(nodes: List<AccessibilityNodeInfo>): Set<String>? {
        val noise = setOf(
            "like", "comment", "comments", "share", "save", "follow", "following", "more",
            "send", "reels", "shorts", "home", "search", "explore", "profile", "audio",
            "original audio", "play", "pause", "mute", "unmute", "close", "back", "next",
            "previous", "sponsored", "views", "view", "see more", "translate",
        )
        val labels = nodes.asSequence()
            .mapNotNull { (it.text ?: it.contentDescription)?.toString()?.trim()?.lowercase() }
            .filter { it.length in 3..80 }
            .filter { label -> label.any { it.isLetter() } }
            .filterNot { it in noise }
            .filterNot { it.startsWith("tab ") }
            .take(60)
            .toSet()
        return if (labels.size >= 2) labels else null
    }

    private fun labelsChanged(old: Set<String>, new: Set<String>): Boolean {
        val union = old.union(new).size
        if (union == 0) return false
        val intersection = old.intersect(new).size
        return (1f - intersection.toFloat() / union.toFloat()) >= 0.35f
    }

    private fun hasBottomLabel(nodes: List<AccessibilityNodeInfo>, token: String): Boolean {
        val screenHeight = resources.displayMetrics.heightPixels
        return nodes.any { node ->
            val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            if (label == null || !label.contains(token)) return@any false
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.bottom >= screenHeight - dp(210)
        }
    }

    private fun hasAudioLabel(nodes: List<AccessibilityNodeInfo>): Boolean =
        nodes.any { node ->
            val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            label == "original audio" || label == "original sound" || label == "audio" || label == "sound"
        }

    private fun hasViewId(nodes: List<AccessibilityNodeInfo>, vararg tokens: String): Boolean =
        nodes.any { node ->
            val id = node.viewIdResourceName?.lowercase()
            id != null && tokens.any { id.contains(it) }
        }

    private fun hasTopTitle(nodes: List<AccessibilityNodeInfo>, token: String): Boolean {
        val screenHeight = resources.displayMetrics.heightPixels
        return nodes.any { node ->
            val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            if (label == null || !label.startsWith(token)) return@any false
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.top < screenHeight * 0.26f && rect.bottom < screenHeight * 0.45f
        }
    }

    private fun isPagerScroll(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return true
        val bounds = Rect()
        source.getBoundsInScreen(bounds)
        if (bounds.height() <= 0) return true
        return bounds.height() >= resources.displayMetrics.heightPixels * 0.5f
    }

    /** Picks the target app's window, cheapest source first, ignoring our own overlays. */
    private fun activeRootFor(pkg: String, source: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        runCatching {
            val active = rootInActiveWindow
            if (active != null && active.packageName?.toString() == pkg) return active
        }
        runCatching {
            windows.forEach { window ->
                val candidate = window.root
                if (candidate != null && candidate.packageName?.toString() == pkg) return candidate
            }
        }
        // Some OEMs answer with the launcher, a dialog, or our own overlay. The node that produced
        // the event always belongs to the app, so climb to its window root instead of losing it.
        return runCatching {
            var node = source ?: return null
            var root = node
            var depth = 0
            while (depth < 40) {
                val parent = node.parent ?: break
                root = parent
                node = parent
                depth++
            }
            root.takeIf { it.packageName?.toString() == pkg }
        }.getOrNull()
    }

    /**
     * Structural proof of a swipeable clip feed: a full-screen scrollable whose own item fills the
     * viewport. Class names and view ids change with every app release; a vertical pager holding one
     * screen-tall page is what Reels, Shorts, For You and Spotlight all are on every device.
     */
    private fun hasClipPager(nodes: List<AccessibilityNodeInfo>): Boolean {
        val metrics = resources.displayMetrics
        val pager = nodes.firstOrNull { node ->
            if (!node.isScrollable ||
                node.actionList.none { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD }
            ) {
                return@firstOrNull false
            }
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.height() >= metrics.heightPixels * 0.72f && rect.width() >= metrics.widthPixels * 0.9f
        } ?: return false
        val bounds = Rect()
        pager.getBoundsInScreen(bounds)
        for (index in 0 until pager.childCount) {
            val child = runCatching { pager.getChild(index) }.getOrNull() ?: continue
            val childBounds = Rect()
            child.getBoundsInScreen(childBounds)
            if (childBounds.height() >= bounds.height() * 0.82f &&
                childBounds.width() >= bounds.width() * 0.9f
            ) {
                return true
            }
        }
        return false
    }

    /** The soft keyboard is its own window: comments are open whenever it is on screen. */
    private fun imeVisible(): Boolean = runCatching {
        windows.any { window ->
            if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) return@any false
            val bounds = Rect()
            window.getBoundsInScreen(bounds)
            bounds.height() > 0
        }
    }.getOrDefault(false)

    private fun collectNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>(128)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            result.add(node)
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            // Collapsed rows stay in the tree and cost a binder call each. Nothing measurable is
            // inside a zero-area container, so the walk stops there instead of at the page.
            if (bounds.width() <= 0 || bounds.height() <= 0) continue
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return result
    }

    // --------------------------------------------------------------- blocking

    private fun detectSurface(
        platformId: String,
        event: AccessibilityEvent?,
        nodes: List<AccessibilityNodeInfo>,
        pager: Boolean,
    ): String? {
        val platform = PlatformCatalog.byId(platformId) ?: return null
        // Activity context is stable across scroll events; event.className is usually RecyclerView there.
        val className = (currentActivityClass ?: event?.className?.toString() ?: "").lowercase()
        platform.surfaces.firstOrNull { surface ->
            surface.classHints.any { className.contains(it) }
        }?.let { return it.id }

        // Version-proof structural path: a screen-filling vertical pager is the app's clip feed,
        // whatever this release named the activity. Stories are excluded by their own captions, and
        // so is any screen still showing the app's tab bar: a normal home feed is a pager too, and
        // counting it as Reels would invent time the user never spent in a clip feed.
        if (pager && !hasAnyLabel(nodes, "story", "stories", "view story", "add to your story") &&
            !hasBottomLabel(nodes, "home")
        ) {
            platform.surfaces.firstOrNull { it.immersive }?.let { return it.id }
        }

        // Compose windows report FrameLayout, so a clip pager has to prove itself by content.
        // Grid surfaces such as Explore keep their bottom bar and are never claimed here.
        if (!hasBottomLabel(nodes, "home")) {
            platform.surfaces.firstOrNull { surface ->
                surface.immersive && (
                    surface.titleTokens.any { hasTopTitle(nodes, it) } ||
                        hasViewId(nodes, *surface.idHints.toTypedArray()) ||
                        hasAudioLabel(nodes)
                    )
            }?.let { return it.id }
        }

        val tokens = platform.surfaces.flatMap { surface -> surface.navTokens.map { it to surface.id } }
        if (tokens.isEmpty()) return null
        val label = nodes.asSequence()
            .filter { it.isSelected }
            .mapNotNull { (it.text ?: it.contentDescription)?.toString()?.trim()?.lowercase() }
            .firstOrNull { candidate -> candidate.length <= 32 && tokens.any { (token, _) -> candidate.startsWith(token) } }
            ?: return null
        return tokens.firstOrNull { (token, _) -> label == token || label.startsWith("$token ") }?.second
            ?: tokens.firstOrNull { (token, _) -> label.contains(token) }?.second
    }

    private fun hasAnyLabel(nodes: List<AccessibilityNodeInfo>, vararg tokens: String): Boolean =
        nodes.any { node ->
            val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
                ?: return@any false
            tokens.any { label.contains(it) }
        }

    /**
     * App-level budget. Reads today's foreground time at most once a minute, warns at 80% and
     * shows the block screen once the budget is spent. Requires usage access; otherwise it is a
     * silent no-op rather than a fake promise.
     */
    private fun enforceDailyLimit(platformId: String, settings: Settings, now: Long): Boolean {
        val config = settings.platforms[platformId] ?: return false
        if (!config.enabled) return false
        val limit = config.dailyLimitMinutes
        if (limit <= 0) return false
        val lastCheck = lastLimitCheckAt[platformId] ?: 0L
        if (now - lastCheck < 60_000L) return false
        lastLimitCheckAt[platformId] = now
        if (!usageRepository.hasPermission()) return false
        val pkg = PlatformCatalog.byId(platformId)?.packageName ?: return false
        val minutes = UsageStatsRepository.minutesToday(this, pkg)
        val fraction = minutes.toFloat() / limit.toFloat()

        if (minutes >= limit) {
            val lastBlock = lastLimitBlockAt[platformId] ?: 0L
            if (now - lastBlock < 5 * 60_000L) return true
            if (!allowIntrusion(now)) return true
            lastLimitBlockAt[platformId] = now
            AppRepository.get(this).logEvent(platformId, "app", BlockMode.STRICT.name, "BLOCKED")
            tracker.leave()
            overlays.clear()
            foregroundPlatform = null
            stopTicker()
            hud.hide()
            nudge.removeNow()
            launchBlock(platformId, "app", BlockMode.STRICT.name, "LIMIT")
            return true
        }

        if (fraction >= 0.8f) {
            val day = LocalDate.now().toEpochDay()
            if (warnedDay != day) {
                warnedDay = day
                warnedLimits.clear()
            }
            val key = "$day|$platformId"
            if (warnedLimits.add(key)) {
                notifyBudget(platformId, minutes, limit)
            }
        }
        return false
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun notifyBudget(platformId: String, minutes: Int, limit: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(LIMIT_CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    LIMIT_CHANNEL,
                    "Daily limits",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Warns before an app budget is spent." },
            )
        }
        val open = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val name = PlatformCatalog.byId(platformId)?.name ?: platformId
        val notification = NotificationCompat.Builder(this, LIMIT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("$name: 80% of budget")
            .setContentText("$minutes of $limit min used today.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            runCatching { NotificationManagerCompat.from(this).notify(platformId.hashCode(), notification) }
        }
    }

    private fun launchBlock(platformId: String, surfaceId: String, mode: String, reason: String) {
        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(BlockActivity.EXTRA_PLATFORM, platformId)
            putExtra(BlockActivity.EXTRA_SURFACE, surfaceId)
            putExtra(BlockActivity.EXTRA_MODE, mode)
            putExtra(BlockActivity.EXTRA_REASON, reason)
        }
        runCatching { startActivity(intent) }
    }

    private fun enforce(platformId: String, surfaceId: String, settings: Settings, now: Long) {
        val decision = RuleEngine.decide(
            platformId = platformId,
            surfaceId = surfaceId,
            settings = settings,
            nowMillis = now,
            bypassUntilMillis = BypassStore.until(platformId, surfaceId),
        )
        if (!decision.block) return

        val key = BypassStore.key(platformId, surfaceId)
        if (now - (lastBlockAt[key] ?: 0L) < BLOCK_DEBOUNCE_MS) return
        if (!allowIntrusion(now)) return
        lastBlockAt[key] = now

        val repo = AppRepository.get(this)
        repo.logEvent(platformId, surfaceId, decision.mode.name, "BLOCKED")
        tracker.leave()
        foregroundPlatform = null
        stopTicker()

        overlays.clear()
        hud.hide()
        nudge.removeNow()

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(BlockActivity.EXTRA_PLATFORM, platformId)
            putExtra(BlockActivity.EXTRA_SURFACE, surfaceId)
            putExtra(BlockActivity.EXTRA_MODE, decision.mode.name)
            putExtra(BlockActivity.EXTRA_REASON, decision.reason.name)
        }
        // Our screen first, then the back press that walks the user out of the feed. Pressing Back
        // when the block screen did not come up - Android blocks background activity starts on
        // some OEMs - navigates the phone with nothing to show for it, which reads as a hang.
        if (runCatching { startActivity(intent) }.isFailure) {
            Log.w("OpeneyeSafety", "block screen could not open for $platformId; left the app alone")
            return
        }
        // The back press has to wait for the handover. Delivered while the feed still owns the
        // window it exits the feed, which is the point; delivered after our own screen appears it
        // dismisses that screen and drops the user straight back into what we meant to block.
        handler.postDelayed({
            if (foregroundWindowPackage() != packageName) {
                runCatching { performGlobalAction(GLOBAL_ACTION_BACK) }
            }
        }, BLOCK_BACK_DELAY_MS)
    }

    /**
     * Whether we may still touch the system on this pass.
     *
     * Covering a button, opening our own screen and pressing Back are the only things here that can
     * make a phone feel broken, and every one of them is driven by a guess about another app's view
     * tree. A burst of them means the guess is wrong, so the shield stops guessing: the intrusive
     * part pauses for ten minutes while counting, the readout and the reminders carry on.
     */
    private fun allowIntrusion(now: Long): Boolean {
        if (now < handsOffUntil) return false
        intrusions.addLast(now)
        while (intrusions.isNotEmpty() && now - intrusions.first() > INTRUSION_WINDOW_MS) {
            intrusions.removeFirst()
        }
        if (intrusions.size <= MAX_INTRUSIONS_PER_WINDOW) return true
        handsOffUntil = now + HANDS_OFF_MS
        intrusions.clear()
        if (::overlays.isInitialized) overlays.clear()
        if (::hud.isInitialized) hud.hide()
        if (::nudge.isInitialized) nudge.removeNow()
        setAmbientDown()
        Log.w("OpeneyeSafety", "too many covers or blocks in ${INTRUSION_WINDOW_MS / 1000}s; hands off for ${HANDS_OFF_MS / 60_000}min")
        return false
    }

    // ---------------------------------------------------------------- ambient

    /**
     * The whole-phone timer goes where there is nothing of ours already saying the same thing: not
     * inside a feed, where the corner counter and the clips are already up, not inside our own
     * screens, and not while the system is being protected from us.
     *
     * Decided from the window that owns the screen right now, never from a remembered package, so a
     * missed event cannot leave it stuck over the launcher or stuck over Instagram.
     */
    private fun refreshAmbient(force: Boolean = false) {
        val controller = ambient ?: return
        val now = SystemClock.elapsedRealtime()
        // Window events arrive in bursts during a transition and each one costs a window lookup.
        // Two seconds is shorter than any transition that matters - but the last event of a burst
        // is the one that tells the truth, so a swallowed check always gets a follow-up.
        if (!force && now - ambientCheckedAt < AMBIENT_MIN_GAP_MS) {
            scheduleAmbientCheck(AMBIENT_MIN_GAP_MS)
            return
        }
        ambientCheckedAt = now
        val settings = AppRepository.get(this).settings.value
        val pkg = foregroundWindowPackage()
        val power = getSystemService(PowerManager::class.java)
        val lit = power == null || power.isInteractive
        val usageOk = ambientUsageAllowed()
        val handsOff = System.currentTimeMillis() < handsOffUntil
        if (BuildConfig.DEBUG) {
            Log.d("OpeneyeAmbient", "pkg=$pkg timer=${settings.ambientTimer} lit=$lit usage=$usageOk hands=$handsOff")
        }
        val wanted = settings.ambientTimer &&
            pkg != null &&
            pkg != packageName &&
            pkg !in PlatformCatalog.packages &&
            lit &&
            !handsOff &&
            usageOk
        if (!wanted) {
            if (controller.isShowing) setAmbientDown()
            return
        }
        val minutes = ScreenTimeMeter.minutes(this)
        if (minutes < 0) {
            // The first read of the day is still on its way from an IO worker. Ask again shortly
            // instead of leaving the corner blank until the next app change.
            if (controller.isShowing) setAmbientDown()
            scheduleAmbientCheck(AMBIENT_RETRY_MS)
            return
        }
        controller.show("today ${UsageStatsRepository.formatMinutes(minutes)}")
        if (!ambientRunning) {
            ambientRunning = true
            handler.postDelayed(ambientTick, AMBIENT_TICK_MS)
        }
    }

    /** Take the corner timer down and stop its loop with it. */
    private fun setAmbientDown() {
        ambientRunning = false
        handler.removeCallbacks(ambientTick)
        handler.removeCallbacks(ambientRetry)
        ambientPending = false
        ambient?.hide()
    }

    private fun scheduleAmbientCheck(delayMs: Long) {
        if (ambientPending) return
        ambientPending = true
        handler.postDelayed(ambientRetry, delayMs)
    }

    /** An AppOps check is a binder call; the switch it gates is only read once every 30 s. */
    private fun ambientUsageAllowed(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - ambientUsageCheckedAt > AMBIENT_USAGE_CHECK_MS) {
            ambientUsageCheckedAt = now
            ambientUsageOk = usageRepository.hasPermission()
        }
        return ambientUsageOk
    }

    /** So flipping the switch shows or hides the timer without waiting for the next window change. */
    fun ambientSettingChanged() {
        handler.post { refreshAmbient(force = true) }
    }

    // -------------------------------------------------------------------- hud

    private fun refreshHud() {
        if (!::hud.isInitialized || !::tracker.isInitialized) return
        if (!AppRepository.get(this).settings.value.trackingEnabled) {
            hud.hide()
            return
        }
        if (foregroundPlatform == null) {
            hud.hide()
            return
        }
        if (focusLostAt != 0L) {
            // Another window is on screen. The ticker is already deciding whether the session is
            // over; until then nothing of ours may be painted over the app in front.
            hud.hide()
            return
        }
        val today = LocalDate.now().toEpochDay()
        val records = AppRepository.get(this).watch.value.filter { it.epochDay == today }
        val baseCount = records.sumOf { it.count }
        val baseMillis = records.sumOf { it.millis }
        var totalCount = baseCount + tracker.pendingCount()
        var totalMillis = baseMillis + tracker.pendingMillis()
        // Never let an async flush make the visible today total jump backwards.
        if (hudDay != today) {
            hudDay = today
            hudCount = 0
            hudMillis = 0L
        }
        hudCount = maxOf(hudCount, totalCount)
        hudMillis = maxOf(hudMillis, totalMillis)
        totalCount = hudCount
        totalMillis = hudMillis
        val snapshot = tracker.snapshot()

        val settings = AppRepository.get(this).settings.value
        val stopRule = settings.contract.stopRuleMinutes
        val overRule = stopRule > 0 && totalMillis >= stopRule * 60_000L
        // Escalating wording instead of one fixed string: an in-feed readout that never changes is
        // looked through within a few sessions, and this one only changes as the session grows.
        val sessionMinutes = snapshot.millis / 60_000L
        val mood = when {
            snapshot.millis <= 0L -> "just opened"
            sessionMinutes < 3 -> "still here"
            sessionMinutes < 10 -> "a while now"
            sessionMinutes < 25 -> "this is a long one"
            else -> "you chose this at some point"
        }
        val surfaceWord = when (snapshot.surfaceId) {
            "reels" -> "reels"
            "shorts" -> "shorts"
            "explore" -> "explore"
            "foryou" -> "for you"
            else -> null
        }
        val lines = buildList {
            add("today  ${compact(totalMillis)}  ·  $totalCount clips")
            if (surfaceWord != null) {
                add("$mood  ${clock(snapshot.millis)}  ·  ${snapshot.swipes} swipes")
                add("this one  ${clock(snapshot.currentItemMillis)}")
            }
            if (overRule) add("over your ${stopRule}m")
        }
        hud.show(lines.joinToString("\n"))
    }

    // ------------------------------------------------------------------ nudge

    /**
     * The periodic in-feed reminder. Timed on minutes actually watched, not wall clock: a popup
     * after 20 minutes of *watching* is a signal, the same popup after 20 minutes with the phone
     * face-down is noise the user learns to swipe past.
     */
    private fun maybeNudge() {
        if (!::nudge.isInitialized || !::tracker.isInitialized) return
        val settings = AppRepository.get(this).settings.value
        val cadence = settings.nudgeMinutes
        if (cadence <= 0 || !settings.trackingEnabled || !tracker.active || foregroundPlatform == null) {
            if (BuildConfig.DEBUG && nudgeAtMillis != 0L) {
                Log.d("OpeneyeNudge", "disarmed cadence=$cadence tracking=${settings.trackingEnabled} active=${tracker.active} fg=$foregroundPlatform")
            }
            nudgeAtMillis = 0L
            if (nudge.isShowing) nudge.hide()
            return
        }
        if (nudgeAtMillis == 0L) {
            // The first one comes early, while switching contexts is still cheap.
            nudgeAtMillis = hudMillis + minOf(cadence, FIRST_NUDGE_MINUTES) * 60_000L
            if (BuildConfig.DEBUG) Log.d("OpeneyeNudge", "armed at ${nudgeAtMillis / 60_000}m, now ${hudMillis / 60_000}m")
            return
        }
        if (hudMillis < nudgeAtMillis) return
        nudgeAtMillis = hudMillis + cadence * 60_000L
        if (BuildConfig.DEBUG) Log.d("OpeneyeNudge", "fired at ${hudMillis / 60_000}m")

        val stopRule = settings.contract.stopRuleMinutes
        val overRule = stopRule > 0 && hudMillis >= stopRule * 60_000L
        val line = if (overRule) {
            Voice.fill(Voice.pick(Voice.nudgesOver, nudgeCursor), compact(hudMillis), "${stopRule}m")
        } else {
            Voice.fill(Voice.pick(Voice.nudges, nudgeCursor), compact(hudMillis), hudCount.toString())
        }
        nudgeCursor++
        val screenTime = if (settings.countLongVideos) screenTimeLine() else null
        nudge.show(if (screenTime == null) line else "$line\n$screenTime")
    }

    /** Whole-app time from UsageStats. Real minutes, so long-form watching is not estimated. */
    private fun screenTimeLine(): String? {
        val platformId = foregroundPlatform?.let { PlatformCatalog.byId(it) } ?: return null
        if (!usageRepository.hasPermission()) return null
        val now = System.currentTimeMillis()
        if (screenMinutesPlatform != platformId.id || now - screenMinutesAt > 60_000L) {
            screenMinutesPlatform = platformId.id
            screenMinutesAt = now
            screenMinutes = UsageStatsRepository.minutesToday(this, platformId.packageName)
        }
        if (screenMinutes <= 0) return null
        return Voice.fill(Voice.nudgeScreenTime, compact(screenMinutes * 60_000L))
    }

    private fun clock(millis: Long): String {
        val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
        return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
    }

    private fun compact(millis: Long): String {
        val minutes = millis / 60_000L
        return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
    }

    private fun startTicker() {
        if (tickerRunning) return
        tickerRunning = true
        handler.post(ticker)
    }

    private fun stopTicker() {
        tickerRunning = false
        handler.removeCallbacks(ticker)
    }

    private fun leaveTargetApp() {
        if (BuildConfig.DEBUG) Log.d("OpeneyeFocus", "leave $currentPackage")
        if (::tracker.isInitialized) tracker.leave()
        signatureSurface = null
        signatureLabels = null
        signatureRow = null
        if (::overlays.isInitialized) overlays.clear()
        if (::hud.isInitialized) hud.hide()
        if (::nudge.isInitialized) nudge.removeNow()
        nudgeAtMillis = 0L
        focusLostAt = 0L
        stopTicker()
        foregroundPlatform = null
        currentPackage = null
        currentActivityClass = null
        lastSurfaceId = null
        lastSurfaceSeen = 0L
        refreshAmbient()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EVALUATION_INTERVAL_MS = 400L
        private const val SURFACE_GRACE_MS = 3_000L
        private const val SURFACE_STALE_MS = 10_000L
        private const val FOCUS_LOST_MS = 2_000L
        private const val TICK_MS = 1_000L
        private const val BLOCK_DEBOUNCE_MS = 1_500L
        private const val BLOCK_BACK_DELAY_MS = 700L
        private const val VERIFY_AFTER_QUIET_MS = 3_000L
        private const val INTRUSION_WINDOW_MS = 120_000L
        private const val MAX_INTRUSIONS_PER_WINDOW = 6
        private const val HANDS_OFF_MS = 600_000L
        private const val MAX_NODES = 400
        private const val LIMIT_CHANNEL = "openeye_limits"
        private const val CLICKABLE_BONUS = 1_000_000L
        private const val FIRST_NUDGE_MINUTES = 2
        private const val AMBIENT_TICK_MS = 60_000L
        private const val AMBIENT_MIN_GAP_MS = 2_000L
        private const val AMBIENT_RETRY_MS = 3_000L
        private const val AMBIENT_USAGE_CHECK_MS = 30_000L

        @Volatile
        var instance: GuardAccessibilityService? = null
            private set
    }
}