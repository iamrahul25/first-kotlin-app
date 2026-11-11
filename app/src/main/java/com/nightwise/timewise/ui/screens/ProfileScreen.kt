package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.profile.ProfileSummary
import com.nightwise.timewise.data.profile.UsageProfile
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.ProfileAvatar
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.profileStyle
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlin.math.roundToInt

/** The gauge runs from 0 to 10 hours a day; anything beyond sits at the end. */
private const val GAUGE_MAX_HOURS = 10f
private const val HOUR_MS = 60 * 60 * 1000L

@Composable
fun ProfileScreen(
    summary: ProfileSummary?,
    usageAccessGranted: Boolean,
    onAllowUsageAccess: () -> Unit,
    onOpenFocus: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Your Profile", onBack = onBack)

        when {
            !usageAccessGranted -> UsageAccessCard(onAllow = onAllowUsageAccess)
            summary == null -> Text(
                text = "Your profile appears after your first full day of tracking. Check back tomorrow.",
                color = Slate,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusCard()
                    .padding(16.dp)
            )
            else -> {
                ProfileHero(summary)
                GaugeCard(summary)
                NextLevelCard(summary, onOpenFocus)
                CostCard(summary)
                SectionTitle("All levels")
                LevelLadder(current = summary.profile)
                Text(
                    text = "Based on your average over the last 7 finished days " +
                        "(${summary.trackedDays} of 7 with data). Today counts once it's over.",
                    color = Mist,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProfileHero(summary: ProfileSummary) {
    val style = profileStyle(summary.profile)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(style.dark, style.light)))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileAvatar(profile = summary.profile, size = 88.dp)
        Text(text = "YOU ARE", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(text = summary.profile.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text(
            text = summary.profile.tagline,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatDuration(summary.dailyAverageMillis)} a day",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            summary.previousDailyAverageMillis?.let { previous ->
                val (change, trend) = changeVsPrevious(summary.dailyAverageMillis, previous)
                ChangePill(text = change, trend = trend)
                Text(text = "vs week before", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GaugeCard(summary: ProfileSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Where you stand", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(5.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                UsageProfile.entries.forEach { level ->
                    Box(
                        modifier = Modifier
                            .weight(gaugeWeight(level))
                            .fillMaxHeight()
                            .background(profileStyle(level).light)
                    )
                }
            }
            val fraction = (summary.dailyAverageMillis.toFloat() / HOUR_MS / GAUGE_MAX_HOURS).coerceIn(0f, 1f)
            val marker = 22.dp
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - marker) * fraction)
                    .size(marker)
                    .background(Color.White, CircleShape)
                    .border(4.dp, profileStyle(summary.profile).dark, CircleShape)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            UsageProfile.entries.forEach { level ->
                Text(
                    text = level.title,
                    modifier = Modifier.weight(gaugeWeight(level)),
                    color = if (level == summary.profile) profileStyle(level).text else Mist,
                    fontSize = 10.sp,
                    fontWeight = if (level == summary.profile) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NextLevelCard(summary: ProfileSummary, onOpenFocus: () -> Unit) {
    val better = summary.profile.better
    val style = profileStyle(better ?: summary.profile)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = style.tint, bordered = false)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(profile = better ?: summary.profile, size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (better == null) {
                    Text(text = "You're at the top", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Stay under ${formatHours(summary.profile.maxDailyMillis)} a day to keep your Monk status.",
                        color = Slate,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                } else {
                    Text(text = "Level up to ${better.title}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Cut ${formatDuration(summary.cutToNextLevelMillis ?: 0L)} a day, every day this week, " +
                            "to get under ${formatHours(better.maxDailyMillis)}.",
                        color = Slate,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
        if (better != null) PrimaryButton(text = "Start a focus challenge", onClick = onOpenFocus, showArrow = true)
    }
}

@Composable
private fun CostCard(summary: ProfileSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "What it costs you", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CostStat(label = "A year", value = "${summary.daysPerYear} days", modifier = Modifier.weight(1f))
            CostStat(label = "Waking day", value = "${(summary.wakingShare * 100).roundToInt()}%", modifier = Modifier.weight(1f))
            CostStat(
                label = "30-day avg",
                value = summary.thirtyDayAverageMillis?.let(::formatDuration) ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "At this pace you spend ${summary.daysPerYear} full days a year staring at your phone. " +
                "Over 10 years that's ${"%.1f".format(summary.daysPerYear * 10 / 365f)} years of your life.",
            color = Slate,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun CostStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(StepperBg)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Slate, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LevelLadder(current: UsageProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        UsageProfile.entries.forEachIndexed { index, level ->
            val isCurrent = level == current
            val style = profileStyle(level)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCurrent) style.tint else Color.Transparent)
                    .then(if (isCurrent) Modifier.border(1.5.dp, style.light, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(profile = level, size = 40.dp, dimmed = !isCurrent)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = level.title, color = if (isCurrent) style.text else Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = rangeLabel(level), color = Slate, fontSize = 11.sp)
                }
                if (isCurrent) {
                    Text(
                        text = "YOU",
                        modifier = Modifier
                            .background(style.dark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (index < UsageProfile.entries.lastIndex && !isCurrent && UsageProfile.entries[index + 1] != current) {
                Box(
                    modifier = Modifier
                        .padding(start = 64.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )
            }
        }
    }
}

/** Each level's share of the 0–10 hour gauge. */
private fun gaugeWeight(level: UsageProfile): Float {
    val max = minOf(level.maxDailyMillis, (GAUGE_MAX_HOURS * HOUR_MS).toLong())
    return (max - level.minDailyMillis).toFloat() / HOUR_MS
}

private fun rangeLabel(level: UsageProfile): String = when {
    level.minDailyMillis == 0L -> "Under ${formatHours(level.maxDailyMillis)} a day"
    level.maxDailyMillis == Long.MAX_VALUE -> "${formatHours(level.minDailyMillis)}+ a day"
    else -> "${formatHours(level.minDailyMillis)} – ${formatHours(level.maxDailyMillis)} a day"
}

private fun formatHours(millis: Long): String = "${millis / HOUR_MS}h"
