package com.example.openmind.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
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
import com.example.openmind.BuildConfig
import com.example.openmind.MainActivity
import com.example.openmind.R
import com.example.openmind.data.AppRepository
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
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
    private var nudgeAt = 0L
    private var nudgeCursor = 0
    private var screenMinutes = 0
    private var screenMinutesAt = 0L
    private var screenMinutesPlatform: String? = null
    private val lastBlockAt = mutableMapOf<String, Long>()
    private val lastLimitCheckAt = mutableMapOf<String, Long>()
    private val lastLimitBlockAt = mutableMapOf<String, Long>()
    private val warnedLimits = mutableSetOf<String>()
    private var warnedDay = -1L
    private lateinit var usageRepository: UsageStatsRepository

    private val ticker = object : Runnable {
        override fun run() {
            if (!tickerRunning) return
            tracker.tick()
            refreshHud()
            maybeNudge()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlays = HidingOverlayController(this, getSystemService(WindowManager::class.java))
        hud = WatchHudController(this, getSystemService(WindowManager::class.java))
        nudge = WatchNudgeController(this, getSystemService(WindowManager::class.java))
        tracker = WatchSessionTracker(AppRepository.get(this))
        usageRepository = UsageStatsRepository(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::overlays.isInitialized) overlays.clear()
        if (::hud.isInitialized) hud.hide()
        if (::nudge.isInitialized) nudge.removeNow()
        lastEvaluation = 0L
    }

    override fun onDestroy() {
        leaveTargetApp()
        if (::overlays.isInitialized) overlays.clear()
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {
        leaveTargetApp()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::overlays.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        val type = event.eventType

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
            // Content changes from the keyboard or system UI must not tear down a live overlay.
            // Only a real window-state change means another app took the foreground.
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) leaveTargetApp()
            return
        }

        val platformId = PlatformCatalog.packages[pkg] ?: return
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentPackage = pkg
            currentActivityClass = event.className?.toString()
            if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "window class=${event.className}")
        }
        foregroundPlatform = platformId
        startTicker()

        val now = System.currentTimeMillis()
        // Count taps before throttling so quick taps are not dropped.
        if ((type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                type == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) &&
            now - lastSurfaceSeen < SURFACE_GRACE_MS
        ) {
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
            foregroundPlatform = null
            stopTicker()
            hud.hide()
            nudge.removeNow()
            return
        }

        if (!settings.trackingEnabled) {
            tracker.leave()
            stopTicker()
            hud.hide()
        }

        val root = activeRootFor(pkg) ?: return

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

        val surfaceId = detectSurface(platformId, event, nodes)
        if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "detect class=$currentActivityClass event=${event.className} -> $surfaceId")
        if (surfaceId != null) {
            lastSurfaceId = surfaceId
            lastSurfaceSeen = now
            tracker.enter(platformId, surfaceId)
        }
        val activeSurface = surfaceId
            ?: lastSurfaceId?.takeIf { now - lastSurfaceSeen < SURFACE_GRACE_MS }

        // Comments sheets contain an editable field; never count their scrolling as feed paging.
        if (activeSurface != null && tracker.active && !hasEditableNode(nodes)) {
            if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && isPagerScroll(event)) {
                tracker.noteScroll()
            }
            maybeCountContentChange(activeSurface, nodes)
        }

        if (surfaceId != null) {
            enforce(platformId, surfaceId, settings, now)
        } else if (now - lastSurfaceSeen > SURFACE_GRACE_MS) {
            if (BuildConfig.DEBUG) Log.d("OpeneyeSurface", "leave surface")
            tracker.leave()
            lastSurfaceId = null
        }

        refreshHud()
    }

    // ---------------------------------------------------------------- hiding

    private fun buildHideTargets(
        platformId: String,
        settings: Settings,
        nodes: List<AccessibilityNodeInfo>,
    ): List<HideTarget> {
        val config = settings.platforms[platformId] ?: return emptyList()
        if (!config.enabled) return emptyList()

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

    private fun hasEditableNode(nodes: List<AccessibilityNodeInfo>): Boolean =
        nodes.any { it.isEditable }

    private fun isPagerScroll(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return true
        val bounds = Rect()
        source.getBoundsInScreen(bounds)
        if (bounds.height() <= 0) return true
        return bounds.height() >= resources.displayMetrics.heightPixels * 0.5f
    }

    /** Picks the active window that belongs to the target app, ignoring our own overlays. */
    private fun activeRootFor(pkg: String): AccessibilityNodeInfo? {
        runCatching {
            windows.forEach { window ->
                val candidate = window.root
                if (candidate != null && candidate.packageName?.toString() == pkg) return candidate
            }
        }
        val fallback = rootInActiveWindow
        return if (fallback?.packageName?.toString() == pkg) fallback else null
    }

    private fun collectNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>(128)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            result.add(node)
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.add(it) }
            }
        }
        return result
    }

    // --------------------------------------------------------------- blocking

    private fun detectSurface(
        platformId: String,
        event: AccessibilityEvent,
        nodes: List<AccessibilityNodeInfo>,
    ): String? {
        val platform = PlatformCatalog.byId(platformId) ?: return null
        // Activity context is stable across scroll events; event.className is usually RecyclerView there.
        val className = (currentActivityClass ?: event.className?.toString() ?: "").lowercase()
        platform.surfaces.firstOrNull { surface ->
            surface.classHints.any { className.contains(it) }
        }?.let { return it.id }

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
        lastBlockAt[key] = now

        val repo = AppRepository.get(this)
        repo.logEvent(platformId, surfaceId, decision.mode.name, "BLOCKED")
        tracker.leave()
        foregroundPlatform = null
        stopTicker()

        overlays.clear()
        hud.hide()
        nudge.removeNow()
        performGlobalAction(GLOBAL_ACTION_BACK)

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
        runCatching { startActivity(intent) }
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
     * The periodic in-feed reminder. Timed on feed minutes, not wall clock: a popup after 20
     * minutes of *watching* is a signal, the same popup after 20 minutes with the phone face-down
     * is noise the user learns to swipe past.
     */
    private fun maybeNudge() {
        if (!::nudge.isInitialized || !::tracker.isInitialized) return
        val now = SystemClock.elapsedRealtime()
        val settings = AppRepository.get(this).settings.value
        val cadence = settings.nudgeMinutes
        if (cadence <= 0 || !settings.trackingEnabled || !tracker.active || foregroundPlatform == null) {
            nudgeAt = 0L
            if (nudge.isShowing) nudge.hide()
            return
        }
        if (nudgeAt == 0L) {
            // The first one comes early, while switching contexts is still cheap.
            nudgeAt = now + minOf(cadence, FIRST_NUDGE_MINUTES) * 60_000L
            return
        }
        if (now < nudgeAt) return
        nudgeAt = now + cadence * 60_000L

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
        if (::tracker.isInitialized) tracker.leave()
        signatureSurface = null
        signatureLabels = null
        signatureRow = null
        if (::overlays.isInitialized) overlays.clear()
        if (::hud.isInitialized) hud.hide()
        if (::nudge.isInitialized) nudge.removeNow()
        nudgeAt = 0L
        stopTicker()
        foregroundPlatform = null
        currentPackage = null
        currentActivityClass = null
        lastSurfaceId = null
        lastSurfaceSeen = 0L
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EVALUATION_INTERVAL_MS = 300L
        private const val SURFACE_GRACE_MS = 3_000L
        private const val BLOCK_DEBOUNCE_MS = 1_500L
        private const val MAX_NODES = 400
        private const val LIMIT_CHANNEL = "openeye_limits"
        private const val CLICKABLE_BONUS = 1_000_000L
        private const val FIRST_NUDGE_MINUTES = 2

        @Volatile
        var instance: GuardAccessibilityService? = null
            private set
    }
}