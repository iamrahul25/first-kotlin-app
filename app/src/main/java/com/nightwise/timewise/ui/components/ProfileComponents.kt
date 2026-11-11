package com.nightwise.timewise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Phishing
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.profile.UsageProfile
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Slate

/** [light] and [dark] make the level's gradient, [tint] backs chips, [text] is for labels on white. */
data class ProfileStyle(val light: Color, val dark: Color, val tint: Color, val text: Color)

fun profileStyle(profile: UsageProfile): ProfileStyle = when (profile) {
    UsageProfile.MONK -> ProfileStyle(Color(0xFF4FAF7F), Color(0xFF1F4E3A), Color(0xFFE4F2E8), Color(0xFF2F6B4F))
    UsageProfile.MINDFUL -> ProfileStyle(Color(0xFF5BB5C9), Color(0xFF1E6A7D), Color(0xFFE2F3F6), Color(0xFF1E6A7D))
    UsageProfile.HOOKED -> ProfileStyle(Color(0xFFF5B942), Color(0xFFB45309), Color(0xFFFDF3E1), Color(0xFFB45309))
    UsageProfile.ADDICT -> ProfileStyle(Color(0xFFF26B5B), Color(0xFF991B1B), Color(0xFFFDECEC), Color(0xFFB42318))
    UsageProfile.ZOMBIE -> ProfileStyle(Color(0xFF7F1D1D), Color(0xFF1C0A0A), Color(0xFFF3E4E4), Color(0xFF7F1D1D))
}

fun profileIcon(profile: UsageProfile): ImageVector = when (profile) {
    UsageProfile.MONK -> Icons.Rounded.SelfImprovement
    UsageProfile.MINDFUL -> Icons.Rounded.Spa
    UsageProfile.HOOKED -> Icons.Rounded.Phishing
    UsageProfile.ADDICT -> Icons.Rounded.Smartphone
    UsageProfile.ZOMBIE -> Icons.Rounded.SentimentVeryDissatisfied
}

/** Round level avatar; grey placeholder while the level is unknown. */
@Composable
fun ProfileAvatar(profile: UsageProfile?, size: Dp, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val style = profile?.let(::profileStyle)
    val brush = when {
        style == null -> SolidColor(NeutralChip)
        dimmed -> SolidColor(style.tint)
        else -> Brush.linearGradient(listOf(style.light, style.dark))
    }
    Box(
        modifier = modifier
            .size(size)
            .background(brush, CircleShape)
            .border(size / 20, if (dimmed || style == null) CardWhite else Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = profile?.let(::profileIcon) ?: Icons.Rounded.Person,
            contentDescription = null,
            tint = when {
                style == null -> Mist
                dimmed -> style.text.copy(alpha = 0.6f)
                else -> Color.White
            },
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/** Compact "avatar + level" chip for the Home header. */
@Composable
fun ProfilePill(profile: UsageProfile?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = profile?.let(::profileStyle)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(style?.tint ?: NeutralChip)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = profile, size = 26.dp)
        Text(
            text = profile?.title ?: "Profile",
            color = style?.text ?: Slate,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
