package com.nightwise.timewise.data.friction

import kotlin.random.Random

/** A photo of someone the user wants to do better for. [locked] ones stay hidden until the PIN is entered. */
data class LovedPhoto(val id: String, val caption: String, val locked: Boolean)

data class Dream(val id: String, val text: String)

data class FrictionTodo(val id: String, val text: String, val done: Boolean)

/** An audio file picked from storage; [hasArt] when its embedded cover was saved. */
data class MotivationTrack(val id: String, val uri: String, val title: String, val artist: String?, val hasArt: Boolean)

/** Everything the user set up for the steps that stand between them and extra time. */
data class FrictionContent(
    val photos: List<LovedPhoto> = emptyList(),
    val dreams: List<Dream> = emptyList(),
    val todos: List<FrictionTodo> = emptyList(),
    val tracks: List<MotivationTrack> = emptyList(),
    val hasPin: Boolean = false
) {
    val openTodos: List<FrictionTodo> get() = todos.filter { !it.done }
}

/** In the order they're shown. */
enum class FrictionStep(val title: String, val builtIn: Boolean) {
    PHRASE("Type it out", builtIn = true),
    PEOPLE("Your people", builtIn = false),
    DREAM("Your dreams", builtIn = false),
    AWARENESS("The real cost", builtIn = true),
    TODOS("Your to-dos", builtIn = false),
    MUSIC("Your music", builtIn = false)
}

object FrictionLogic {
    const val PEOPLE_SECONDS = 5
    const val AWARENESS_SECONDS = 10
    const val MUSIC_SECONDS = 30

    val PHRASES = listOf(
        "I still want to watch the screen and make my life useless",
        "I am choosing my phone over my dreams right now",
        "These fifteen minutes will never come back to me",
        "I know more scrolling will not make me feel better",
        "I am trading my future for a few more videos",
        "My family and my goals can wait while I scroll",
        "I promised myself I would stop and I am breaking that promise",
        "Nothing I am about to watch will matter tomorrow",
        "I am letting an app decide how I spend my life",
        "I could be building my dream instead of watching others live theirs"
    )

    fun randomPhrase(random: Random = Random): String = PHRASES.random(random)

    /** Steps the user goes through; ones that need content are skipped until it's set up. */
    fun steps(content: FrictionContent): List<FrictionStep> = FrictionStep.entries.filter { isReady(it, content) }

    fun isReady(step: FrictionStep, content: FrictionContent): Boolean = when (step) {
        FrictionStep.PHRASE, FrictionStep.AWARENESS -> true
        FrictionStep.PEOPLE -> content.photos.isNotEmpty()
        FrictionStep.DREAM -> content.dreams.isNotEmpty()
        FrictionStep.TODOS -> content.openTodos.isNotEmpty()
        FrictionStep.MUSIC -> content.tracks.isNotEmpty()
    }

    /** Case, extra spaces and end punctuation don't matter; every word does. */
    fun normalize(text: String): String =
        text.trim().lowercase().replace(WHITESPACE, " ").trimEnd('.', '!', '?').trim()

    fun matchesPhrase(typed: String, phrase: String): Boolean = normalize(typed) == normalize(phrase)

    fun matchingDream(typed: String, dreams: List<Dream>): Dream? {
        val normalized = normalize(typed)
        if (normalized.isEmpty()) return null
        return dreams.firstOrNull { normalize(it.text) == normalized }
    }

    /** How much of [phrase] has been typed correctly from the start, for a progress hint. */
    fun correctPrefixLength(typed: String, phrase: String): Int {
        val a = loose(typed)
        val b = phrase.lowercase()
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        return i
    }

    /** True once something typed doesn't match the phrase so far. */
    fun hasTypo(typed: String, phrase: String): Boolean =
        !matchesPhrase(typed, phrase) && correctPrefixLength(typed, phrase) < loose(typed).length

    private fun loose(text: String): String = text.lowercase().replace(WHITESPACE, " ").trimStart()

    private val WHITESPACE = Regex("\\s+")
}
