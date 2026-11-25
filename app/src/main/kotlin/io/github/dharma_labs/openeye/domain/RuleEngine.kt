package io.github.dharma_labs.openeye.domain

import io.github.dharma_labs.openeye.data.BlockMode
import io.github.dharma_labs.openeye.data.BlockReason
import io.github.dharma_labs.openeye.data.PlatformCatalog
import io.github.dharma_labs.openeye.data.Settings
import java.time.Instant
import java.time.ZoneId

data class Decision(
    val block: Boolean,
    val mode: BlockMode = BlockMode.OFF,
    val reason: BlockReason = BlockReason.NONE,
)

/**
 * Pure decision layer. No Android dependencies so it can be exhaustively unit-tested.
 *
 * Precedence: master switch -> focus session -> schedule -> intentional bypass -> surface rule.
 */
object RuleEngine {

    fun decide(
        platformId: String,
        surfaceId: String,
        settings: Settings,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        bypassUntilMillis: Long? = null,
    ): Decision {
        if (!settings.masterEnabled) return Decision(false)

        val platform = settings.platforms[platformId]
        if (platform == null || !platform.enabled) return Decision(false)
        val surface = platform.surfaces[surfaceId]
        if (surface == null || surface.mode == BlockMode.OFF) return Decision(false)

        if (settings.focus.active && nowMillis < settings.focus.endEpochMs) {
            return Decision(true, BlockMode.STRICT, BlockReason.FOCUS)
        }

        if (isScheduleActive(settings, nowMillis, zone)) {
            return Decision(true, settings.schedule.mode, BlockReason.SCHEDULE)
        }

        val effectiveMode = if (settings.strictMode) BlockMode.STRICT else surface.mode
        if (effectiveMode == BlockMode.OFF) return Decision(false)

        // A bypass only ever applies to an intentional surface, never to strict mode.
        if (effectiveMode != BlockMode.STRICT && bypassUntilMillis != null && nowMillis < bypassUntilMillis) {
            return Decision(false)
        }

        return Decision(true, effectiveMode, BlockReason.SHIELD)
    }

    fun isScheduleActive(settings: Settings, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val schedule = settings.schedule
        if (!schedule.enabled || schedule.startMinute == schedule.endMinute) return false
        val zoned = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val minuteOfDay = zoned.hour * 60 + zoned.minute
        val today = zoned.dayOfWeek.value
        val previousDay = if (today == 1) 7 else today - 1

        return if (schedule.startMinute < schedule.endMinute) {
            minuteOfDay in schedule.startMinute until schedule.endMinute && today in schedule.days
        } else {
            // Overnight window, e.g. 22:30 -> 07:00.
            if (minuteOfDay >= schedule.startMinute) {
                today in schedule.days
            } else if (minuteOfDay < schedule.endMinute) {
                previousDay in schedule.days
            } else {
                false
            }
        }
    }

    fun surfaceLabel(platformId: String, surfaceId: String): String =
        PlatformCatalog.surface(platformId, surfaceId)?.label ?: surfaceId

    fun platformName(platformId: String): String =
        PlatformCatalog.byId(platformId)?.name ?: platformId
}