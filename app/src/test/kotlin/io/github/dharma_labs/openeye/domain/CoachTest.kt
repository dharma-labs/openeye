package io.github.dharma_labs.openeye.domain

import io.github.dharma_labs.openeye.data.AttentionContract
import io.github.dharma_labs.openeye.data.FeelingCheck
import io.github.dharma_labs.openeye.data.ImplementationPlan
import io.github.dharma_labs.openeye.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachTest {

    private val minute = 60_000L

    private fun settingsWithRule(minutes: Int) =
        Settings(contract = AttentionContract(stopRuleMinutes = minutes, signedEpochMs = 1L))

    @Test
    fun noDataBeatsNoRuleWhenNothingTracked() {
        assertEquals(Coach.DayState.NO_DATA, Coach.dayState(0L, AttentionContract(stopRuleMinutes = 30)))
        assertEquals(Coach.DayState.NO_DATA, Coach.dayState(0L, AttentionContract()))
    }

    @Test
    fun noNumberMeansNoVerdictAvailable() {
        assertEquals(Coach.DayState.NO_RULE, Coach.dayState(45 * minute, AttentionContract()))
    }

    @Test
    fun crossingTheOwnRuleFlipsToOver() {
        val contract = AttentionContract(stopRuleMinutes = 30)
        assertEquals(Coach.DayState.UNDER_RULE, Coach.dayState(29 * minute, contract))
        assertEquals(Coach.DayState.OVER_RULE, Coach.dayState(30 * minute, contract))
        assertEquals(Coach.DayState.OVER_RULE, Coach.dayState(90 * minute, contract))
        assertTrue(Coach.isLapse(31 * minute, contract))
        assertFalse(Coach.isLapse(31 * minute, AttentionContract()))
    }

    @Test
    fun todayMessageNeverLeavesTheNumberUntranslated() {
        val message = Coach.todayMessage(150 * minute, 90, settingsWithRule(30), cursor = 0)
        assertEquals(Coach.DayState.OVER_RULE, Coach.dayState(150 * minute, settingsWithRule(30).contract))
        assertTrue(message.primary, message.primary.contains("2h 30m"))
        assertTrue(message.secondary, message.secondary.contains("clips"))
        assertEquals("reentry", message.target)
        assertTrue(message.cta!!.isNotEmpty())
    }

    @Test
    fun anEmptyDayAsksForDataInsteadOfGivingAdvice() {
        val message = Coach.todayMessage(0L, 0, Settings(), cursor = 3)
        assertNull(message.cta)
        assertTrue(message.primary.contains("nothing tracked"))
    }

    @Test
    fun anUndefinedDayInvitesThePersonToSetTheirOwnNumber() {
        val message = Coach.todayMessage(40 * minute, 20, Settings(), cursor = 1)
        assertEquals("plan", message.target)
        assertEquals("set your number", message.cta)
        assertTrue(message.secondary, message.secondary.contains("40m"))
    }

    @Test
    fun blockLineQuotesTheirOwnNumberBackInGentleMode() {
        val line = Coach.blockLine(
            reason = "SHIELD",
            strict = false,
            cursor = 0,
            todayMillis = 47 * minute,
            contract = AttentionContract(stopRuleMinutes = 20),
        )
        assertTrue(line, line.contains("you said 20 min today"))
        assertTrue(line, line.contains("47m"))
    }

    @Test
    fun blockLineStaysFirmAndRotatesInStrictMode() {
        val contract = AttentionContract(stopRuleMinutes = 20)
        val first = Coach.blockLine("SHIELD", true, 0, 47 * minute, contract)
        val second = Coach.blockLine("SHIELD", true, 1, 47 * minute, contract)
        assertTrue(first, Voice.blockStrict.contains(first))
        assertTrue(second, Voice.blockStrict.contains(second))
        assertTrue(first != second)
        assertTrue(
            Coach.blockLine("FOCUS", false, 0, 47 * minute, AttentionContract()) in Voice.blockFocus,
        )
    }

    @Test
    fun feelingRevealNeedsEvidenceBeforeItSpeaks() {
        assertNull(Coach.feelingReveal(emptyList()))
        val two = listOf(check("flat"), check("numb"))
        assertNull(Coach.feelingReveal(two))
        val three = two + check("flat")
        val reveal = requireNotNull(Coach.feelingReveal(three))
        assertTrue(reveal, reveal.contains("checked in 3"))
        assertTrue(reveal, reveal.contains("wanting, not liking"))
    }

    @Test
    fun allPositiveCheckInsProduceNoReveal() {
        val good = listOf(check("good"), check("calmer"), check("good"))
        assertNull(Coach.feelingReveal(good))
    }

    @Test
    fun onlyAnArmedPlanGetsSurfacedToTheBlockScreen() {
        val parked = plan("when i'm in bed", "phone face down", enabled = false)
        val armed = plan("after 11pm", "screen off", enabled = true)
        val settings = Settings(plans = listOf(parked, armed))
        assertEquals(armed, Coach.activePlan(settings))
        assertNull(Coach.activePlan(Settings(plans = listOf(parked))))
    }

    private fun check(feeling: String) = FeelingCheck(
        epochMs = System.currentTimeMillis(),
        epochDay = 1L,
        feeling = feeling,
        minutes = 20L,
    )

    private fun plan(trigger: String, action: String, enabled: Boolean) = ImplementationPlan(
        id = trigger,
        trigger = trigger,
        action = action,
        createdAtEpochMs = 1L,
        enabled = enabled,
    )
}
