package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.friction.Awareness
import com.nightwise.timewise.data.friction.AwarenessItem
import com.nightwise.timewise.data.friction.AwarenessKind
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint

private const val HOUR_MS = 60 * 60 * 1000L

/** Every fact, quote and "start today" note, with what the user's own average costs them. */
@Composable
fun AwarenessScreen(dailyAverageMillis: Long?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "The real cost", onBack = onBack)
        YourNumbersCard(dailyAverageMillis)
        AwarenessKind.entries.forEach { kind ->
            SectionTitle(kind.label)
            Awareness.byKind(kind).forEach { AwarenessCard(it) }
        }
    }
}

@Composable
private fun YourNumbersCard(dailyAverageMillis: Long?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(ForestDeep, Forest)))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "YOUR NUMBERS", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        if (dailyAverageMillis != null && dailyAverageMillis > 0) {
            Text(
                text = "${Awareness.daysPerYear(dailyAverageMillis)} days a year",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "At your ${formatDuration(dailyAverageMillis)} a day, that's how much of each year goes to your phone, around the clock.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (dailyAverageMillis > HOUR_MS) {
                Text(
                    text = "Cut 1 hour a day and you get ${Awareness.daysPerYear(HOUR_MS)} full days back this year.",
                    color = Mint,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = "Your numbers appear after a full day of tracking.",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AwarenessCard(item: AwarenessItem) {
    val (icon, tint, background) = when (item.kind) {
        AwarenessKind.FACT -> Triple(Icons.Rounded.Science, Amber, AmberTint)
        AwarenessKind.QUOTE -> Triple(Icons.Rounded.FormatQuote, Purple, PurpleTint)
        AwarenessKind.GAIN -> Triple(Icons.AutoMirrored.Rounded.TrendingUp, Forest, Mint)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(icon = icon, tint = tint, background = background, size = 32.dp, iconSize = 18.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (item.kind == AwarenessKind.QUOTE) "“${item.text}”" else item.text,
                color = Ink,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = if (item.kind == AwarenessKind.QUOTE) FontStyle.Italic else FontStyle.Normal
            )
            Text(text = item.source, color = Mist, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
