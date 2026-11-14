package com.nightwise.timewise.data.usage

/** Buckets for "usage by category", in the order they're listed. */
enum class AppCategory(val label: String, val color: Long) {
    SOCIAL("Social", 0xFFE8577A),
    MEDIA("Video & music", 0xFF7C5CE0),
    GAMES("Games", 0xFF3B82F6),
    BROWSING("Browsing & news", 0xFFF59E0B),
    PRODUCTIVITY("Productivity", 0xFF3A9D6A),
    OTHER("Other", 0xFFB9B5AC);

    companion object {
        fun fromName(name: String?): AppCategory? = entries.firstOrNull { it.name == name }
    }
}

/** Mirrors `ApplicationInfo.CATEGORY_*` so classification stays plain Kotlin and testable. */
object SystemCategory {
    const val GAME = 0
    const val AUDIO = 1
    const val VIDEO = 2
    const val IMAGE = 3
    const val SOCIAL = 4
    const val NEWS = 5
    const val MAPS = 6
    const val PRODUCTIVITY = 7
    const val ACCESSIBILITY = 8
}

data class CategoryTotal(
    val category: AppCategory,
    val millis: Long,
    /** Most used first. */
    val apps: List<AppUsage>
)

object CategoryLogic {

    /**
     * The user's choice wins; then well-known apps, which often leave their store category
     * blank; then the category the app declares to Android (8.0+).
     */
    fun categorize(packageName: String, override: AppCategory?, systemCategory: Int?, isGame: Boolean): AppCategory {
        override?.let { return it }
        KNOWN_APPS[packageName]?.let { return it }
        if (isGame) return AppCategory.GAMES
        return when (systemCategory) {
            SystemCategory.GAME -> AppCategory.GAMES
            SystemCategory.AUDIO, SystemCategory.VIDEO, SystemCategory.IMAGE -> AppCategory.MEDIA
            SystemCategory.SOCIAL -> AppCategory.SOCIAL
            SystemCategory.NEWS -> AppCategory.BROWSING
            SystemCategory.PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            else -> AppCategory.OTHER
        }
    }

    /** Every category, used or not, so the legend always reads the same; most used first. */
    fun breakdown(apps: List<AppUsage>, categoryOf: (String) -> AppCategory): List<CategoryTotal> {
        val grouped = apps.filter { it.millis > 0 }.groupBy { categoryOf(it.packageName) }
        return AppCategory.entries
            .map { category ->
                val inCategory = grouped[category].orEmpty().sortedByDescending { it.millis }
                CategoryTotal(category, inCategory.sumOf { it.millis }, inCategory)
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.millis }.thenBy { it.category.ordinal })
    }

    /** Adds up each app's time across [days]. */
    fun totalsAcross(days: List<DayUsage>): List<AppUsage> =
        days.flatMap { it.apps }
            .groupBy { it.packageName }
            .map { (pkg, uses) -> AppUsage(pkg, uses.sumOf { it.millis }) }
            .sortedByDescending { it.millis }

    private val KNOWN_APPS: Map<String, AppCategory> = buildMap {
        listOf(
            "com.instagram.android", "com.facebook.katana", "com.facebook.lite", "com.facebook.orca",
            "com.whatsapp", "com.whatsapp.w4b", "com.snapchat.android", "com.reddit.frontpage",
            "com.twitter.android", "org.telegram.messenger", "com.discord", "com.linkedin.android",
            "com.pinterest", "com.instagram.barcelona", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "com.tumblr", "com.quora.android", "in.mohalla.sharechat", "com.bereal.ft"
        ).forEach { put(it, AppCategory.SOCIAL) }
        listOf(
            "com.google.android.youtube", "com.google.android.apps.youtube.music", "com.netflix.mediaclient",
            "com.spotify.music", "com.amazon.avod.thirdpartyclient", "in.startv.hotstar", "com.jio.media.ondemand",
            "com.mxtech.videoplayer.ad", "com.jio.jiosaavn", "com.gaana", "tv.twitch.android.app",
            "com.google.android.apps.photos", "com.vivo.gallery", "com.android.gallery3d"
        ).forEach { put(it, AppCategory.MEDIA) }
        listOf(
            "com.android.chrome", "com.brave.browser", "org.mozilla.firefox", "com.opera.browser",
            "com.opera.mini.native", "com.microsoft.emmx", "com.sec.android.app.sbrowser", "com.duckduckgo.mobile.android",
            "com.google.android.googlequicksearchbox", "com.vivo.browser", "com.google.android.apps.magazines",
            "com.medium.reader", "in.inshorts.app"
        ).forEach { put(it, AppCategory.BROWSING) }
        listOf(
            "com.Slack", "com.google.android.gm", "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
            "com.google.android.calendar", "com.google.android.keep", "notion.id", "com.microsoft.teams",
            "com.microsoft.office.outlook", "us.zoom.videomeetings", "com.google.android.apps.meetings",
            "com.todoist", "com.openai.chatgpt", "com.anthropic.claude", "com.github.android",
            "com.atlassian.android.jira.core", "com.microsoft.office.officehubrow", "com.dropbox.android",
            "com.nightwise.timewise"
        ).forEach { put(it, AppCategory.PRODUCTIVITY) }
    }
}
