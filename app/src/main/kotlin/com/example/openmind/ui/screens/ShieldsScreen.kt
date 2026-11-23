package com.example.openmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.ModeSelector
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.OMSwitch
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.ToggleRow
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

@Composable
fun ShieldsScreen(
    settings: Settings,
    onTogglePlatform: (String, Boolean) -> Unit,
    onSurfaceMode: (String, String, BlockMode) -> Unit,
    onStrictChange: (Boolean) -> Unit,
    onOpenSchedule: () -> Unit,
    onLimitChange: (String, Int) -> Unit,
    onNudgeChange: (Int) -> Unit,
    onLongVideosChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>("instagram") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    ) {
        item {
            ScreenHeader(
                title = "shields",
                subtitle = "the apps stay. the endless part doesn't.",
                trailing = {
                    com.example.openmind.ui.components.NavGlyph(
                        type = "settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp).clickable(onClick = onOpenSettings),
                    )
                },
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "strict mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Pill("no bypass", color = AccentDim)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "locked means locked. everywhere.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        OMSwitch(checked = settings.strictMode, onCheckedChange = onStrictChange)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item { SectionLabel("apps") }
        item { Spacer(Modifier.height(10.dp)) }

        PlatformCatalog.all.forEach { platform ->
            val config = settings.platforms[platform.id]
            item(key = platform.id) {
                OMCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    padding = PaddingValues(0.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = if (expanded == platform.id) null else platform.id }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Monogram(platform, size = 46.dp)
                            Spacer(Modifier.width(13.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    platform.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (config?.enabled == true) {
                                        val shielded = config.surfaces.values.count { it.mode != BlockMode.OFF }
                                        "shield on - $shielded ${if (shielded == 1) "surface" else "surfaces"} covered"
                                    } else {
                                        "shield off - this one does nothing"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (config?.enabled == true) AccentDim else TextTertiary,
                                )
                            }
                            OMSwitch(
                                checked = config?.enabled == true,
                                onCheckedChange = { onTogglePlatform(platform.id, it) },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (expanded == platform.id) "⌃" else "⌄",
                                color = TextTertiary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (expanded == platform.id) {
                            Divider()
                            Column(modifier = Modifier.padding(16.dp)) {
                                platform.surfaces.forEach { surface ->
                                    val mode = config?.surfaces?.get(surface.id)?.mode ?: BlockMode.OFF
                                    Text(
                                        surface.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        surface.blurb,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    ModeSelector(
                                        selected = mode,
                                        onSelect = { onSurfaceMode(platform.id, surface.id, it) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                                Text(
                                    "daily limit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextTertiary,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(0 to "off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h").forEach { (minutes, label) ->
                                        val selected = (config?.dailyLimitMinutes ?: 0) == minutes
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (selected) Accent else SurfaceOne, RoundedCornerShape(100.dp))
                                                .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(100.dp))
                                                .clickable { onLimitChange(platform.id, minutes) }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) Ink else TextSecondary,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "one budget per app. usage access is what lets us warn you at 80%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = when {
                                        config?.enabled != true -> "flip the shield on and these stop being suggestions."
                                        settings.strictMode -> "strict is on. everything above is a dead end."
                                        else -> "strict closes the feed instantly. gentle gives you a breath first."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
        item { SectionLabel("reminders") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Text("while you're in there", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nudgeChoices.forEach { minutes ->
                            ChoiceChip(
                                label = if (minutes == 0) "off" else "${minutes}m",
                                selected = settings.nudgeMinutes == minutes,
                                onClick = { onNudgeChange(minutes) },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    ToggleRow(
                        title = "long videos too",
                        subtitle = "whole-app time, measured not estimated",
                        checked = settings.countLongVideos,
                        onCheckedChange = onLongVideosChange,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
        item {
            OMCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSchedule),
                padding = PaddingValues(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("quiet hours", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (settings.schedule.enabled) "your window is running" else "no window claimed yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Text("→", color = Accent, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
        item {
            Text(
                "feeds get covered. your chats and dms stay untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

private val nudgeChoices = listOf(0, 3, 5, 10)

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Accent else SurfaceOne, RoundedCornerShape(100.dp))
            .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else TextSecondary,
        )
    }
}