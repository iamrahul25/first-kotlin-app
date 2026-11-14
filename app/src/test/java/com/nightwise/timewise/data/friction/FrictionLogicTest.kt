package com.nightwise.timewise.data.friction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionLogicTest {

    private val phrase = "I still want to watch the screen and make my life useless"

    @Test
    fun phraseIgnoresCaseSpacingAndEndPunctuation() {
        assertTrue(FrictionLogic.matchesPhrase("  i STILL want to watch   the screen and make my life useless. ", phrase))
        assertFalse(FrictionLogic.matchesPhrase("I still want to watch the screen", phrase))
        assertFalse(FrictionLogic.matchesPhrase("I still want to watch the screen and make my life useful", phrase))
    }

    @Test
    fun typoIsFlaggedOnlyOnceTypingGoesWrong() {
        assertFalse(FrictionLogic.hasTypo("", phrase))
        assertFalse(FrictionLogic.hasTypo("I still want", phrase))
        assertTrue(FrictionLogic.hasTypo("I stil want", phrase))
        assertFalse(FrictionLogic.hasTypo(phrase.uppercase(), phrase))
        assertEquals(12, FrictionLogic.correctPrefixLength("i still want", phrase))
    }

    @Test
    fun dreamMustMatchOneFromTheList() {
        val dreams = listOf(Dream("1", "Run a marathon"), Dream("2", "Start my own company"))
        assertEquals("2", FrictionLogic.matchingDream("start my own  company", dreams)?.id)
        assertNull(FrictionLogic.matchingDream("Start my own", dreams))
        assertNull(FrictionLogic.matchingDream("   ", dreams))
    }

    @Test
    fun stepsWithoutContentAreSkipped() {
        assertEquals(listOf(FrictionStep.PHRASE, FrictionStep.AWARENESS), FrictionLogic.steps(FrictionContent()))

        val full = FrictionContent(
            photos = listOf(LovedPhoto("p", "Mom", locked = false)),
            dreams = listOf(Dream("d", "Run a marathon")),
            todos = listOf(FrictionTodo("t", "Call grandma", done = false)),
            tracks = listOf(MotivationTrack("m", "content://song", "Song", null, hasArt = false))
        )
        assertEquals(FrictionStep.entries, FrictionLogic.steps(full))
    }

    @Test
    fun finishedTodosDontCount() {
        val content = FrictionContent(todos = listOf(FrictionTodo("t", "Call grandma", done = true)))
        assertFalse(FrictionStep.TODOS in FrictionLogic.steps(content))
    }

    @Test
    fun awarenessPicksOneOfEachKind() {
        val picked = Awareness.pickOneOfEach()
        assertEquals(AwarenessKind.entries, picked.map { it.kind })
        assertEquals(15L, Awareness.daysPerYear(60 * 60_000L))
        assertEquals(76L, Awareness.daysPerYear(5 * 60 * 60_000L))
    }
}
