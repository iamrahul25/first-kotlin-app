package com.nightwise.timewise.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cyclone
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DoNotTouch
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.data.badges.BadgeKind
import com.nightwise.timewise.data.badges.BadgeRecord
import com.nightwise.timewise.data.badges.BadgeTier
import com.nightwise.timewise.data.badges.UsageBadge
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Slate

/** Medal colors: [light] and [dark] make the gradient, [tint] backs chips and section headers. */
data class TierStyle(val light: Color, val dark: Color, val tint: Color, val text: Color)

fun tierStyle(tier: BadgeTier): TierStyle = when (tier) {
    BadgeTier.BRONZE -> TierStyle(Color(0xFFF0B27A), Color(0xFF9C5221), Color(0xFFFBEBDD), Color(0xFF8A4A1E))
    BadgeTier.SILVER -> TierStyle(Color(0xFFE3E8EE), Color(0xFF7D8896), Color(0xFFEFF2F5), Color(0xFF4B5563))
    BadgeTier.GOLD -> TierStyle(Color(0xFFFFDE73), Color(0xFFC98A0B), Color(0xFFFFF5D6), Color(0xFF92600A))
    BadgeTier.LEGENDARY -> TierStyle(Color(0xFFC4B0FF), Color(0xFF5B34D1), Color(0xFFEFE9FD), Color(0xFF5B34D1))
    BadgeTier.MILD -> TierStyle(Color(0xFFFDBA74), Color(0xFFC2410C), Color(0xFFFFEDD5), Color(0xFFC2410C))
    BadgeTier.SEVERE -> TierStyle(Color(0xFFF87171), Color(0xFF991B1B), Color(0xFFFDECEC), Color(0xFFB42318))
    BadgeTier.EXTREME -> TierStyle(Color(0xFF7F1D1D), Color(0xFF1C0A0A), Color(0xFFF3E4E4), Color(0xFF7F1D1D))
}

fun badgeIcon(badge: UsageBadge): ImageVector = when (badge) {
    UsageBadge.MINDFUL_DAY -> Icons.Rounded.WbSunny
    UsageBadge.SWEET_DREAMS -> Icons.Rounded.Bedtime
    UsageBadge.COMEBACK_KID -> Icons.AutoMirrored.Rounded.TrendingDown
    UsageBadge.BALANCED_WEEK -> Icons.Rounded.Balance
    UsageBadge.LIGHT_TOUCH -> Icons.Rounded.TouchApp
    UsageBadge.HANDS_OFF -> Icons.Rounded.DoNotTouch
    UsageBadge.STEADY_SEVEN -> Icons.Rounded.EventAvailable
    UsageBadge.FREE_SPIRIT -> Icons.Rounded.Hiking
    UsageBadge.ZEN_MASTER -> Icons.Rounded.Spa
    UsageBadge.IRON_WILL -> Icons.Rounded.Shield
    UsageBadge.CLEAN_MONTH -> Icons.Rounded.Verified
    UsageBadge.GHOST_MODE -> Icons.Rounded.VisibilityOff
    UsageBadge.DIGITAL_MONK -> Icons.Rounded.SelfImprovement
    UsageBadge.UNBREAKABLE -> Icons.Rounded.Diamond
    UsageBadge.OFFLINE_LEGEND -> Icons.Rounded.MilitaryTech
    UsageBadge.COUCH_POTATO -> Icons.Rounded.Weekend
    UsageBadge.RABBIT_HOLE -> Icons.Rounded.AllInclusive
    UsageBadge.MIDNIGHT_SCROLLER -> Icons.Rounded.NightsStay
    UsageBadge.SCREEN_ADDICT -> Icons.Rounded.PhoneAndroid
    UsageBadge.COMPULSIVE_CHECKER -> Icons.Rounded.LockOpen
    UsageBadge.BINGE_WEEK -> Icons.Rounded.LiveTv
    UsageBadge.GLUED_TO_GLASS -> Icons.Rounded.Link
    UsageBadge.ZOMBIE_MODE -> Icons.Rounded.SentimentVeryDissatisfied
    UsageBadge.DOOM_SPIRAL -> Icons.Rounded.Cyclone
    UsageBadge.LOST_WEEK -> Icons.Rounded.HourglassBottom
    UsageBadge.DIGITAL_PRISONER -> Icons.Rounded.Dangerous
}

/**
 * A round medal: tier gradient and glow once earned, grey with a lock (achievements) or a
 * check (warnings you have avoided) before that. [highlight] gently pulses new wins.
 */
@Composable
fun BadgeMedal(
    badge: UsageBadge,
    earned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    highlight: Boolean = false
) {
    val style = tierStyle(badge.tier)
    val pulse = if (highlight) {
        val transition = rememberInfiniteTransition(label = "medal")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "medalScale"
        )
        scale
    } else 1f

    Box(modifier = modifier.size(size + size / 8), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulse)
                .then(
                    if (earned) Modifier.shadow(size / 6, CircleShape, ambientColor = style.dark, spotColor = style.dark)
                    else Modifier
                )
                .background(
                    brush = if (earned) Brush.linearGradient(listOf(style.light, style.dark)) else SolidColor(NeutralChip),
                    shape = CircleShape
                )
                .border(
                    width = size / 18,
                    brush = if (earned) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), style.light)) else SolidColor(CardWhite),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.7f)
                    .background(
                        brush = if (earned) Brush.linearGradient(listOf(style.dark, style.light)) else SolidColor(CardWhite),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon(badge),
                    contentDescription = null,
                    tint = if (earned) Color.White else Mist.copy(alpha = 0.7f),
                    modifier = Modifier.size(size * 0.38f)
                )
            }
        }
        if (!earned) {
            val avoided = badge.kind == BadgeKind.WARNING
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -(size / 16), y = -(size / 16))
                    .size(size / 3.2f)
                    .background(if (avoided) Forest else Slate, CircleShape)
                    .border(2.dp, CardWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (avoided) Icons.Rounded.Check else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size / 5.5f)
                )
            }
        }
    }
}

/** "×5" under an earned medal. */
@Composable
fun BadgeCountChip(badge: UsageBadge, count: Int, modifier: Modifier = Modifier) {
    val style = tierStyle(badge.tier)
    Text(
        text = "×$count",
        modifier = modifier
            .background(Brush.horizontalGradient(listOf(style.dark, style.light)), RoundedCornerShape(10.dp))
            .border(1.5.dp, CardWhite, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

/** Entry card to the badges screen, showing the latest wins. */
@Composable
fun BadgesEntryCard(records: Map<UsageBadge, BadgeRecord>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val achievements = UsageBadge.entries.filter { it.kind == BadgeKind.ACHIEVEMENT }
    val unlocked = achievements.count { records[it]?.earned == true }
    val warnings = UsageBadge.entries.count { it.kind == BadgeKind.WARNING && records[it]?.earned == true }
    val newWins = records.values.sumOf { it.unseen }
    val recent = UsageBadge.entries
        .filter { records[it]?.earned == true }
        .sortedByDescending { records[it]?.lastEarnedDay }
        .take(3)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (recent.isEmpty()) {
            IconTile(icon = Icons.Rounded.EmojiEvents, tint = Forest, background = Mint, size = 44.dp, iconSize = 26.dp)
        } else {
            Box {
                recent.forEachIndexed { index, badge ->
                    BadgeMedal(badge = badge, earned = true, size = 34.dp, modifier = Modifier.offset(x = (index * 22).dp))
                }
                Box(modifier = Modifier.size(width = (38 + (recent.size - 1) * 22).dp, height = 1.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Badges", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (newWins > 0) {
                    Text(
                        text = "$newWins new",
                        modifier = Modifier
                            .background(Forest, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = if (unlocked + warnings == 0) "Earn your first by keeping screen time low"
                else "$unlocked of ${achievements.size} unlocked · $warnings warnings",
                color = Slate,
                fontSize = 11.sp
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "Open badges",
            modifier = Modifier.size(18.dp)
        )
    }
}
