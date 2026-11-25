package io.github.dharma_labs.openeye.domain

import kotlin.math.roundToInt

/**
 * Turns feed minutes into something a person can actually feel the weight of.
 *
 * Awareness-only dashboards measurably fail to change use, so raw "3h 12m" is never the last
 * line on a card. Every equivalent here is arithmetic on real measured time, and the yearly one
 * is always introduced with "at this rate" because it is an extrapolation, not a fact.
 */
object TimeTranslation {

    private const val FILM_MINUTES = 105L
    private const val EPISODE_MINUTES = 25L
    private const val WALK_MINUTES = 40L
    private const val WORKDAY_MINUTES = 480L

    data class Equivalent(val headline: String, val detail: String)

    fun minutesOf(millis: Long): Long = millis / 60_000L

    /** One concrete forgone thing, chosen by magnitude so small days don't sound dramatic. */
    fun oneThing(millis: Long): Equivalent {
        val minutes = minutesOf(millis)
        return when {
            minutes < 1 -> Equivalent("under a minute", "you looked, you left. that counts.")
            minutes < 5 -> Equivalent("${minutes}m", "a kettle boils faster than this.")
            minutes < EPISODE_MINUTES -> Equivalent(
                "${minutes}m",
                "shorter than one episode of a good show.",
            )
            minutes < WALK_MINUTES -> Equivalent(
                "${minutes}m",
                "one episode plus a bit. you could've had both.",
            )
            minutes < FILM_MINUTES -> Equivalent(
                "${minutes}m",
                "a 40 minute walk, with ${minutes - WALK_MINUTES}m left over.",
            )
            minutes < 180 -> Equivalent(
                "${minutes}m",
                "that's the film you keep saying you'll watch tonight.",
            )
            minutes < WORKDAY_MINUTES -> Equivalent(
                "${minutes}m",
                "two films back to back, with snacks.",
            )
            else -> Equivalent(
                "${minutes}m",
                "longer than a workday. you were clocked in to a feed.",
            )
        }
    }

    /**
     * Today's number projected across a year. Framed as a rate, never as a verdict, and refuses
     * to quote a scary figure for a one-minute day.
     */
    fun atThisRate(millis: Long): String? {
        val days = millis / 86_400_000.0 * 365.0
        if (days < 3) return null
        val rounded = days.roundToInt()
        return if (rounded >= 30) {
            "at this rate that's about $rounded days a year. a whole month of being elsewhere."
        } else {
            "at this rate that's about $rounded days a year."
        }
    }

    /** Clips passed, expressed as the pace rather than a count of "videos watched". */
    fun clipPace(millis: Long, exposures: Int): String? {
        if (exposures <= 0 || millis <= 0) return null
        val secondsEach = (millis / 1000.0 / exposures).roundToInt()
        return "$exposures clips, about ${secondsEach}s each. you kept going."
    }

    fun hoursText(millis: Long): String {
        val totalMinutes = minutesOf(millis)
        return when {
            totalMinutes < 1 -> "${(millis / 1000L)}s"
            totalMinutes < 60 -> "${totalMinutes}m"
            else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        }
    }
}
