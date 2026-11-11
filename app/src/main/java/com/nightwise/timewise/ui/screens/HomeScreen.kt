package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.data.badges.BadgeRecord
import com.nightwise.timewise.data.badges.UsageBadge
import com.nightwise.timewise.data.challenge.ActiveChallenge
import com.nightwise.timewise.data.profile.ProfileLogic
import com.nightwise.timewise.data.profile.UsageProfile
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.AppUsageRow
import com.nightwise.timewise.ui.components.BadgesEntryCard
import com.nightwise.timewise.ui.components.profileStyle
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.HourlyTimeline
import com.nightwise.timewise.ui.components.ProfilePill
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Slate
import java.util.Calendar

private const val TOP_APPS = 5
/** The usage meter runs from 0 to 10 hours, two hours per level. */
private const val METER_MAX_MS = 10 * 60 * 60 * 1000L

@Composable
fun HomeScreen(
    usage: UsageSnapshot?,
    usageAccessGranted: Boolean,
    activeChallenge: ActiveChallenge?,
    badgeRecords: Map<UsageBadge, BadgeRecord>,
    profile: UsageProfile?,
    onAllowUsageAccess: () -> Unit,
    onOpenApps: (Long) -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenBadges: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GreetingHeader(profile = profile, onOpenProfile = onOpenProfile)

        if (!usageAccessGranted) {
            UsageAccessCard(onAllow = onAllowUsageAccess)
            return@Column
        }

        HeroCard(usage)

        FocusTeaser(activeChallenge = activeChallenge, onClick = onOpenFocus)

        BadgesEntryCard(records = badgeRecords, onClick = onOpenBadges)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val today = usage?.today
            val yesterdayUnlocks = usage?.yesterday?.unlocks
            MiniStat(
                icon = Icons.Rounded.LockOpen,
                label = "Unlocks",
                value = today?.unlocks?.toString() ?: "—",
                caption = yesterdayUnlocks?.let { "Yesterday $it" } ?: "Not supported",
                modifier = Modifier.weight(1f)
            )
            val top = today?.apps?.firstOrNull()
            MostUsedStat(
                packageName = top?.packageName,
                millis = top?.millis ?: 0L,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Most used today", modifier = Modifier.weight(1f))
            if (usage != null && usage.today.apps.size > TOP_APPS) {
                Text(
                    text = "See all",
                    color = Forest,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenApps(usage.today.epochDay) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        TopAppsCard(usage, onOpenApp)

        SectionTitle("Today's Timeline")
        HourlyTimeline(
            hourlyMinutes = usage?.today?.hourlyMinutes ?: List(24) { 0 },
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun FocusTeaser(activeChallenge: ActiveChallenge?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = Mint, bordered = false, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (activeChallenge != null) "${activeChallenge.type.label} in progress" else "Take a focus challenge",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (activeChallenge != null) "Stay off your phone until it ends." else "Put the phone down for 1 hour and earn badges.",
                color = Slate,
                fontSize = 11.sp
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "Open focus",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GreetingHeader(profile: UsageProfile?, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sun),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = greetingForNow(), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Here's where your time went today.", color = Slate, fontSize = 12.sp)
        }
        ProfilePill(profile = profile, onClick = onOpenProfile)
    }
}

@Composable
private fun HeroCard(usage: UsageSnapshot?) {
    val todayMillis = usage?.today?.totalMillis
    val level = todayMillis?.let(ProfileLogic::profileFor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = ForestDeep, bordered = false)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Screen time today",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (level != null) LevelChip(level)
        }
        Text(
            text = todayMillis?.let(::formatDuration) ?: "—",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        if (usage != null && level != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (change, trend) = changeVsPrevious(usage.today.totalMillis, usage.yesterday.totalMillis)
                ChangePill(text = change, trend = trend)
                Text(
                    text = "vs yesterday (${formatDuration(usage.yesterday.totalMillis)})",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
            UsageMeter(todayMillis, level, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Dot in the level's color plus its name, e.g. "● Addict". */
@Composable
private fun LevelChip(level: UsageProfile) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(profileStyle(level).light, CircleShape)
        )
        Text(text = level.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Thin 0–10h meter split into the five levels, with a marker at today's total. */
@Composable
private fun UsageMeter(todayMillis: Long, level: UsageProfile, modifier: Modifier = Modifier) {
    val fraction = (todayMillis.toFloat() / METER_MAX_MS).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                UsageProfile.entries.forEach { band ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(profileStyle(band).light.copy(alpha = if (band.ordinal <= level.ordinal) 0.95f else 0.3f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - 12.dp) * fraction)
                    .size(12.dp)
                    .background(Color.White, CircleShape)
                    .border(3.dp, profileStyle(level).light, CircleShape)
            )
        }
        val next = UsageProfile.entries.getOrNull(level.ordinal + 1)
        Text(
            text = if (next == null) "Past 8 hours today"
            else "${formatDuration(level.maxDailyMillis - todayMillis)} left before ${next.title}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MiniStat(
    icon: ImageVector,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatIcon(icon)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = caption, color = Mist, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MostUsedStat(packageName: String?, millis: Long, modifier: Modifier = Modifier) {
    val info = packageName?.let { rememberAppInfo(it) }
    Row(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatIcon(Icons.Rounded.Star)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Most used", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                text = info?.label ?: "—",
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = if (packageName != null) formatDuration(millis) else "No usage yet", color = Mist, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Mint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun TopAppsCard(usage: UsageSnapshot?, onOpenApp: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val apps = usage?.today?.apps.orEmpty()
        if (apps.isEmpty()) {
            Text(
                text = if (usage == null) "Loading…" else "No app usage recorded yet today.",
                color = Slate,
                fontSize = 12.sp
            )
        } else {
            val max = apps.first().millis
            apps.take(TOP_APPS).forEach { app ->
                AppUsageRow(
                    packageName = app.packageName,
                    millis = app.millis,
                    maxMillis = max,
                    onClick = { onOpenApp(app.packageName) }
                )
            }
        }
    }
}

private fun greetingForNow(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    else -> "Good Evening"
}
