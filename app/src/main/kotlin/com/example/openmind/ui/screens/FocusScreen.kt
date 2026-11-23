package com.example.openmind.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ProgressRing
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun FocusScreen(
    settings: Settings,
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val focus = settings.focus
    val active = focus.active && focus.endEpochMs > System.currentTimeMillis()
    var selectedDuration by rememberSaveable { mutableIntStateOf(25) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var customOpen by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(30) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        ScreenHeader(
            title = "focus",
            subtitle = "one block, one thing.",
            onBack = onBack,
        )
        Spacer(Modifier.height(24.dp))

        if (active) {
            val total = (focus.endEpochMs - focus.startEpochMs).coerceAtLeast(1L)
            val remaining = (focus.endEpochMs - now).coerceAtLeast(0L)
            val minutes = remaining / 60_000L
            val seconds = (remaining / 1_000L) % 60L
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ProgressRing(
                        progress = remaining.toFloat() / total.toFloat(),
                        modifier = Modifier.size(210.dp),
                        strokeWidth = 12.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("left", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "shields hold while this runs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                    GhostButton(text = "end it early", onClick = onStop)
                }
            }
        } else {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(22.dp)) {
                Column {
                    SectionLabel("how long")
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(15, 25, 45, 90).forEach { minutes ->
                            val selected = selectedDuration == minutes
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) Accent else SurfaceOne,
                                        RoundedCornerShape(18.dp),
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) Accent else Hairline,
                                        RoundedCornerShape(18.dp),
                                    )
                                    .clickable { selectedDuration = minutes }
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = minutes.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (selected) com.example.openmind.ui.theme.Ink else TextPrimary,
                                )
                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) com.example.openmind.ui.theme.Ink.copy(alpha = 0.6f) else TextTertiary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "or pick your own",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier.clickable { customOpen = true },
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(text = "run $selectedDuration min", onClick = { onStart(selectedDuration) })
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("what's off")
        Spacer(Modifier.height(10.dp))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(6.dp)) {
            Column {
                val blocked = PlatformCatalog.all.flatMap { platform ->
                    val config = settings.platforms[platform.id]
                    if (config?.enabled != true) emptyList()
                    else platform.surfaces
                        .filter { (config.surfaces[it.id]?.mode ?: BlockMode.OFF) != BlockMode.OFF }
                        .map { platform to it }
                }
                if (blocked.isEmpty()) {
                    Text(
                        "nothing shielded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(14.dp),
                    )
                } else {
                    blocked.forEachIndexed { index, (platform, surface) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Monogram(platform, size = 34.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${platform.name} · ${surface.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text("strict", style = MaterialTheme.typography.labelSmall, color = Accent)
                        }
                        if (index != blocked.lastIndex) Divider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "it ends on its own.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(30.dp))
    }

    if (customOpen) {
        Dialog(onDismissRequest = { customOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceOne, RoundedCornerShape(28.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                    .padding(22.dp),
            ) {
                Text("your own number", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("-") { customMinutes = (customMinutes - 5).coerceAtLeast(5) }
                    Text(
                        "$customMinutes",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    StepButton("+") { customMinutes = (customMinutes + 5).coerceAtMost(180) }
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = "run $customMinutes min",
                    onClick = { onStart(customMinutes); customOpen = false },
                )
                Spacer(Modifier.height(8.dp))
                GhostButton(text = "cancel", onClick = { customOpen = false })
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(SurfaceOne, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}