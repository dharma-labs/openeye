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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.openmind.data.ScheduleConfig
import com.example.openmind.data.Settings
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.OMSwitch
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.WeekdayChip
import com.example.openmind.ui.formatClock
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceTwo
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

private val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

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
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        ScreenHeader(
            title = "quiet hours",
            subtitle = "the hours you claimed. nothing opens here.",
            onBack = onBack,
        )
        Spacer(Modifier.height(18.dp))
        SectionLabel("routines")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        Spacer(Modifier.height(22.dp))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "run the schedule",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    OMSwitch(
                        checked = schedule.enabled,
                        onCheckedChange = { enabled -> onUpdate { it.copy(enabled = enabled) } },
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("days")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dayLetters.forEachIndexed { index, letter ->
                val day = index + 1
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
        Spacer(Modifier.height(24.dp))
        SectionLabel("window")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Spacer(Modifier.height(14.dp))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Pill("strict", color = Accent, filled = true)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "no gentle mode, no breathing timer in here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (schedule.startMinute > schedule.endMinute) {
                "this window crosses midnight. it ends the next morning."
            } else {
                "times follow your device clock."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        Spacer(Modifier.height(24.dp))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
            Column {
                Text("how it behaves", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                listOf(
                    "schedule beats gentle mode. focus beats everything.",
                    "needs Accessibility access to close a feed.",
                ).forEach { line ->
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("·", color = Accent, modifier = Modifier.width(14.dp))
                        Text(line, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
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
private fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(SurfaceOne, RoundedCornerShape(22.dp))
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceOne, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(22.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper(
                    value = hour.toString().padStart(2, '0'),
                    onMinus = { hour = if (hour == 0) 23 else hour - 1 },
                    onPlus = { hour = if (hour == 23) 0 else hour + 1 },
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    value = minute.toString().padStart(2, '0'),
                    onMinus = { minute = if (minute == 0) 55 else minute - 5 },
                    onPlus = { minute = if (minute == 55) 0 else minute + 5 },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "set ${formatClock(hour * 60 + minute)}",
                onClick = { onConfirm(hour * 60 + minute) },
            )
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "cancel", onClick = onDismiss)
        }
    }
}

@Composable
private fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Ink, RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepButton(text = "⌃", onClick = onPlus)
        Text(value, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        StepButton(text = "⌄", onClick = onMinus)
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(SurfaceTwo, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextSecondary, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}
@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(SurfaceOne, RoundedCornerShape(100.dp))
            .border(1.dp, Hairline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}