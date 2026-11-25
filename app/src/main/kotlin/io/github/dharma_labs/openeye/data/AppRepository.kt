package io.github.dharma_labs.openeye.data

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.openMindStore by preferencesDataStore(name = "openmind")

/**
 * Single source of truth. Everything lives in one local Preferences DataStore:
 * no account, no network permission, no cloud backup.
 */
class AppRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _events = MutableStateFlow<List<BlockEvent>>(emptyList())
    val events: StateFlow<List<BlockEvent>> = _events.asStateFlow()

    private val _watch = MutableStateFlow<List<WatchRecord>>(emptyList())
    val watch: StateFlow<List<WatchRecord>> = _watch.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch {
            appContext.openMindStore.data
                .catch { emit(emptyPreferences()) }
                .collect { prefs ->
                    prefs[SETTINGS_KEY]?.let { raw ->
                        runCatching { json.decodeFromString<Settings>(raw) }.getOrNull()
                            ?.let { _settings.value = it.withCatalogDefaults() }
                    }
                    prefs[EVENTS_KEY]?.let { raw ->
                        runCatching { json.decodeFromString<List<BlockEvent>>(raw) }.getOrNull()?.let { _events.value = it }
                    }
                    prefs[WATCH_KEY]?.let { raw ->
                        runCatching { json.decodeFromString<List<WatchRecord>>(raw) }.getOrNull()?.let { _watch.value = it }
                    }
                    _ready.value = true
                }
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        persistSettings(next)
    }

    private fun persistSettings(settings: Settings) {
        scope.launch {
            runCatching {
                appContext.openMindStore.edit { prefs ->
                    prefs[SETTINGS_KEY] = json.encodeToString(settings)
                }
            }
        }
    }

    fun logEvent(platformId: String, surfaceId: String, mode: String, outcome: String, intention: String? = null) {
        val event = BlockEvent(
            id = UUID.randomUUID().toString(),
            epochMs = System.currentTimeMillis(),
            platformId = platformId,
            surfaceId = surfaceId,
            mode = mode,
            outcome = outcome,
            intention = intention,
        )
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val next = (listOf(event) + _events.value)
            .filter { it.epochMs >= cutoff }
            .take(MAX_EVENTS)
        _events.value = next
        scope.launch {
            runCatching {
                appContext.openMindStore.edit { prefs ->
                    prefs[EVENTS_KEY] = json.encodeToString(next)
                }
            }
        }
    }

    /** Merges a finished watch slice inside a DataStore transaction so no update can be lost. */
    fun addWatch(
        platformId: String,
        surfaceId: String,
        count: Int,
        millis: Long,
        sessions: Int = 0,
        longestSessionMs: Long = 0,
        touches: Int = 0,
        hour: Int = -1,
    ) {
        if (count <= 0 && millis <= 0 && sessions <= 0 && touches <= 0) return
        val day = LocalDate.now().toEpochDay()
        scope.launch {
            runCatching {
                appContext.openMindStore.edit { prefs ->
                    val current = prefs[WATCH_KEY]?.let { raw ->
                        runCatching { json.decodeFromString<List<WatchRecord>>(raw) }.getOrNull()
                    } ?: _watch.value
                    val existing = current.firstOrNull {
                        it.epochDay == day && it.platformId == platformId &&
                            it.surfaceId == surfaceId && it.hour == hour
                    }
                    val merged = if (existing == null) {
                        current + WatchRecord(
                            day, platformId, surfaceId, count, millis,
                            sessions, longestSessionMs, touches, hour,
                        )
                    } else {
                        current.map { record ->
                            if (record === existing) {
                                record.copy(
                                    count = record.count + count,
                                    millis = record.millis + millis,
                                    sessions = record.sessions + sessions,
                                    longestSessionMs = maxOf(record.longestSessionMs, longestSessionMs),
                                    touches = record.touches + touches,
                                )
                            } else {
                                record
                            }
                        }
                    }.filter { it.epochDay >= day - 120 }.takeLast(3000)
                    prefs[WATCH_KEY] = json.encodeToString(merged)
                    _watch.value = merged
                }
            }
        }
    }
    fun todayWatch(): List<WatchRecord> =
        _watch.value.filter { it.epochDay == LocalDate.now().toEpochDay() }

    fun completeOnboarding() = updateSettings { it.copy(onboardingComplete = true) }

    fun setMaster(enabled: Boolean) = updateSettings { it.copy(masterEnabled = enabled) }

    fun setThemeMode(mode: String) = updateSettings { it.copy(themeMode = mode) }

    fun setTrackingEnabled(enabled: Boolean) = updateSettings { it.copy(trackingEnabled = enabled) }

    fun setNudgeMinutes(minutes: Int) = updateSettings {
        it.copy(nudgeMinutes = minutes.coerceIn(0, 60))
    }

    fun setCountLongVideos(enabled: Boolean) = updateSettings {
        it.copy(countLongVideos = enabled)
    }

    fun setAmbientTimer(enabled: Boolean) = updateSettings {
        it.copy(ambientTimer = enabled)
    }

    fun setDailyLimit(platformId: String, minutes: Int) = updateSettings { settings ->
        val platform = settings.platforms[platformId] ?: return@updateSettings settings
        settings.copy(
            platforms = settings.platforms + (
                platformId to platform.copy(dailyLimitMinutes = minutes.coerceIn(0, 600))
                ),
        )
    }

    fun setStrict(enabled: Boolean) {
        if (enabled) io.github.dharma_labs.openeye.domain.BypassStore.clear()
        updateSettings { it.copy(strictMode = enabled) }
    }

    fun setPlatformEnabled(platformId: String, enabled: Boolean) = updateSettings { settings ->
        val platform = settings.platforms[platformId] ?: return@updateSettings settings
        settings.copy(platforms = settings.platforms + (platformId to platform.copy(enabled = enabled)))
    }

    fun setSurfaceMode(platformId: String, surfaceId: String, mode: BlockMode) = updateSettings { settings ->
        val platform = settings.platforms[platformId] ?: return@updateSettings settings
        val surface = platform.surfaces[surfaceId] ?: SurfaceConfig(mode)
        settings.copy(
            platforms = settings.platforms + (
                platformId to platform.copy(
                    surfaces = platform.surfaces + (surfaceId to surface.copy(mode = mode)),
                    enabled = if (mode == BlockMode.OFF && !platform.enabled) platform.enabled else true,
                )
                ),
        )
    }

    fun updateSchedule(transform: (ScheduleConfig) -> ScheduleConfig) = updateSettings {
        it.copy(schedule = transform(it.schedule))
    }

    fun startFocus(minutes: Int) {
        val now = System.currentTimeMillis()
        io.github.dharma_labs.openeye.domain.BypassStore.clear()
        updateSettings {
            it.copy(
                focus = FocusConfig(
                    active = true,
                    startEpochMs = now,
                    endEpochMs = now + minutes * 60_000L,
                    durationMinutes = minutes,
                ),
            )
        }
    }

    fun stopFocus() = updateSettings { it.copy(focus = it.focus.copy(active = false, endEpochMs = 0L)) }

    /**
     * The stop rule is signed with a timestamp on purpose. The block screen says "you set this on
     * a sunday afternoon", which is a person talking to a person, not a limit being enforced.
     */
    fun signContract(minutes: Int, why: String = "") = updateSettings { settings ->
        val trimmed = minutes.coerceIn(0, 600)
        settings.copy(
            contract = settings.contract.copy(
                stopRuleMinutes = trimmed,
                signedEpochMs = System.currentTimeMillis(),
                why = why.trim().take(160),
            ),
        )
    }

    fun setUsualFeeling(feeling: String) = updateSettings { settings ->
        settings.copy(contract = settings.contract.copy(usualFeeling = feeling.trim().take(40)))
    }

    fun addPlan(trigger: String, action: String) = updateSettings { settings ->
        val cleanTrigger = trigger.trim().take(140)
        val cleanAction = action.trim().take(140)
        if (cleanTrigger.isEmpty() || cleanAction.isEmpty()) return@updateSettings settings
        settings.copy(
            plans = settings.plans + ImplementationPlan(
                id = UUID.randomUUID().toString(),
                trigger = cleanTrigger,
                action = cleanAction,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    fun removePlan(id: String) = updateSettings { settings ->
        settings.copy(plans = settings.plans.filterNot { it.id == id })
    }

    fun setPlanEnabled(id: String, enabled: Boolean) = updateSettings { settings ->
        settings.copy(plans = settings.plans.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun logFeeling(feeling: String, minutes: Long, source: String) = updateSettings { settings ->
        val clean = feeling.trim().lowercase().take(24)
        if (clean.isEmpty()) return@updateSettings settings
        val entry = FeelingCheck(
            epochMs = System.currentTimeMillis(),
            epochDay = LocalDate.now().toEpochDay(),
            feeling = clean,
            minutes = minutes.coerceAtLeast(0L),
            source = source,
        )
        // Same feeling twice in a minute is a double tap, not two check-ins.
        if (settings.feelings.any { it.feeling == clean && it.epochMs > entry.epochMs - 60_000L }) {
            return@updateSettings settings
        }
        settings.copy(feelings = (settings.feelings + entry).takeLast(MAX_FEELINGS))
    }

    fun advancePromptCursor() = updateSettings { settings ->
        settings.copy(promptCursor = if (settings.promptCursor >= Int.MAX_VALUE - 1) 0 else settings.promptCursor + 1)
    }

    fun markLapseHandled(day: Long) = updateSettings { it.copy(lapseHandledDay = day) }

    fun markPlanAsked() = updateSettings { it.copy(planAskedInOnboarding = true) }

    fun clearAll() {
        _settings.value = Settings()
        _events.value = emptyList()
        _watch.value = emptyList()
        io.github.dharma_labs.openeye.domain.BypassStore.clear()
        scope.launch {
            runCatching { appContext.openMindStore.edit { it.clear() } }
        }
    }

    fun exportSummary(): String {
        val s = _settings.value
        val events = _events.value
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val blocksToday = events.count {
            it.outcome == "BLOCKED" && Instant.ofEpochMilli(it.epochMs).atZone(zone).toLocalDate() == today
        }
        val watch = todayWatch()
        val lines = buildString {
            appendLine("Openeye")
            appendLine("generated ${Instant.now().atZone(zone)}")
            appendLine()
            appendLine("feed time today: ${watch.sumOf { it.millis } / 60_000L} min, ${watch.sumOf { it.count }} clips")
            appendLine("pulled out of a feed today: $blocksToday")
            appendLine("let through on purpose (30 days): ${events.count { it.outcome == "BYPASSED" }}")
            if (s.contract.stopRuleMinutes > 0) {
                appendLine("your number: ${s.contract.stopRuleMinutes} min a day")
            }
            if (s.plans.any { it.enabled }) {
                appendLine("your plans:")
                s.plans.filter { it.enabled }.forEach { appendLine("  when ${it.trigger} -> ${it.action}") }
            }
            if (s.feelings.isNotEmpty()) {
                val heavy = s.feelings.takeLast(30).count {
                    it.feeling in setOf("flat", "numb", "meh", "anxious", "wired")
                }
                appendLine("check-ins: ${s.feelings.size} total, $heavy of the last 30 felt flat-ish")
            }
            appendLine()
            appendLine("shields")
            s.platforms.forEach { (id, platform) ->
                val name = PlatformCatalog.byId(id)?.name ?: id
                appendLine("- $name: ${if (platform.enabled) "on" else "off"}")
            }
            appendLine("- schedule: ${if (s.schedule.enabled) "on" else "off"}")
            appendLine("- focus: ${if (s.focus.active) "${s.focus.durationMinutes} min running" else "idle"}")
            appendLine()
            appendLine("stays on this device. no account, no cloud, no streaks.")
        }
        return lines
    }

    companion object {
        private const val MAX_EVENTS = 1000
        private const val MAX_FEELINGS = 200
        private val SETTINGS_KEY = stringPreferencesKey("settings_json_v1")
        private val EVENTS_KEY = stringPreferencesKey("events_json_v1")
        private val WATCH_KEY = stringPreferencesKey("watch_json_v1")

        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context).also { instance = it }
            }
    }
}