package com.example.openmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.openmind.data.BlockMode
import com.example.openmind.data.Platform
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.PlatformConfig
import com.example.openmind.data.Settings
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.DisclosureRow
import com.example.openmind.ui.components.EyeIcon
import com.example.openmind.ui.components.IconTile
import com.example.openmind.ui.components.ModeSelector
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.OMSwitch
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.Revealed
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.ToggleRow
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.AccentSoft
import com.example.openmind.ui.theme.EyeShape
import com.example.openmind.ui.theme.EyeSpace
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.MinTouch
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import com.example.openmind.ui.theme.Warn

/**
 * Shields answers one question before the person scrolls: is it working right now?
 *
 * The card at the top is the state of the phone, not a menu. Everything below it is an edit. People
 * check a protection app the way they check a lock on a door - a glance, then they walk away - so the
 * glance has to be the first thing on the screen, and it has to be true.
 */
@Composable
fun ShieldsScreen(
    settings: Settings,
    permissions: PermissionState,
    onTogglePlatform: (String, Boolean) -> Unit,
    onSurfaceMode: (String, String, BlockMode) -> Unit,
    onStrictChange: (Boolean) -> Unit,
    onOpenSchedule: () -> Unit,
    onLimitChange: (String, Int) -> Unit,
    onGrantUsage: () -> Unit,
    onNudgeChange: (Int) -> Unit,
    onLongVideosChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var showMoreApps by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    val coreApps = setOf(PlatformCatalog.INSTAGRAM, PlatformCatalog.YOUTUBE, PlatformCatalog.FACEBOOK)
    val shownApps = PlatformCatalog.all.filter { showMoreApps || it.id in coreApps }
    val armed = armedSurfaces(settings)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = EyeSpace.screen,
            top = EyeSpace.lg,
            end = EyeSpace.screen,
            bottom = EyeSpace.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        item {
            ScreenHeader(
                title = "shields",
                subtitle = "the apps stay. the endless part doesn't.",
                trailing = {
                    IconTile(
                        type = "settings",
                        tint = TextSecondary,
                        size = 40.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(EyeShape.chip))
                            .eyePress(onClick = onOpenSettings),
                    )
                },
            )
        }

        item { ShieldState(armed, settings.masterEnabled) }

        item {
            OMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
            ) {
                ToggleRow(
                    title = "strict mode",
                    subtitle = if (settings.strictMode) "no way through, not even a tap" else "one tap lets you through",
                    checked = settings.strictMode,
                    onCheckedChange = onStrictChange,
                    icon = "lock",
                )
            }
        }

        item {
            SectionHeader(
                text = "apps",
                action = if (showMoreApps) null else "add apps",
                onAction = { showMoreApps = true },
            )
        }

        shownApps.forEach { platform ->
            val config = settings.platforms[platform.id]
            item(key = platform.id) {
                AppCard(
                    platform = platform,
                    config = config,
                    expanded = expanded == platform.id,
                    usageAccess = permissions.usageAccess,
                    onExpand = { expanded = if (expanded == platform.id) null else platform.id },
                    onToggle = { onTogglePlatform(platform.id, it) },
                    onSurfaceMode = { surfaceId, mode -> onSurfaceMode(platform.id, surfaceId, mode) },
                    onLimitChange = { minutes -> onLimitChange(platform.id, minutes) },
                    onGrantUsage = onGrantUsage,
                )
            }
        }

        item {
            OMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .eyePress(onClick = onOpenSchedule)
                        .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(
                        type = "moon",
                        tint = if (settings.schedule.enabled) Accent else TextSecondary,
                        size = 36.dp,
                    )
                    Spacer(Modifier.width(EyeSpace.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("quiet hours", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (settings.schedule.enabled) {
                                "on from ${clockLabel(settings.schedule.startMinute)} to ${clockLabel(settings.schedule.endMinute)}"
                            } else {
                                "the feeds wait until morning"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Pill(
                        text = if (settings.schedule.enabled) "on" else "off",
                        color = if (settings.schedule.enabled) AccentDim else TextTertiary,
                    )
                    Spacer(Modifier.width(EyeSpace.sm))
                    EyeIcon("chevron", tint = TextTertiary, size = 16.dp)
                }
            }
        }

        item {
            DisclosureRow(
                label = "nudges and counting",
                expanded = showAdvanced,
                onClick = { showAdvanced = !showAdvanced },
                hint = if (showAdvanced) null else "how often you get told",
            )
        }
        item {
            Revealed(expanded = showAdvanced) {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                    Column {
                        SectionLabel("remind me every")
                        Spacer(Modifier.height(EyeSpace.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                            nudgeChoices.forEach { minutes ->
                                ChoiceChip(
                                    label = if (minutes == 0) "off" else "${minutes}m",
                                    selected = settings.nudgeMinutes == minutes,
                                    onClick = { onNudgeChange(minutes) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(EyeSpace.lg))
                        Divider()
                        Spacer(Modifier.height(EyeSpace.lg))
                        ToggleRow(
                            title = "long videos too",
                            subtitle = "count whole apps, not only clips",
                            checked = settings.countLongVideos,
                            onCheckedChange = onLongVideosChange,
                            icon = "play",
                        )
                    }
                }
            }
        }

        item {
            Text(
                "chats and dms stay untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

/** The state of the phone, measured from the same settings the service reads. */
@Composable
private fun ShieldState(armed: List<String>, masterOn: Boolean) {
    val on = masterOn && armed.isNotEmpty()
    OMCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(EyeSpace.xl),
        glow = if (on) Accent else null,
        surface = if (on) SurfaceOne else AccentSoft.copy(alpha = 0.35f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(type = "shield", tint = if (on) Accent else TextTertiary, size = 44.dp, filled = on)
            Spacer(Modifier.width(EyeSpace.lg))
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("right now")
                Spacer(Modifier.height(EyeSpace.sm))
                Text(
                    text = when {
                        !masterOn -> "everything is off"
                        armed.isEmpty() -> "nothing armed"
                        else -> "${armed.size} shield${if (armed.size == 1) "" else "s"} up"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (on) TextPrimary else TextSecondary,
                )
                Spacer(Modifier.height(EyeSpace.xs))
                Text(
                    text = when {
                        !masterOn -> "turn the master switch back on in settings"
                        armed.isEmpty() -> "arm one below and it starts on your next swipe"
                        else -> armed.joinToString(" · ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (masterOn) TextSecondary else Warn,
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    platform: Platform,
    config: PlatformConfig?,
    expanded: Boolean,
    usageAccess: Boolean,
    onExpand: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSurfaceMode: (String, BlockMode) -> Unit,
    onLimitChange: (Int) -> Unit,
    onGrantUsage: () -> Unit,
) {
    val enabled = config?.enabled == true
    val limit = config?.dailyLimitMinutes ?: 0
    OMCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(0.dp),
        raised = expanded,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouch)
                    .eyePress(onClick = onExpand)
                    .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Monogram(platform, size = 36.dp)
                Spacer(Modifier.width(EyeSpace.md))
                Text(
                    platform.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) TextPrimary else TextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Pill(text = platformStateLabel(platform, config), color = if (enabled) AccentDim else TextTertiary)
                Spacer(Modifier.width(EyeSpace.sm))
                OMSwitch(checked = enabled, onCheckedChange = onToggle)
                Spacer(Modifier.width(EyeSpace.xs))
                EyeIcon(
                    type = if (expanded) "chevron_up" else "chevron",
                    tint = TextTertiary,
                    size = 18.dp,
                )
            }
            Revealed(expanded = expanded) {
                Divider()
                Column(modifier = Modifier.padding(EyeSpace.lg)) {
                    platform.surfaces.forEach { surface ->
                        val mode = config?.surfaces?.get(surface.id)?.mode ?: BlockMode.OFF
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (mode == BlockMode.OFF) Hairline else Accent,
                                        RoundedCornerShape(100.dp),
                                    )
                            )
                            Spacer(Modifier.width(EyeSpace.sm))
                            Text(
                                surface.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.height(EyeSpace.sm))
                        ModeSelector(selected = mode, onSelect = { onSurfaceMode(surface.id, it) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(EyeSpace.lg))
                    }

                    SectionLabel("daily limit")
                    Spacer(Modifier.height(EyeSpace.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.xs)) {
                        limitChoices.forEach { (minutes, label) ->
                            ChoiceChip(
                                label = label,
                                selected = limit == minutes,
                                onClick = { onLimitChange(minutes) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (limit > 0 && !usageAccess) {
                        Spacer(Modifier.height(EyeSpace.md))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinTouch)
                                .clip(RoundedCornerShape(EyeShape.field))
                                .background(Warn.copy(alpha = 0.10f))
                                .eyePress(onClick = onGrantUsage)
                                .padding(horizontal = EyeSpace.md, vertical = EyeSpace.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EyeIcon("alert", tint = Warn, size = 18.dp)
                            Spacer(Modifier.width(EyeSpace.sm))
                            Text(
                                "the limit needs usage access",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Warn,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EyeShape.chip)
    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(if (selected) Accent else SurfaceOne, shape)
            .border(1.dp, if (selected) Accent else Hairline, shape)
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.xs, vertical = EyeSpace.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else TextSecondary,
            maxLines = 1,
        )
    }
}

private val nudgeChoices = listOf(0, 3, 5, 10)
private val limitChoices = listOf(0 to "off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h")

/** Which feeds are actually covered, grouped by app. Read from the same map the service uses. */
private fun armedSurfaces(settings: Settings): List<String> =
    PlatformCatalog.all.mapNotNull { platform ->
        val config = settings.platforms[platform.id] ?: return@mapNotNull null
        if (!config.enabled) return@mapNotNull null
        val labels = platform.surfaces
            .filter { surface -> (config.surfaces[surface.id]?.mode ?: BlockMode.OFF) != BlockMode.OFF }
            .map { surface -> surface.label.lowercase() }
        if (labels.isEmpty()) null else "${platform.name.lowercase()}: ${labels.joinToString(", ")}"
    }

private fun clockLabel(minuteOfDay: Int): String {
    val h = minuteOfDay / 60 % 24
    val m = minuteOfDay % 60
    val suffix = if (h < 12) "am" else "pm"
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return if (m == 0) "$hour12$suffix" else "$hour12:${"%02d".format(m)}$suffix"
}

private fun platformStateLabel(platform: Platform, config: PlatformConfig?): String {
    if (config?.enabled != true) return "off"
    val active = platform.surfaces.count { surface ->
        (config.surfaces[surface.id]?.mode ?: BlockMode.OFF) != BlockMode.OFF
    }
    return if (active > 0) "$active on" else "on"
}
