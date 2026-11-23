package com.example.openmind.domain

import com.example.openmind.data.AttentionContract
import com.example.openmind.data.FeelingCheck
import com.example.openmind.data.ImplementationPlan
import com.example.openmind.data.Settings

/**
 * Decides what Openeye says, given what actually happened.
 *
 * Three ideas do the work here:
 * 1. Precommitment. A rule written while calm is quoted back by name when the person is already
 *    mid-craving, because in-the-moment willpower is the weakest possible intervention point.
 * 2. Anti-habituation. Block copy rotates on a stored cursor, so the interrupt never becomes
 *    wallpaper the eye learns to skip.
 * 3. Self-compassion at the slip. Over-rule days produce curiosity and a small re-entry, never a
 *    lost-streak message, because shame reliably predicts the next lapse.
 */
object Coach {

    private val heavyFeelings = setOf("flat", "numb", "meh", "anxious", "wired")

    enum class DayState { NO_DATA, NO_RULE, UNDER_RULE, OVER_RULE }

    data class Message(
        val primary: String,
        val secondary: String,
        val cta: String?,
        val target: String?,
    )

    fun dayState(todayMillis: Long, contract: AttentionContract): DayState = when {
        todayMillis <= 0L -> DayState.NO_DATA
        contract.stopRuleMinutes <= 0 -> DayState.NO_RULE
        todayMillis >= contract.stopRuleMinutes * 60_000L -> DayState.OVER_RULE
        else -> DayState.UNDER_RULE
    }

    /** The number a person should see first on Today, always translated into something concrete. */
    fun todayMessage(
        todayMillis: Long,
        exposures: Int,
        settings: Settings,
        cursor: Int,
    ): Message {
        val contract = settings.contract
        val time = TimeTranslation.hoursText(todayMillis)
        val limit = "${contract.stopRuleMinutes}m"
        return when (dayState(todayMillis, contract)) {
            DayState.NO_DATA -> Message(
                primary = "nothing tracked yet.",
                secondary = "open a feed and it gets counted. no advice until there's data.",
                cta = null,
                target = null,
            )
            DayState.NO_RULE -> Message(
                primary = "$time in feeds today.",
                secondary = Voice.fill(Voice.pick(Voice.reminders, cursor), time),
                cta = "set your number",
                target = "plan",
            )
            DayState.UNDER_RULE -> Message(
                primary = "$time so far.",
                secondary = Voice.fill(Voice.pick(Voice.underRule, cursor), time, limit)
                    .ifBlank { TimeTranslation.oneThing(todayMillis).detail },
                cta = "put an hour back",
                target = "focus",
            )
            DayState.OVER_RULE -> {
                val normalise = Voice.pick(Voice.lapse, cursor)
                val pace = TimeTranslation.clipPace(todayMillis, exposures)
                Message(
                    primary = "$time today. over your $limit.",
                    secondary = if (pace == null) normalise else "$normalise $pace",
                    cta = "take 10 minutes back",
                    target = "reentry",
                )
            }
        }
    }

    fun isLapse(todayMillis: Long, contract: AttentionContract): Boolean =
        dayState(todayMillis, contract) == DayState.OVER_RULE

    /**
     * The headline on the block screen. Strict modes get firm, boring copy; gentle mode gets a
     * rotating reflective line. Past their own number, the rule is quoted back rather than a
     * lecture, because the number is theirs.
     */
    fun blockLine(
        reason: String,
        strict: Boolean,
        cursor: Int,
        todayMillis: Long,
        contract: AttentionContract,
    ): String {
        val overRule = contract.stopRuleMinutes > 0 &&
            todayMillis >= contract.stopRuleMinutes * 60_000L
        if (overRule && !strict) {
            return "you said ${contract.stopRuleMinutes} min today. it's ${TimeTranslation.hoursText(todayMillis)}. still going?"
        }
        val pool = when {
            reason == "FOCUS" -> Voice.blockFocus
            reason == "LIMIT" -> Voice.blockLimit
            reason == "SCHEDULE" -> Voice.blockSchedule
            strict -> Voice.blockStrict
            else -> Voice.blockGentle
        }
        return Voice.pick(pool, cursor)
    }

    fun activePlan(settings: Settings): ImplementationPlan? = settings.plans.firstOrNull { it.enabled }

    /**
     * Wanting versus liking, shown back from the person's own check-ins. Needs three entries
     * before it speaks, and never diagnoses.
     */
    fun feelingReveal(feelings: List<FeelingCheck>): String? {
        val recent = feelings.takeLast(30)
        if (recent.size < 3) return null
        val lower = recent.map { it.feeling.lowercase() }
        val heavy = lower.count { it in heavyFeelings }
        if (heavy == 0) return null
        val named = lower.filter { it in heavyFeelings }.distinct().take(2).joinToString(" or ")
        return "you checked in ${recent.size} times. $heavy landed on $named. that's wanting, not liking."
    }
}
