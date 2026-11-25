package com.example.openmind.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import com.example.openmind.ui.components.EyeIcon
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.IconTile
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
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

private val stopRuleChoices = listOf(15, 30, 45, 60, 90, 120)

/**
 * Four steps, and a person can leave at any point.
 *
 * Onboarding is where a wellbeing app either earns trust or loses it, so the order is the order of
 * effort: understand it, allow one switch, answer two calm questions, see the state you are in. The
 * progress dots matter more than they look - a visible last step is what makes people finish rather
 * than background the app.
 */
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
                .padding(horizontal = EyeSpace.huge, vertical = EyeSpace.xxxl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyeMark(modifier = Modifier.size(20.dp), color = Accent)
                Spacer(Modifier.width(EyeSpace.sm))
                Text("openeye", style = MaterialTheme.typography.labelMedium, color = Accent, modifier = Modifier.weight(1f))
                Text(
                    "skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier
                        .heightIn(min = MinTouch)
                        .eyePress(onClick = onFinish)
                        .padding(horizontal = EyeSpace.sm),
                )
            }
            Spacer(Modifier.height(EyeSpace.lg))
            StepDots(step)
            Spacer(Modifier.height(EyeSpace.xxl))

            when (step) {
                0 -> Welcome()
                1 -> Permissions(
                    permissions = permissions,
                    onEnableAccessibility = onEnableAccessibility,
                    onEnableUsage = onEnableUsage,
                    onEnableNotifications = onEnableNotifications,
                )
                2 -> Questions(
                    contract = contract,
                    pickedMinutes = pickedMinutes,
                    pickedFeeling = pickedFeeling,
                    onSetStopRule = onSetStopRule,
                    onSetUsualFeeling = onSetUsualFeeling,
                )
                else -> Summary(permissions, contract)
            }

            Spacer(Modifier.height(EyeSpace.xxxl))
            when (step) {
                0, 1 -> PrimaryButton(text = "continue", onClick = { step += 1 })
                2 -> {
                    PrimaryButton(text = "continue", onClick = { step = 3 })
                    Spacer(Modifier.height(EyeSpace.sm))
                    GhostButton(text = "i'll decide later", onClick = { step = 3 })
                }
                else -> PrimaryButton(text = "open openeye", onClick = onFinish, icon = "forward")
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.xs)) {
        repeat(4) { index ->
            val done = index <= step
            val color by animateColorAsState(if (done) Accent else SurfaceThree, label = "dot$index")
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(if (index == step) 26.dp else 12.dp)
                    .background(color, RoundedCornerShape(100.dp)),
            )
        }
    }
}

@Composable
private fun Welcome() {
    Column {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(SurfaceOne, CircleShape)
                .border(1.dp, Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            EyeMark(modifier = Modifier.size(40.dp), color = Accent)
        }
        Spacer(Modifier.height(EyeSpace.xxl))
        Text(
            "the feed isn't\naccidentally endless.",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(EyeSpace.lg))
        Text(
            "built like a slot machine. most swipes pay nothing, so you keep pulling.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(EyeSpace.xxxl))
        listOf(
            "shield" to "covers reels, shorts, explore. not the whole app.",
            "chart" to "counts your time in real units.",
            "lock" to "nothing leaves your phone. no account, no internet.",
        ).forEach { (icon, line) ->
            Row(
                modifier = Modifier.padding(bottom = EyeSpace.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(type = icon, tint = Accent, size = 32.dp)
                Spacer(Modifier.width(EyeSpace.md))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun Permissions(
    permissions: PermissionState,
    onEnableAccessibility: () -> Unit,
    onEnableUsage: () -> Unit,
    onEnableNotifications: () -> Unit,
) {
    Column {
        Text("one switch does it.", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(EyeSpace.sm))
        Text(
            "screen access hides the feed. the rest only adds charts.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(EyeSpace.xxl))
        PermissionCard(
            icon = "shield",
            title = "screen access",
            body = "sees when a reels or shorts screen is open, then covers it. never reads what you type.",
            hint = "android settings -> accessibility -> installed apps -> openeye -> use openeye's service",
            granted = permissions.accessibility,
            action = "allow",
            onClick = onEnableAccessibility,
        )
        Spacer(Modifier.height(EyeSpace.md))
        PermissionCard(
            icon = "chart",
            title = "screen time",
            body = "adds whole-app usage to the numbers page.",
            granted = permissions.usageAccess,
            action = "add",
            optional = true,
            onClick = onEnableUsage,
        )
        Spacer(Modifier.height(EyeSpace.md))
        PermissionCard(
            icon = "bell",
            title = "notifications",
            body = "for focus and quiet hours only.",
            granted = permissions.notifications,
            action = "add",
            optional = true,
            onClick = onEnableNotifications,
        )
        Spacer(Modifier.height(EyeSpace.xl))
        Text(
            text = if (permissions.accessibility) "it's on. counts start the next time you open a feed."
            else "no internet permission. that's the build, not a setting.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun Questions(
    contract: AttentionContract,
    pickedMinutes: Int?,
    pickedFeeling: String?,
    onSetStopRule: (Int) -> Unit,
    onSetUsualFeeling: (String) -> Unit,
) {
    Column {
        Text(
            "two questions, while you're\ncalm about it.",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(EyeSpace.xs))
        Text(
            "answer now. i'll quote them back later.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(EyeSpace.xxl))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
            Column {
                SectionLabel("what's a number you'd believe?")
                Spacer(Modifier.height(EyeSpace.sm))
                Text(
                    "not zero. zero gets broken and ignored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                Spacer(Modifier.height(EyeSpace.md))
                ChipGrid(
                    options = stopRuleChoices.map { "$it min" },
                    selected = pickedMinutes?.let { "${it} min" }
                        ?: contract.stopRuleMinutes.takeIf { rule -> rule > 0 }?.let { "$it min" },
                    onSelect = { label ->
                        val minutes = label.removeSuffix(" min").toIntOrNull() ?: return@ChipGrid
                        onSetStopRule(minutes)
                    },
                )
            }
        }
        Spacer(Modifier.height(EyeSpace.md))
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
            Column {
                SectionLabel("how does a long scroll usually leave you?")
                Spacer(Modifier.height(EyeSpace.sm))
                Text(
                    Voice.feelPromptLong,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                Spacer(Modifier.height(EyeSpace.md))
                ChipGrid(
                    options = Voice.feelWords,
                    selected = pickedFeeling ?: contract.usualFeeling.takeIf { it.isNotEmpty() },
                    onSelect = { feeling -> onSetUsualFeeling(feeling) },
                )
            }
        }
        Spacer(Modifier.height(EyeSpace.lg))
        Text(
            "no streaks, no badges. a broken one makes people quit.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun Summary(permissions: PermissionState, contract: AttentionContract) {
    Column {
        Text("you're set.", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
        Spacer(Modifier.height(EyeSpace.sm))
        Text(
            "instagram, youtube and facebook are already shielded. add tiktok or snapchat any time from shields.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(EyeSpace.xxl))
        OMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(EyeSpace.xl),
            glow = if (permissions.accessibility) Accent else null,
        ) {
            Column {
                SectionLabel("before you go")
                Spacer(Modifier.height(EyeSpace.md))
                if (contract.stopRuleMinutes > 0) {
                    StatusLine("your number: ${contract.stopRuleMinutes} min a day", true)
                    Spacer(Modifier.height(EyeSpace.sm))
                }
                if (contract.usualFeeling.isNotEmpty()) {
                    StatusLine("you said a scroll leaves you ${contract.usualFeeling}", true)
                    Spacer(Modifier.height(EyeSpace.sm))
                }
                StatusLine("screen access", permissions.accessibility)
                Spacer(Modifier.height(EyeSpace.sm))
                StatusLine("screen time", permissions.usageAccess)
                if (!permissions.accessibility) {
                    Spacer(Modifier.height(EyeSpace.md))
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

/** Wrapping chip row. Hand-rolled so onboarding never needs a keyboard. */
@Composable
private fun ChipGrid(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                row.forEach { option ->
                    val active = option == selected
                    val shape = RoundedCornerShape(EyeShape.chip)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MinTouch)
                            .background(if (active) Accent else SurfaceOne, shape)
                            .border(1.dp, if (active) Accent else Hairline, shape)
                            .eyePress(onClick = { onSelect(option) })
                            .padding(horizontal = EyeSpace.sm),
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
    icon: String,
    title: String,
    body: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit,
    optional: Boolean = false,
    hint: String? = null,
) {
    OMCard(
        modifier = Modifier
            .fillMaxWidth()
            .eyePress(enabled = !granted, onClick = onClick),
        padding = PaddingValues(EyeSpace.lg),
        glow = if (granted) Accent else null,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(type = icon, tint = if (granted) Accent else TextSecondary, size = 36.dp)
                Spacer(Modifier.width(EyeSpace.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (optional) {
                        Spacer(Modifier.height(2.dp))
                        Text("optional", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
                Pill(
                    text = if (granted) "on" else action,
                    color = if (granted) Accent else AccentDim,
                    filled = granted,
                )
            }
            Spacer(Modifier.height(EyeSpace.sm))
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (hint != null && !granted) {
                Spacer(Modifier.height(EyeSpace.xs))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EyeIcon(
            type = if (granted) "check" else "minus",
            tint = if (granted) Accent else TextTertiary,
            size = 16.dp,
        )
        Spacer(Modifier.width(EyeSpace.md))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (granted) TextPrimary else TextSecondary,
        )
    }
}
