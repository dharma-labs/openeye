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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.openmind.data.AttentionContract
import com.example.openmind.data.ImplementationPlan
import com.example.openmind.domain.TimeTranslation
import com.example.openmind.domain.Voice
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceTwo
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        item {
            ScreenHeader("plans", "decide now, use it later.", onBack = onBack)
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    SectionLabel("your daily number")
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ruleChoices.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { minutes ->
                                    val active = contract.stopRuleMinutes == minutes
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (active) Accent else SurfaceTwo, RoundedCornerShape(14.dp))
                                            .border(1.dp, if (active) Accent else Hairline, RoundedCornerShape(14.dp))
                                            .clickable { onSetRule(minutes) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$minutes",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (active) Ink else TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("minutes of reels + shorts a day", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Spacer(Modifier.height(14.dp))
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
        item { Spacer(Modifier.height(16.dp)) }
        item { SectionLabel("when it happens, then what") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Text(
                        "specific beats motivated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    PlanField(
                        value = triggerDraft,
                        onValueChange = { triggerDraft = it },
                        placeholder = Voice.planTriggerHint,
                        label = "when",
                    )
                    Spacer(Modifier.height(10.dp))
                    PlanField(
                        value = actionDraft,
                        onValueChange = { actionDraft = it },
                        placeholder = Voice.planActionHint,
                        label = "then",
                    )
                    Spacer(Modifier.height(14.dp))
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
        item { Spacer(Modifier.height(18.dp)) }
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
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(if (plan.enabled) Accent else Hairline, CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (plan.enabled) "armed" else "off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (plan.enabled) AccentDim else TextTertiary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "delete",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextTertiary,
                                    modifier = Modifier
                                        .clickable { onRemovePlan(plan.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("when", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Spacer(Modifier.height(2.dp))
                            Text(plan.trigger, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("then", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Spacer(Modifier.height(2.dp))
                            Text(plan.action, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(SurfaceTwo, RoundedCornerShape(100.dp))
                                    .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                                    .clickable { onTogglePlan(plan.id, !plan.enabled) }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    if (plan.enabled) "park it" else "arm it",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(22.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column {
                    SectionLabel("what we don't do")
                    Spacer(Modifier.height(10.dp))
                    Text(Voice.noStreaks, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun PlanField(value: String, onValueChange: (String) -> Unit, placeholder: String, label: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextTertiary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceOne,
                unfocusedContainerColor = SurfaceOne,
                cursorColor = Accent,
                focusedIndicatorColor = Accent,
                unfocusedIndicatorColor = Hairline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceOne, RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp)),
        )
    }
}
