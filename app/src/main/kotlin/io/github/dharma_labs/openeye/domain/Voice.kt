package io.github.dharma_labs.openeye.domain

/**
 * Every user-facing sentence in Openeye lives here.
 *
 * Two reasons it is a pool and not a constant: repeated identical warnings get visually avoided
 * rather than read (habituation is the failure mode of almost every screen-time interrupt), and a
 * single fixed line turns the app into wallpaper. [pick] walks the pool with a stored cursor so
 * the same line can never appear twice in a row.
 *
 * Voice rules, learned the hard way by a lot of wellbeing apps:
 * - name the manipulation, never the user's weakness. the feed is the villain, not you.
 * - lowercase, at most 12 words a line, at most one question per screen.
 * - always leave an exit visible. a nudge with no exit is a cage and gets uninstalled.
 * - no red, no ALL CAPS guilt, no exclamation piles, no forced slang.
 * - banned: addict, detox, junk, discipline, failure, "you'll thank yourself", "put the phone down".
 */
object Voice {

    /** Shown when a surface is blocked but the person can still choose to open it. */
    val blockGentle = listOf(
        "reels is paused. not because you're weak. because it's built to be endless.",
        "this feed has no bottom. you were never going to find it.",
        "the next one might be great. that's the trick. it's always might.",
        "nothing's broken here. your brain is doing exactly what it was trained to do.",
        "autoplay never asks permission. you're allowed to close it mid-video.",
        "you're here because a machine decided you would be. want to disagree?",
        "this is the part where you feel nothing and keep swiping. or not.",
        "past-you set this at 11pm on a tuesday. they had a plot. hi, go to bed.",
        "want it for 5 minutes? fine. set it now, while you're still deciding.",
        "you don't owe this feed your evening. it doesn't even like you.",
        "feeling flat? that's wanting without liking. very on brand for short video.",
        "locked for a second. not to punish you. to give you a moment to choose.",
    )

    /** Strict mode: firm, boring, zero moralising. Being annoying on purpose is the feature. */
    val blockStrict = listOf(
        "not right now. you set this when you were calm. past-you gets a vote.",
        "this one's locked till your window opens. i'll be boring about it.",
        "no reels in this mode. deal's a deal.",
        "locked. you can still do literally anything else.",
        "the feed's off. that was the whole point of installing me.",
        "nothing to negotiate here. that's the useful part.",
    )

    val blockFocus = listOf(
        "focus is running. this opens when the timer ends.",
        "you said 25 minutes of the hard thing. the feed can wait that long.",
        "the feed is on the other side of this timer. keep going.",
        "attention's already paid for the next bit. don't hand it over for free.",
    )

    val blockSchedule = listOf(
        "quiet hours. the feed wakes up when you do.",
        "this is the hour you said you wanted back.",
        "locked overnight on purpose. 3am reels never fixed anything.",
    )

    val blockLimit = listOf(
        "that's your number. you picked it, not me.",
        "budget's spent for today. it resets at midnight, not before.",
        "you said enough for today. today isn't over yet.",
    )

    /**
     * "How much have you actually watched" reminders. %s is the formatted duration; one %s only.
     * Translating the number is the whole job. Awareness alone doesn't change behaviour, so the
     * line has to land as a specific lost thing, or as a question the person can answer.
     */
    val reminders = listOf(
        "%s in feeds today. that's a whole film you never agreed to watch.",
        "%s today. honestly: worth it, meh, or worse than before?",
        "%s of swiping. you could've learned pasta. or slept.",
        "%s today. roughly 20 clips you'd struggle to describe right now.",
        "%s. and there were things you said you'd do today.",
        "%s in feeds. the app's job is to make that feel like nothing. it isn't.",
        "%s today. your thumb has been working. you haven't been.",
        "%s. not judging. you chose a number, and here we are.",
    )

    /** Shown when today is under the person's own rule. Gain-framed, not congratulatory noise. */
    val underRule = listOf(
        "%s so far. under your number. that's the whole game.",
        "%s today. your limit is %s. still yours to spend.",
        "under what you picked. nothing to celebrate, nothing to fix.",
        "%s of %s. the feed isn't running your evening tonight.",
    )

    /**
     * Lapse copy. Self-compassion after a slip increases the chance of the next one sticking;
     * shame predictably makes it worse. So: normalise, get curious, offer a small re-entry.
     * Never a lost streak, because there is no streak.
     */
    val lapse = listOf(
        "today went over. one day, not a character flaw.",
        "yeah, today was a lot. that's information, not a verdict.",
        "over your number. happens. the interesting bit is what set it off.",
        "today got away from you. it's still got this evening left in it.",
    )

    val lapseQuestion = "what set it off?"

    val lapseLadder = listOf(
        "bored", "tired", "stressed", "avoiding something", "just opened it", "nothing, really",
    )

    /**
     * Affect labels. Naming the feeling is the intervention, so the words have to be plain.
     * One word each, deliberately: these render as quarter-width chips on the block sheet, and a
     * two-word label gets clipped to "actually" and looks like a truncated string.
     */
    val feelWords = listOf("wired", "flat", "numb", "meh", "calmer", "anxious", "good")

    val feelPrompt = "how'd that leave you?"

    val feelPromptLong = "one word. after a long scroll, most people land on flat or numb. no wrong answer."

    /** Onboarding. Autonomy first: they name the number and the trigger, we just hold it. */
    val onboardingAsk = listOf(
        "what's a number you'd actually believe you set? type it in.",
        "how much of this do you genuinely want? not zero. real.",
        "when does the scroll always get you? name the moment.",
    )

    val planTriggerHint = "when i open instagram in bed after 11"

    val planActionHint = "phone goes face down, timer on for 10"

    /** Used where an app would normally show a peer statistic it never verified. */
    val noPeerStats =
        "apps love telling you 90% of people scroll more than you. we didn't verify that, so we won't say it."

    /** Where a streak or badge would normally go. */
    val noStreaks =
        "no streaks, no badges, no confetti. a broken streak makes people quit; and you're here to use your phone less, not to play a game with us."

    /**
     * The in-feed popup, while a Reels/Shorts surface is open. Two slots: today's feed time, then
     * clips watched. Shorter than [reminders] on purpose - it floats over someone's thumb and the
     * app they are still holding, so a paragraph would just be read as lag.
     */
    val nudges = listOf(
        "%s today. %s clips.",
        "%s in feeds. %s clips, and no bottom.",
        "checking in: %s today, %s clips.",
        "%s so far. %s clips you'd struggle to describe.",
        "you're at %s today across %s clips.",
        "%s gone. %s clips in.",
        "%s today. %s clips. still here.",
        "heads up: %s today, %s clips.",
    )

    /** When today is already past the number they picked. Two slots: feed time, then their rule. */
    val nudgesOver = listOf(
        "%s today. over your %s.",
        "%s today. your number was %s.",
        "%s and counting. you said %s.",
    )

    /** Where the whole-app screen time line goes, when long-video counting is on. */
    val nudgeScreenTime = "%s in the app today."

    fun pick(pool: List<String>, cursor: Int): String {
        if (pool.isEmpty()) return ""
        val index = ((cursor % pool.size) + pool.size) % pool.size
        return pool[index]
    }

    /** %s substitution that survives a pool entry accidentally carrying the wrong count. */
    fun fill(template: String, vararg values: String): String {
        var result = template
        for (value in values) {
            val at = result.indexOf("%s")
            if (at < 0) break
            result = result.substring(0, at) + value + result.substring(at + 2)
        }
        return result
    }
}
