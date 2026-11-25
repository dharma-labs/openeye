package io.github.dharma_labs.openeye.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.dharma_labs.openeye.data.ScheduleConfig
import io.github.dharma_labs.openeye.data.Settings
import io.github.dharma_labs.openeye.ui.components.EyeIcon
import io.github.dharma_labs.openeye.ui.components.GhostButton
import io.github.dharma_labs.openeye.ui.components.OMCard
import io.github.dharma_labs.openeye.ui.components.Pill
import io.github.dharma_labs.openeye.ui.components.PrimaryButton
import io.github.dharma_labs.openeye.ui.components.ScreenHeader
import io.github.dharma_labs.openeye.ui.components.SectionLabel
import io.github.dharma_labs.openeye.ui.components.ToggleRow
import io.github.dharma_labs.openeye.ui.components.WeekdayChip
import io.github.dharma_labs.openeye.ui.components.eyePress
import io.github.dharma_labs.openeye.ui.formatClock
import io.github.dharma_labs.openeye.ui.theme.Accent
import io.github.dharma_labs.openeye.ui.theme.AccentDim
import io.github.dharma_labs.openeye.ui.theme.EyeShape
import io.github.dharma_labs.openeye.ui.theme.EyeSpace
import io.github.dharma_labs.openeye.ui.theme.Hairline
import io.github.dharma_labs.openeye.ui.theme.Ink
import io.github.dharma_labs.openeye.ui.theme.MinTouch
import io.github.dharma_labs.openeye.ui.theme.SurfaceOne
import io.github.dharma_labs.openeye.ui.theme.SurfaceTwo
import io.github.dharma_labs.openeye.ui.theme.TextPrimary
import io.github.dharma_labs.openeye.ui.theme.TextSecondary
import io.github.dharma_labs.openeye.ui.theme.TextTertiary

private val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Quiet hours are a promise made in advance, so the screen reads as a promise: what you claimed,
 * which days, and what happens at the edge. The three presets exist because deciding a window from
 * scratch is the part people abandon.
 */
@Composable
fun ScheduleScreen(
    settings: Settings,
    onBack: () -> Unit,
    onUpdate: ((ScheduleConfig) -> ScheduleConfig) -> Unit,
) {
    val schedule = settings.schedule
    var editing by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(EyeSpace.screen),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        ScreenHeader(
            title = "quiet hours",
            subtitle = "the hours you claimed. nothing opens here.",
            onBack = onBack,
        )

        Column {
            SectionLabel("routines")
            Spacer(Modifier.height(EyeSpace.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                PresetChip("study", Modifier.weight(1f)) {
                    onUpdate { it.copy(enabled = true, days = setOf(1, 2, 3, 4, 5), startMinute = 17 * 60, endMinute = 21 * 60) }
                }
                PresetChip("work", Modifier.weight(1f)) {
                    onUpdate { it.copy(enabled = true, days = setOf(1, 2, 3, 4, 5), startMinute = 9 * 60, endMinute = 18 * 60) }
                }
                PresetChip("sleep", Modifier.weight(1f)) {
                    onUpdate { it.copy(enabled = true, days = setOf(1, 2, 3, 4, 5, 6, 7), startMinute = 22 * 60 + 30, endMinute = 7 * 60) }
                }
            }
        }

        OMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(horizontal = EyeSpace.lg, vertical = EyeSpace.xs),
            glow = if (schedule.enabled) Accent else null,
        ) {
            ToggleRow(
                title = "run the schedule",
                subtitle = if (schedule.enabled) {
                    "${formatClock(schedule.startMinute).lowercase()} to ${formatClock(schedule.endMinute).lowercase()}"
                } else {
                    "off means these hours are just hours"
                },
                checked = schedule.enabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(enabled = enabled) } },
                icon = "moon",
            )
        }

        Column {
            SectionLabel("days")
            Spacer(Modifier.height(EyeSpace.md))
            Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                dayLetters.forEachIndexed { index, letter ->
                    val day = index + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        WeekdayChip(
                            letter = letter,
                            selected = day in schedule.days,
                            onClick = {
                                onUpdate { current ->
                                    val days = if (day in current.days) current.days - day else current.days + day
                                    current.copy(days = days)
                                }
                            },
                        )
                    }
                }
            }
        }

        Column {
            SectionLabel("window")
            Spacer(Modifier.height(EyeSpace.md))
            Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.md)) {
                TimeField(
                    label = "starts",
                    value = formatClock(schedule.startMinute),
                    modifier = Modifier.weight(1f),
                    onClick = { editing = "start" },
                )
                TimeField(
                    label = "ends",
                    value = formatClock(schedule.endMinute),
                    modifier = Modifier.weight(1f),
                    onClick = { editing = "end" },
                )
            }
            Spacer(Modifier.height(EyeSpace.sm))
            Text(
                text = if (schedule.startMinute > schedule.endMinute) {
                    "this window crosses midnight. it ends the next morning."
                } else {
                    "times follow your device clock."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }

        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "how it behaves",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Pill("strict", color = Accent, filled = true)
                }
                Spacer(Modifier.height(EyeSpace.md))
                BehaviourLine("schedule beats gentle mode. focus beats everything.")
                BehaviourLine("needs screen access to close a feed.")
                BehaviourLine("no breathing timer in here.")
            }
        }
    }

    val target = editing
    if (target != null) {
        TimeStepperDialog(
            title = if (target == "start") "start time" else "end time",
            initialMinute = if (target == "start") schedule.startMinute else schedule.endMinute,
            onDismiss = { editing = null },
            onConfirm = { minute ->
                onUpdate {
                    if (target == "start") it.copy(startMinute = minute) else it.copy(endMinute = minute)
                }
                editing = null
            },
        )
    }
}

@Composable
private fun BehaviourLine(text: String) {
    Row(
        modifier = Modifier.padding(bottom = EyeSpace.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .padding(top = 6.dp)
                .background(AccentDim, CircleShape),
        )
        Spacer(Modifier.width(EyeSpace.md))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(EyeShape.cardLarge)
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .background(SurfaceOne, shape)
            .border(1.dp, Hairline, shape)
            .eyePress(onClick = onClick)
            .padding(EyeSpace.lg),
    ) {
        SectionLabel(label)
        Spacer(Modifier.height(EyeSpace.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            EyeIcon("clock", tint = TextTertiary, size = 18.dp)
        }
    }
}

@Composable
private fun TimeStepperDialog(
    title: String,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by remember { mutableIntStateOf((initialMinute / 60) % 24) }
    var minute by remember { mutableIntStateOf((initialMinute % 60 / 5) * 5) }

    Dialog(onDismissRequest = onDismiss) {
        OMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(EyeSpace.xl),
            raised = false,
        ) {
            Column {
                SectionLabel(title)
                Spacer(Modifier.height(EyeSpace.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.md)) {
                    Stepper(
                        label = "hour",
                        value = hour.toString().padStart(2, '0'),
                        onMinus = { hour = if (hour == 0) 23 else hour - 1 },
                        onPlus = { hour = if (hour == 23) 0 else hour + 1 },
                        modifier = Modifier.weight(1f),
                    )
                    Stepper(
                        label = "minute",
                        value = minute.toString().padStart(2, '0'),
                        onMinus = { minute = if (minute == 0) 55 else minute - 5 },
                        onPlus = { minute = if (minute == 55) 0 else minute + 5 },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(EyeSpace.lg))
                PrimaryButton(
                    text = "set ${formatClock(hour * 60 + minute)}",
                    onClick = { onConfirm(hour * 60 + minute) },
                )
                Spacer(Modifier.height(EyeSpace.sm))
                GhostButton(text = "cancel", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Ink, RoundedCornerShape(EyeShape.control))
            .border(1.dp, Hairline, RoundedCornerShape(EyeShape.control))
            .padding(vertical = EyeSpace.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepButton("chevron_up", onClick = onPlus)
        Text(value, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        StepButton("chevron", onClick = onMinus)
    }
}

@Composable
private fun StepButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(MinTouch)
            .background(SurfaceTwo, CircleShape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        EyeIcon(icon, tint = TextSecondary, size = 20.dp)
    }
}

@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(EyeShape.chip)
    Box(
        modifier = modifier
            .heightIn(min = MinTouch)
            .background(SurfaceOne, shape)
            .border(1.dp, Hairline, shape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}
