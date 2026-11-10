package com.nightwise.scrollguard.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.data.FocusSettings
import com.nightwise.scrollguard.data.FocusStats
import com.nightwise.scrollguard.data.Platform
import com.nightwise.scrollguard.ui.components.FocusToggle
import com.nightwise.scrollguard.ui.components.ProtectionState
import com.nightwise.scrollguard.ui.components.ProtectionStatusPill
import com.nightwise.scrollguard.ui.components.SectionTitle
import com.nightwise.scrollguard.ui.components.SmallActionButton
import com.nightwise.scrollguard.ui.components.focusCard
import com.nightwise.scrollguard.ui.formatDuration
import com.nightwise.scrollguard.ui.formatLimitMinutes
import com.nightwise.scrollguard.ui.smallIcon
import com.nightwise.scrollguard.ui.theme.Amber
import com.nightwise.scrollguard.ui.theme.AmberTint
import com.nightwise.scrollguard.ui.theme.CardBorder
import com.nightwise.scrollguard.ui.theme.Forest
import com.nightwise.scrollguard.ui.theme.Ink
import com.nightwise.scrollguard.ui.theme.Mint
import com.nightwise.scrollguard.ui.theme.Mist
import com.nightwise.scrollguard.ui.theme.Red
import com.nightwise.scrollguard.ui.theme.Slate
import com.nightwise.scrollguard.ui.theme.StepperBg
import java.util.Calendar

@Composable
fun HomeScreen(
    settings: FocusSettings,
    stats: FocusStats,
    protectionState: ProtectionState,
    usageAccessGranted: Boolean,
    appUsageMillis: Map<String, Long>,
    onProtectionChange: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenLimits: () -> Unit,
    onAllowUsageAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GreetingHeader(
            protectionEnabled = settings.protectionEnabled,
            protectionState = protectionState,
            onProtectionChange = onProtectionChange
        )

        if (protectionState == ProtectionState.NeedsPermission) {
            PermissionBanner(onFix = onOpenPermissions)
        }

        HealthCard(stats = stats, onClick = onOpenStats)

        SectionTitle("Today")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStat(
                iconRes = R.drawable.ic_stat_time,
                label = "Time Wasted",
                value = formatDuration(stats.today.seconds),
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                iconRes = R.drawable.ic_stat_reels,
                label = "Reels Scrolled",
                value = stats.today.reels.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStat(
                icon = Icons.Rounded.Block,
                label = "Blocked Today",
                value = stats.today.blocked.toString(),
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                icon = Icons.Rounded.Shield,
                label = "Blocked All Time",
                value = stats.blockedTotal.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle("Today's Limits")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard(onClick = onOpenLimits)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LimitProgress(
                label = "Daily Timer",
                enabled = settings.dailyTimerEnabled,
                usedText = formatDuration(stats.today.seconds),
                limitText = formatLimitMinutes(settings.dailyTimerMinutes),
                fraction = stats.today.seconds / (settings.dailyTimerMinutes * 60f)
            )
            LimitProgress(
                label = "Reels Limit",
                enabled = settings.reelsLimitEnabled,
                usedText = stats.today.reels.toString(),
                limitText = settings.reelsLimit.toString(),
                fraction = stats.today.reels / settings.reelsLimit.toFloat()
            )
        }

        SectionTitle("App Usage Today")
        AppUsageCard(
            usageAccessGranted = usageAccessGranted,
            appUsageMillis = appUsageMillis,
            onAllowUsageAccess = onAllowUsageAccess
        )
    }
}

@Composable
private fun GreetingHeader(
    protectionEnabled: Boolean,
    protectionState: ProtectionState,
    onProtectionChange: (Boolean) -> Unit
) {
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
            Text(text = "Small steps today, big changes tomorrow.", color = Slate, fontSize = 12.sp)
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FocusToggle(checked = protectionEnabled, onCheckedChange = onProtectionChange)
            ProtectionStatusPill(state = protectionState)
        }
    }
}

@Composable
private fun PermissionBanner(onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmberTint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Protection isn't running", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Allow Accessibility access so FocusBlock can block short videos.",
                color = Slate,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        SmallActionButton(text = "Fix", onClick = onFix)
    }
}

@Composable
private fun HealthCard(stats: FocusStats, onClick: () -> Unit) {
    val (label, message) = healthSummary(stats)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = Mint, bordered = false, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_smiley),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Overall Health", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = label, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = message, color = Slate, fontSize = 11.sp)
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "Open stats",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (iconRes != null) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
        } else if (icon != null) {
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LimitProgress(
    label: String,
    enabled: Boolean,
    usedText: String,
    limitText: String,
    fraction: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (enabled) "$usedText / $limitText" else "Off",
                color = Slate,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (enabled) StepperBg else CardBorder.copy(alpha = 0.5f))
        ) {
            if (enabled) {
                val clamped = fraction.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (clamped >= 1f) Red else Forest)
                )
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    usageAccessGranted: Boolean,
    appUsageMillis: Map<String, Long>,
    onAllowUsageAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (usageAccessGranted) {
            Platform.entries.filter { it.supported }.forEach { platform ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(platform.smallIcon()),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = platform.label,
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDuration((appUsageMillis[platform.packageName] ?: 0L) / 1000),
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(text = "Total time in each app, not just short videos.", color = Mist, fontSize = 11.sp)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allow Usage Access to see how long you spend in YouTube, Instagram and Snapchat.",
                    modifier = Modifier.weight(1f),
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                SmallActionButton(text = "Allow", onClick = onAllowUsageAccess)
            }
        }
    }
}

private fun greetingForNow(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    else -> "Good Evening"
}

private fun healthSummary(stats: FocusStats): Pair<String, String> {
    val current = stats.thisWeekSeconds
    val previous = stats.lastWeekSeconds
    val blockedToday = stats.today.blocked
    if (previous <= 0L) {
        return when {
            current <= 30 * 60 && blockedToday > 0 -> "Great" to "$blockedToday short videos blocked today. Keep it up!"
            current <= 30 * 60 -> "Great" to "You're off to a calm start this week."
            else -> "Fair" to "Try a daily limit to cut back a little."
        }
    }
    val ratio = current.toDouble() / previous
    return when {
        ratio <= 0.5 -> "Great" to "You've cut your scrolling in half!"
        ratio < 1.0 -> "Good" to "You're doing better than last week!"
        ratio <= 1.2 -> "Fair" to "About the same as last week."
        else -> "Needs Work" to "You're scrolling more than last week."
    }
}
