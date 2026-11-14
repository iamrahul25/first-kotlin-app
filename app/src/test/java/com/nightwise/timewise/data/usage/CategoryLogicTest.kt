package com.nightwise.timewise.data.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLogicTest {

    @Test
    fun userChoiceBeatsKnownAppsAndDeclaredCategory() {
        assertEquals(AppCategory.SOCIAL, CategoryLogic.categorize("com.instagram.android", null, null, isGame = false))
        assertEquals(
            AppCategory.PRODUCTIVITY,
            CategoryLogic.categorize("com.instagram.android", AppCategory.PRODUCTIVITY, SystemCategory.SOCIAL, isGame = false)
        )
    }

    @Test
    fun declaredCategoryIsUsedForUnknownApps() {
        assertEquals(AppCategory.GAMES, CategoryLogic.categorize("x.game", null, SystemCategory.GAME, isGame = false))
        assertEquals(AppCategory.GAMES, CategoryLogic.categorize("x.old", null, null, isGame = true))
        assertEquals(AppCategory.MEDIA, CategoryLogic.categorize("x.video", null, SystemCategory.VIDEO, isGame = false))
        assertEquals(AppCategory.BROWSING, CategoryLogic.categorize("x.news", null, SystemCategory.NEWS, isGame = false))
        assertEquals(AppCategory.OTHER, CategoryLogic.categorize("x.maps", null, SystemCategory.MAPS, isGame = false))
        assertEquals(AppCategory.OTHER, CategoryLogic.categorize("x.unknown", null, null, isGame = false))
    }

    @Test
    fun breakdownListsEveryCategoryMostUsedFirst() {
        val apps = listOf(AppUsage("a", 30), AppUsage("b", 50), AppUsage("c", 10), AppUsage("d", 0))
        val categories = mapOf("a" to AppCategory.SOCIAL, "b" to AppCategory.PRODUCTIVITY, "c" to AppCategory.SOCIAL)
        val breakdown = CategoryLogic.breakdown(apps) { categories[it] ?: AppCategory.OTHER }

        assertEquals(AppCategory.entries.size, breakdown.size)
        assertEquals(AppCategory.PRODUCTIVITY, breakdown[0].category)
        assertEquals(AppCategory.SOCIAL, breakdown[1].category)
        assertEquals(40L, breakdown[1].millis)
        assertEquals(listOf("a", "c"), breakdown[1].apps.map { it.packageName })
        assertEquals(0L, breakdown.first { it.category == AppCategory.OTHER }.millis)
    }

    private fun day(epochDay: Long, vararg apps: AppUsage) =
        DayUsage(epochDay, apps.sumOf { it.millis }, apps.toList(), null, List(24) { 0 })

    @Test
    fun totalsAddUpAcrossDays() {
        val totals = CategoryLogic.totalsAcross(listOf(day(1, AppUsage("a", 5), AppUsage("b", 1)), day(2, AppUsage("a", 2))))
        assertEquals(listOf(AppUsage("a", 7), AppUsage("b", 1)), totals)
    }
}
