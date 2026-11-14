package com.nightwise.timewise.data.friction

import kotlin.random.Random

enum class AwarenessKind(val label: String) {
    FACT("Facts"),
    QUOTE("Quotes"),
    GAIN("If you start today")
}

data class AwarenessItem(val kind: AwarenessKind, val text: String, val source: String)

/** Facts about phone use, quotes on time, and what cutting back gives you. Shown before extra time and in full on their own screen. */
object Awareness {

    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val DAYS_A_YEAR = 365

    val items: List<AwarenessItem> = listOf(
        AwarenessItem(
            AwarenessKind.FACT,
            "Having your phone on the desk, even face-down and silent, lowers how much you can hold in mind and focus on. Moving it to another room helps.",
            "Ward et al., Journal of the Association for Consumer Research, 2017"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "Limiting social media to about 30 minutes a day for three weeks led to significant drops in loneliness and depression.",
            "Hunt et al., University of Pennsylvania, 2018"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "People who read on a light-emitting screen before bed took longer to fall asleep, had less REM sleep and were less alert the next morning than people reading a printed book.",
            "Chang et al., PNAS, 2015"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "Adults need 7 or more hours of sleep a night. Regularly getting less is linked to weight gain, heart disease, depression and a weaker immune system.",
            "American Academy of Sleep Medicine & Sleep Research Society, 2015"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "Bending your head forward 60° to look at a phone puts around 27 kg (60 lb) of load on your neck.",
            "Hansraj, Surgical Technology International, 2014"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "After an interruption it takes about 23 minutes, on average, to get fully back to what you were doing.",
            "Gloria Mark, University of California, Irvine"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "You blink less while staring at a screen, which dries and tires your eyes. Every 20 minutes, look at something 20 feet (6 m) away for 20 seconds.",
            "American Academy of Ophthalmology"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "Pull-to-refresh and likes work like a slot machine: unpredictable rewards are what keep you checking.",
            "Tristan Harris, Center for Humane Technology"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "Infinite scroll removes every natural place to stop. Its inventor, Aza Raskin, has said he feels guilty about how it's used.",
            "Aza Raskin, BBC interview, 2018"
        ),
        AwarenessItem(
            AwarenessKind.FACT,
            "5 hours a day on your phone adds up to about 76 days a year: more than two months of your waking life.",
            "5 h × 365 days"
        ),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "It is not that we have a short time to live, but that we waste a lot of it.",
            "Seneca, On the Shortness of Life"
        ),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "How we spend our days is, of course, how we spend our lives.",
            "Annie Dillard, The Writing Life"
        ),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "The cost of a thing is the amount of what I will call life which is required to be exchanged for it.",
            "Henry David Thoreau, Walden"
        ),
        AwarenessItem(AwarenessKind.QUOTE, "Lost time is never found again.", "Benjamin Franklin"),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "Almost everything will work again if you unplug it for a few minutes, including you.",
            "Anne Lamott"
        ),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "The best minds of my generation are thinking about how to make people click ads.",
            "Jeff Hammerbacher"
        ),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "You will never find time for anything. If you want time, you must make it.",
            "Charles Buxton"
        ),
        AwarenessItem(AwarenessKind.QUOTE, "We are what we repeatedly do.", "Will Durant, on Aristotle"),
        AwarenessItem(
            AwarenessKind.QUOTE,
            "Discipline is choosing between what you want now and what you want most.",
            "Unknown"
        ),
        AwarenessItem(
            AwarenessKind.GAIN,
            "Cut just 1 hour a day and you get back 365 hours a year: about 15 full days, or 9 working weeks.",
            "1 h × 365 days"
        ),
        AwarenessItem(
            AwarenessKind.GAIN,
            "365 hours is enough to read around 50 books, at about 7 hours a book.",
            "365 h ÷ 7 h"
        ),
        AwarenessItem(
            AwarenessKind.GAIN,
            "An hour a day for a year is about half the 600–750 class hours it takes an English speaker to learn Spanish or French well.",
            "US Foreign Service Institute"
        ),
        AwarenessItem(
            AwarenessKind.GAIN,
            "Charge your phone outside the bedroom: no scrolling in bed, and your alarm gets you up to switch it off.",
            "Simple habit"
        ),
        AwarenessItem(
            AwarenessKind.GAIN,
            "You don't need to start perfectly. Every hour you don't scroll today is an hour you keep.",
            "TimeWise"
        )
    )

    fun byKind(kind: AwarenessKind): List<AwarenessItem> = items.filter { it.kind == kind }

    /** One of each kind, for the step before extra time. */
    fun pickOneOfEach(random: Random = Random): List<AwarenessItem> =
        AwarenessKind.entries.map { byKind(it).random(random) }

    /** Whole days a year spent at [dailyMillis] a day. */
    fun daysPerYear(dailyMillis: Long): Long = dailyMillis * DAYS_A_YEAR / DAY_MS
}
