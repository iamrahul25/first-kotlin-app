package com.nightwise.scrollguard.data

enum class Platform(
    val packageName: String,
    val label: String,
    val subtitle: String,
    val supported: Boolean
) {
    YOUTUBE("com.google.android.youtube", "YouTube", "Shorts", supported = true),
    INSTAGRAM("com.instagram.android", "Instagram", "Reels", supported = true),
    SNAPCHAT("com.snapchat.android", "Snapchat", "Spotlight & short videos", supported = false),
    FACEBOOK("com.facebook.katana", "Facebook", "Reels & short videos", supported = false);

    companion object {
        fun fromPackage(packageName: String): Platform? =
            entries.firstOrNull { it.supported && it.packageName == packageName }
    }
}
