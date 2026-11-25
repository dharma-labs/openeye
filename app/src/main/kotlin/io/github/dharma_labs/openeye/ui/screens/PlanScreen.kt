package io.github.dharma_labs.openeye.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dharma_labs.openeye.data.AttentionContract
import io.github.dharma_labs.openeye.data.ImplementationPlan
import io.github.dharma_labs.openeye.domain.TimeTranslation
import io.github.dharma_labs.openeye.domain.Voice
import io.github.dharma_labs.openeye.ui.components.ActionCard
import io.github.dharma_labs.openeye.ui.components.EyeIcon
import io.github.dharma_labs.openeye.ui.components.OMCard
import io.github.dharma_labs.openeye.ui.components.PrimaryButton
import io.github.dharma_labs.openeye.ui.components.ScreenHeader
import io.github.dharma_labs.openeye.ui.components.SectionLabel
import io.github.dharma_labs.openeye.ui.components.eyePress
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

private val ruleChoices = listOf(10, 15, 20, 30, 45, 60, 90, 120)

/**
 * The plans screen. Implementation intentions are the strongest thing in this app: a specific
 * trigger paired with a specific action beats monitoring and beats willpower, so this screen asks
 * for both halves in the person's own words and then quotes them back at the moment it matters.
 */
@Composable
fun PlanScreen(
    contract: AttentionContract,
    plans: List<ImplementationPlan>,
    todayMillis: Long,
    onSetRule: (Int) -> Unit,
    onAddPlan: (String, String) -> Unit,
    onRemovePlan: (String) -> Unit,
    onTogglePlan: (String, Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var triggerDraft by rememberSaveable { mutableStateOf("") }
    var actionDraft by rememberSaveable { mutableStateOf("") }
    val todayMinutes = TimeTranslation.minutesOf(todayMillis)
    val overRule = contract.stopRuleMinutes > 0 && todayMinutes >= contract.stopRuleMinutes

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = EyeSpace.screen,
            top = EyeSpace.md,
            end = EyeSpace.screen,
            bottom = EyeSpace.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        item { ScreenHeader("plans", "decide now, use it later.", onBack = onBack) }

        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
                Column {
                    SectionLabel("your daily number")
                    Spacer(Modifier.height(EyeSpace.md))
                    Column(verticalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                        ruleChoices.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                                row.forEach { minutes ->
                                    RuleTile(
                                        minutes = minutes,
                                        active = contract.stopRuleMinutes == minutes,
                                        onClick = { onSetRule(minutes) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(EyeSpace.xs))
                    Text(
                        "minutes of reels + shorts a day",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    if (contract.stopRuleMinutes > 0) {
                        val signed = if (contract.signedEpochMs > 0) {
                            "yours. today is ${todayMinutes}m."
                        } else {
                            "set by you."
                        }
                        Text(
                            text = if (overRule) "over it at ${todayMinutes}m. a day, not a verdict." else signed,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overRule) AccentDim else TextSecondary,
                        )
                    } else {
                        Text(
                            "no number yet, so nothing is over or under.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                }
            }
        }

        item { SectionLabel("when it happens, then what") }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
                Column {
                    Text(
                        "specific beats motivated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    PlanField(
                        value = triggerDraft,
                        onValueChange = { triggerDraft = it },
                        placeholder = Voice.planTriggerHint,
                        label = "when",
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    PlanField(
                        value = actionDraft,
                        onValueChange = { actionDraft = it },
                        placeholder = Voice.planActionHint,
                        label = "then",
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    PrimaryButton(
                        text = "add plan",
                        enabled = triggerDraft.isNotBlank() && actionDraft.isNotBlank(),
                        onClick = {
                            onAddPlan(triggerDraft, actionDraft)
                            triggerDraft = ""
                            actionDraft = ""
                        },
                    )
                }
            }
        }

        if (plans.isEmpty()) {
            item {
                Text(
                    "nothing yet. one plan beats five.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        } else {
            plans.forEach { plan ->
                item {
                    OMCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(EyeSpace.lg),
                        raised = plan.enabled,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (plan.enabled) Accent else Hairline, CircleShape),
                                )
                                Spacer(Modifier.width(EyeSpace.sm))
                                Text(
                                    if (plan.enabled) "armed" else "parked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (plan.enabled) AccentDim else TextTertiary,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(MinTouch)
                                        .clip(RoundedCornerShape(EyeShape.field))
                                        .eyePress(onClick = { onRemovePlan(plan.id) }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EyeIcon("close", tint = TextTertiary, size = 17.dp)
                                }
                            }
                            Spacer(Modifier.height(EyeSpace.md))
                            Text("when", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Spacer(Modifier.height(EyeSpace.xxs))
                            Text(plan.trigger, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(EyeSpace.sm))
                            Text("then", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Spacer(Modifier.height(EyeSpace.xxs))
                            Text(plan.action, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.height(EyeSpace.md))
                            ActionCard(
                                label = if (plan.enabled) "park it" else "arm it",
                                onClick = { onTogglePlan(plan.id, !plan.enabled) },
                                emphasized = !plan.enabled,
                                icon = if (plan.enabled) "minus" else "check",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                Column {
                    SectionLabel("what we don't do")
                    Spacer(Modifier.height(EyeSpace.sm))
                    Text(Voice.noStreaks, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RuleTile(minutes: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EyeShape.control)
    Box(
        modifier = modifier
            .heightIn(min = MinTouch)
            .background(if (active) Accent else SurfaceTwo, shape)
            .border(1.dp, if (active) Accent else Hairline, shape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$minutes",
            style = MaterialTheme.typography.titleMedium,
            color = if (active) Ink else TextSecondary,
        )
    }
}

@Composable
private fun PlanField(value: String, onValueChange: (String) -> Unit, placeholder: String, label: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(EyeSpace.xs))
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextTertiary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = MaterialTheme.typography.bodyMedium,
            // The field paints its own shape; a second background/border around it leaves the
            // default 28dp outline showing through the corners.
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
}
