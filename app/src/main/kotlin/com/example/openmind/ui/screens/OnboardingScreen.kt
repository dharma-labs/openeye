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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.openmind.data.AttentionContract
import com.example.openmind.domain.Voice
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

private val stopRuleChoices = listOf(15, 30, 45, 60, 90, 120)

@Composable
fun OnboardingScreen(
    permissions: PermissionState,
    contract: AttentionContract,
    onSetStopRule: (Int) -> Unit,
    onSetUsualFeeling: (String) -> Unit,
    onEnableAccessibility: () -> Unit,
    onEnableUsage: () -> Unit,
    onEnableNotifications: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var pickedMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var pickedFeeling by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = step > 0) { step -= 1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.10f), Ink, Ink),
                    radius = 900f,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("openeye", style = MaterialTheme.typography.labelSmall, color = Accent, modifier = Modifier.weight(1f))
                Text(
                    "skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.clickable(onClick = onFinish).padding(6.dp),
                )
            }
            Spacer(Modifier.height(40.dp))

            when (step) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SurfaceOne, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        EyeMark(modifier = Modifier.size(38.dp), color = Accent)
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "the feed isn't\naccidentally endless.",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "built like a slot machine. most swipes pay nothing, so you keep pulling.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(30.dp))
                    listOf(
                        "covers reels, shorts, explore. not the whole app.",
                        "counts your time in real units.",
                        "nothing leaves your phone. no account, no internet.",
                    ).forEach { line ->
                        Row(modifier = Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Accent, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(line, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }

                2 -> {
                    Text(
                        "two questions, while you're\ncalm about it.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "answer now. i'll quote them back later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(26.dp))
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("what's a number you'd believe?")
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "not zero. zero gets broken and ignored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                            Spacer(Modifier.height(16.dp))
                            ChipGrid(
                                options = stopRuleChoices.map { "$it min" },
                                selected = pickedMinutes?.let { "${it} min" } ?: contract.stopRuleMinutes
                                    .takeIf { rule -> rule > 0 }?.let { "$it min" },
                                onSelect = { label ->
                                    val minutes = label.removeSuffix(" min").toIntOrNull() ?: return@ChipGrid
                                    pickedMinutes = minutes
                                    onSetStopRule(minutes)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("how does a long scroll usually leave you?")
                            Spacer(Modifier.height(12.dp))
                            Text(
                                Voice.feelPromptLong,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                            Spacer(Modifier.height(16.dp))
                            ChipGrid(
                                options = Voice.feelWords,
                                selected = pickedFeeling ?: contract.usualFeeling.takeIf { it.isNotEmpty() },
                                onSelect = { feeling ->
                                    pickedFeeling = feeling
                                    onSetUsualFeeling(feeling)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "no streaks, no badges. a broken one makes people quit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }

                1 -> {
                    Text("one switch does it.", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "screen access hides the feed. screen time only adds charts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    PermissionCard(
                        title = "screen access",
                        body = "sees when a reels or shorts screen is open, then covers it. never reads what you type.",
                        hint = "android settings -> accessibility -> installed apps -> openeye -> use openeye's service",
                        granted = permissions.accessibility,
                        action = "allow",
                        onClick = onEnableAccessibility,
                    )
                    Spacer(Modifier.height(10.dp))
                    PermissionCard(
                        title = "screen time (optional)",
                        body = "adds app usage to the numbers page.",
                        granted = permissions.usageAccess,
                        action = "add",
                        onClick = onEnableUsage,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (permissions.accessibility) "it's on. counts start the next time you open a feed."
                        else "no internet permission. that's the build, not a setting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }

                else -> {
                    Text("you're set.", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "instagram, youtube and facebook are already shielded. add tiktok or snapchat any time from shields.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(26.dp))
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("before you go")
                            Spacer(Modifier.height(12.dp))
                            if (contract.stopRuleMinutes > 0) {
                                StatusLine("your number: ${contract.stopRuleMinutes} min a day", true)
                                Spacer(Modifier.height(10.dp))
                            }
                            if (contract.usualFeeling.isNotEmpty()) {
                                StatusLine("you said a scroll leaves you ${contract.usualFeeling}", true)
                                Spacer(Modifier.height(10.dp))
                            }
                            StatusLine("screen access", permissions.accessibility)
                            Spacer(Modifier.height(10.dp))
                            StatusLine("screen time", permissions.usageAccess)
                            if (!permissions.accessibility) {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "nothing's covered until screen access is on. counts start then.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            when (step) {
                0 -> PrimaryButton(text = "continue", onClick = { step = 1 })
                1 -> PrimaryButton(text = "continue", onClick = { step = 2 })
                2 -> {
                    PrimaryButton(text = "continue", onClick = { step = 3 })
                    Spacer(Modifier.height(8.dp))
                    GhostButton(text = "i'll decide later", onClick = { step = 3 })
                }
                else -> PrimaryButton(text = "open openeye", onClick = onFinish)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Wrapping chip row. Hand-rolled so onboarding never needs a keyboard. */
@Composable
private fun ChipGrid(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val active = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (active) Accent else SurfaceOne, RoundedCornerShape(100.dp))
                            .border(1.dp, if (active) Accent else Hairline, RoundedCornerShape(100.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            option,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) Ink else TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    action: String,
    hint: String? = null,
    onClick: () -> Unit,
) {
    OMCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !granted, onClick = onClick),
        padding = PaddingValues(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                Pill(
                    text = if (granted) "on" else action,
                    color = if (granted) Accent else AccentDim,
                    filled = granted,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (hint != null && !granted) {
                Spacer(Modifier.height(4.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (granted) "✓" else "—",
            color = if (granted) Accent else TextTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (granted) TextPrimary else TextSecondary, modifier = Modifier.weight(1f))
    }
}
