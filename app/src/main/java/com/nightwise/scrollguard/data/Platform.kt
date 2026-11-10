package com.nightwise.scrollguard.data

enum class Platform(
    val packageName: String,
    val label: String,
    val subtitle: String,
    val supported: Boolean,
    /** Browsers where this platform's short-form videos are detected on its website. */
    val browserPackages: Set<String> = emptySet()
) {
    YOUTUBE(
        "com.google.android.youtube", "YouTube", "Shorts in app & Brave", supported = true,
        browserPackages = setOf("com.brave.browser")
    ),
    INSTAGRAM("com.instagram.android", "Instagram", "Reels", supported = true),
    SNAPCHAT("com.snapchat.android", "Snapchat", "Spotlight & short videos", supported = true),
    FACEBOOK("com.facebook.katana", "Facebook", "Reels & short videos", supported = false);

    companion object {
        fun fromPackage(packageName: String): Platform? =
            entries.firstOrNull {
                it.supported && (it.packageName == packageName || packageName in it.browserPackages)
            }
    }
}
