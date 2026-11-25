package com.example.openmind.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.EyeIcon
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.IconTile
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ProgressRing
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.EyeShape
import com.example.openmind.ui.theme.EyeSpace
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.MinTouch
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceThree
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Focus is the only screen in the app that asks for nothing.
 *
 * Four numbers, one button. Choosing a length is the whole decision, so nothing else is allowed on
 * the same screen as it - no stats, no streaks. Once it is running the clock is the only element that
 * moves, which is what tells the person the app is holding the phone for them rather than watching.
 */
@Composable
fun FocusScreen(
    settings: Settings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
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
            .padding(EyeSpace.screen),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        ScreenHeader(
            title = "focus",
            subtitle = "one block, one thing.",
            trailing = {
                IconTile(
                    type = "settings",
                    tint = TextSecondary,
                    size = 40.dp,
                    modifier = Modifier.eyePress(onClick = onOpenSettings),
                )
            },
        )

        if (active) {
            val total = (focus.endEpochMs - focus.startEpochMs).coerceAtLeast(1L)
            val remaining = (focus.endEpochMs - now).coerceAtLeast(0L)
            val minutes = remaining / 60_000L
            val seconds = (remaining / 1_000L) % 60L
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xxl), glow = Accent) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ProgressRing(
                        progress = remaining.toFloat() / total.toFloat(),
                        modifier = Modifier.size(208.dp),
                        strokeWidth = 12.dp,
                        track = SurfaceThree,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(EyeSpace.xs))
                            Text("left", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                    Spacer(Modifier.height(EyeSpace.xl))
                    Text(
                        "shields hold while this runs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(EyeSpace.lg))
                    GhostButton(text = "end it early", onClick = onStop, icon = "close")
                }
            }
        } else {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
                Column {
                    SectionLabel("how long")
                    Spacer(Modifier.height(EyeSpace.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm), modifier = Modifier.fillMaxWidth()) {
                        durations.forEach { minutes ->
                            DurationTile(
                                minutes = minutes,
                                selected = selectedDuration == minutes,
                                onClick = { selectedDuration = minutes },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(EyeSpace.md))
                    Text(
                        "or pick your own",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouch)
                            .eyePress(onClick = { customOpen = true }),
                    )
                    Spacer(Modifier.height(EyeSpace.sm))
                    PrimaryButton(text = "run $selectedDuration min", onClick = { onStart(selectedDuration) }, icon = "play")
                }
            }
        }

        SectionLabel("what's off")
        BlockedList(settings)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
            EyeIcon("check", tint = TextTertiary, size = 15.dp)
            Text("it ends on its own.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }

    if (customOpen) {
        Dialog(
            onDismissRequest = { customOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            OMCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EyeSpace.screen),
                padding = PaddingValues(EyeSpace.xl),
            ) {
                Column {
                    SectionLabel("your own number")
                    Spacer(Modifier.height(EyeSpace.lg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepButton("minus") { customMinutes = (customMinutes - 5).coerceAtLeast(5) }
                        Text(
                            "$customMinutes",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                        StepButton("plus") { customMinutes = (customMinutes + 5).coerceAtMost(180) }
                    }
                    Spacer(Modifier.height(EyeSpace.lg))
                    PrimaryButton(
                        text = "run $customMinutes min",
                        onClick = {
                            onStart(customMinutes)
                            customOpen = false
                        },
                    )
                    Spacer(Modifier.height(EyeSpace.sm))
                    GhostButton(text = "cancel", onClick = { customOpen = false })
                }
            }
        }
    }
}

private val durations = listOf(15, 25, 45, 90)

@Composable
private fun DurationTile(minutes: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EyeShape.control)
    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .background(if (selected) Accent else SurfaceOne, shape)
            .border(1.dp, if (selected) Accent else Hairline, shape)
            .eyePress(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = minutes.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (selected) Ink else TextPrimary,
        )
        Text(
            text = "min",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Ink.copy(alpha = 0.65f) else TextTertiary,
        )
    }
}

@Composable
private fun BlockedList(settings: Settings) {
    val blocked = PlatformCatalog.all.flatMap { platform ->
        val config = settings.platforms[platform.id]
        if (config?.enabled != true) emptyList()
        else platform.surfaces
            .filter { surface -> (config.surfaces[surface.id]?.mode ?: BlockMode.OFF) != BlockMode.OFF }
            .map { surface -> platform to surface }
    }
    OMCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(if (blocked.isEmpty()) EyeSpace.lg else 0.dp),
    ) {
        Column {
            if (blocked.isEmpty()) {
                Text(
                    "nothing shielded yet. focus still works - it just won't feel firm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            } else {
                blocked.forEachIndexed { index, (platform, surface) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouch)
                            .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Monogram(platform, size = 32.dp)
                        Spacer(Modifier.width(EyeSpace.md))
                        Text(
                            "${platform.name} · ${surface.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Text("held", style = MaterialTheme.typography.labelSmall, color = Accent)
                    }
                    if (index != blocked.lastIndex) Divider(modifier = Modifier.padding(horizontal = EyeSpace.lg))
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(EyeShape.field)
    Box(
        modifier = Modifier
            .size(MinTouch)
            .background(SurfaceOne, shape)
            .border(1.dp, Hairline, shape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        EyeIcon(icon, tint = TextPrimary, size = 20.dp)
    }
}
