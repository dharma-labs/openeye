package com.example.openmind.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.openmind.data.AppRepository
import com.example.openmind.data.BlockMode
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.domain.AttentionStats
import com.example.openmind.domain.BypassStore
import com.example.openmind.domain.Coach
import com.example.openmind.domain.RuleEngine
import com.example.openmind.domain.TimeTranslation
import com.example.openmind.domain.Voice
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ProgressRing
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.EyeShape
import com.example.openmind.ui.theme.EyeSpace
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.MinTouch
import com.example.openmind.ui.theme.OpenEyeTheme
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceThree
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * The interrupt. Its job is not to punish, it is to insert a gap between the cue and the swipe,
 * because the habit is cue-driven and the first seconds are automatic.
 *
 * Order matters: pause first (the ring has to finish before anything opens), then the person's own
 * precommitment quoted back, then an affect label, then an exit that is always one tap away.
 * Copy rotates on a stored cursor so it never reads the same way twice in a row.
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = AppRepository.get(this)
        val themeMode = repo.settings.value.themeMode
        val systemNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> systemNight
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        val platformId = intent.getStringExtra(EXTRA_PLATFORM) ?: PlatformCatalog.INSTAGRAM
        val surfaceId = intent.getStringExtra(EXTRA_SURFACE) ?: "reels"
        val mode = runCatching { BlockMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: "STRICT") }
            .getOrDefault(BlockMode.STRICT)
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "SHIELD"

        // Snapshot before advancing, so this appearance shows a line the last one did not.
        val shownSettings = repo.settings.value
        val todayMillis = AttentionStats.forDay(repo.watch.value, LocalDate.now().toEpochDay()).millis
        val cursor = shownSettings.promptCursor
        repo.advancePromptCursor()

        setContent {
            OpenEyeTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    BlockScreen(
                        platformId = platformId,
                        surfaceId = surfaceId,
                        mode = mode,
                        reason = reason,
                        settings = shownSettings,
                        todayMillis = todayMillis,
                        cursor = cursor,
                        onLeave = { goHome() },
                        onBypass = { feeling, hope ->
                            BypassStore.allow(platformId, surfaceId, 5)
                            repo.logEvent(
                                platformId = platformId,
                                surfaceId = surfaceId,
                                mode = mode.name,
                                outcome = "BYPASSED",
                                intention = hope?.takeIf { it.isNotBlank() } ?: feeling,
                            )
                            if (!feeling.isNullOrBlank()) repo.logFeeling(feeling, todayMillis, "block")
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun goHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_PLATFORM = "platform"
        const val EXTRA_SURFACE = "surface"
        const val EXTRA_MODE = "mode"
        const val EXTRA_REASON = "reason"
    }
}

@Composable
private fun BlockScreen(
    platformId: String,
    surfaceId: String,
    mode: BlockMode,
    reason: String,
    settings: Settings,
    todayMillis: Long,
    cursor: Int,
    onLeave: () -> Unit,
    onBypass: (feeling: String?, hope: String?) -> Unit,
) {
    val platform = PlatformCatalog.byId(platformId)
    val surfaceLabel = if (surfaceId == "app") "daily limit" else RuleEngine.surfaceLabel(platformId, surfaceId)
    val strict = mode == BlockMode.STRICT || reason == "FOCUS" || reason == "LIMIT"
    val plan = Coach.activePlan(settings)
    val contract = settings.contract
    val line = Coach.blockLine(reason, strict, cursor, todayMillis, contract)
    var seconds by remember { mutableIntStateOf(if (strict) 0 else 8) }
    var feeling by remember { mutableStateOf<String?>(null) }
    var hope by remember { mutableStateOf("") }

    LaunchedEffect(strict) {
        if (strict) return@LaunchedEffect
        while (seconds > 0) {
            delay(1_000L)
            seconds -= 1
        }
    }

    BackHandler { onLeave() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.10f), Ink, Ink),
                    radius = 1100f,
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = EyeSpace.xxl, vertical = EyeSpace.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                EyeMark(modifier = Modifier.size(20.dp), color = Accent)
                Spacer(Modifier.width(EyeSpace.sm))
                Text(
                    "openeye",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.weight(1f),
                )
                Pill(
                    text = when {
                        reason == "FOCUS" -> "focus running"
                        reason == "LIMIT" -> "limit reached"
                        reason == "SCHEDULE" -> "quiet hours"
                        strict -> "locked"
                        else -> "paused"
                    },
                    color = if (strict) Accent else AccentDim,
                )
            }

            Spacer(Modifier.weight(1f))
            if (platform != null) Monogram(platform, size = 46.dp)
            Spacer(Modifier.height(EyeSpace.lg))
            Text(
                text = surfaceLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(EyeSpace.md))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            if (!strict) {
                Spacer(Modifier.height(EyeSpace.xxl))
                ProgressRing(
                    progress = if (seconds == 0) 1f else (8 - seconds) / 8f,
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 5.dp,
                    color = if (seconds == 0) Accent else AccentDim,
                    track = SurfaceThree,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (seconds == 0) "your call" else seconds.toString(),
                            style = if (seconds == 0) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.displayMedium
                            },
                            color = if (seconds == 0) Accent else TextPrimary,
                        )
                        Text(
                            if (seconds == 0) "choose either way" else "breathing",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(EyeSpace.xl))
                Text(
                    text = when {
                        reason == "FOCUS" -> "this opens when your timer ends."
                        reason == "LIMIT" -> "budget's spent. resets at midnight."
                        else -> "$surfaceLabel stays covered in this mode."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            if (contract.stopRuleMinutes > 0 || plan != null) {
                Spacer(Modifier.height(EyeSpace.lg))
                OMCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(EyeSpace.lg),
                    raised = false,
                ) {
                    Column {
                        if (contract.stopRuleMinutes > 0) {
                            Text(
                                "your number: ${contract.stopRuleMinutes}m. today: ${TimeTranslation.hoursText(todayMillis)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        if (plan != null) {
                            if (contract.stopRuleMinutes > 0) Spacer(Modifier.height(EyeSpace.md))
                            Text(
                                "your plan: ${plan.trigger}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                            Spacer(Modifier.height(EyeSpace.xxs))
                            Text(
                                plan.action,
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentDim,
                            )
                        }
                    }
                }
            }

            if (!strict) {
                Spacer(Modifier.height(EyeSpace.xl))
                Text(Voice.feelPrompt, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Spacer(Modifier.height(EyeSpace.md))
                Column(verticalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                    Voice.feelWords.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { label ->
                                val selected = feeling == label
                                val shape = RoundedCornerShape(EyeShape.chip)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = MinTouch)
                                        .background(if (selected) Accent else SurfaceOne, shape)
                                        .border(1.dp, if (selected) Accent else Hairline, shape)
                                        .eyePress(onClick = { feeling = if (selected) null else label }),
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
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(EyeSpace.md))
                TextField(
                    value = hope,
                    onValueChange = { if (it.length <= 90) hope = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "what were you hoping for?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    // The field paints its own shape. Wrapping a default TextField in a second
                    // background/border leaves the old 28dp outline showing through the corners.
                    shape = RoundedCornerShape(EyeShape.field),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceOne,
                        unfocusedContainerColor = SurfaceOne,
                        cursorColor = Accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.weight(1f))
            if (!strict) {
                PrimaryButton(
                    text = if (seconds > 0) "hold on" else "open for 5 min",
                    onClick = { if (seconds == 0) onBypass(feeling, hope) },
                    enabled = seconds == 0,
                    icon = if (seconds > 0) null else "play",
                )
                Spacer(Modifier.height(EyeSpace.xxs))
                // The exit gets the bigger target, not the smaller one. When someone is mid-urge they
                // scan for the way out; a low-contrast text row is exactly what gets missed.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouch + 8.dp)
                        .eyePress(onClick = onLeave),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("nah, go back", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                }
            } else {
                PrimaryButton(text = "close it", onClick = onLeave, icon = "check")
                Spacer(Modifier.height(EyeSpace.sm))
                Text(
                    "back's off too. that's this mode.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            Spacer(Modifier.height(EyeSpace.xs))
        }
    }
}
