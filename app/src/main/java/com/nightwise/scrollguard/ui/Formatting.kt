package com.nightwise.scrollguard.ui

import androidx.annotation.DrawableRes
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.data.Platform
import com.nightwise.scrollguard.ui.components.Trend
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** "3h 24m", "24m", "0m". */
fun formatDuration(seconds: Long): String {
    val totalMinutes = seconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** "2h 00m", the format used for limit values. */
fun formatLimitMinutes(minutes: Int): String =
    String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60)

/** Week-over-week change, where going down is the good direction. */
fun weeklyChange(current: Long, previous: Long): Pair<String, Trend> {
    if (previous <= 0L) return "—" to Trend.Neutral
    val percent = (abs(current - previous) * 100.0 / previous).roundToInt()
    return when {
        current < previous -> "↓ $percent%" to Trend.Better
        current > previous -> "↑ $percent%" to Trend.Worse
        else -> "0%" to Trend.Neutral
    }
}

@DrawableRes
fun Platform.smallIcon(): Int = when (this) {
    Platform.YOUTUBE -> R.drawable.ic_youtube_32
    Platform.INSTAGRAM -> R.drawable.ic_instagram_32
    Platform.SNAPCHAT -> R.drawable.ic_snapchat_32
    Platform.FACEBOOK -> R.drawable.ic_facebook_32
}

@DrawableRes
fun Platform.largeIcon(): Int = when (this) {
    Platform.YOUTUBE -> R.drawable.ic_youtube_40
    Platform.INSTAGRAM -> R.drawable.ic_instagram_40
    Platform.SNAPCHAT -> R.drawable.ic_snapchat_40
    Platform.FACEBOOK -> R.drawable.ic_facebook_40
}
