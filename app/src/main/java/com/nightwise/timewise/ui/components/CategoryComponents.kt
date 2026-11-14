package com.nightwise.timewise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.usage.AppCategory
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.CategoryLogic
import com.nightwise.timewise.data.usage.CategoryRepository
import com.nightwise.timewise.data.usage.CategoryTotal
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

val AppCategory.uiColor: Color get() = Color(color)

/** [apps] split by category, following the user's own category choices. Null while loading. */
@Composable
fun rememberCategoryBreakdown(apps: List<AppUsage>?): List<CategoryTotal>? {
    val context = LocalContext.current
    val repository = remember { CategoryRepository.get(context) }
    val overrides by repository.overrides.collectAsState()
    val breakdown by produceState<List<CategoryTotal>?>(null, apps, overrides) {
        if (apps == null) return@produceState
        value = withContext(Dispatchers.IO) { CategoryLogic.breakdown(apps) { repository.categoryOf(it, overrides) } }
    }
    return breakdown
}

/** Ring split by category, with the total in the middle. */
@Composable
fun CategoryDonut(totals: List<CategoryTotal>, size: Dp, modifier: Modifier = Modifier, stroke: Dp = size / 9) {
    val total = totals.sumOf { it.millis }
    val used = totals.filter { it.millis > 0 }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val width = stroke.toPx()
            val arcSize = Size(this.size.width - width, this.size.height - width)
            val topLeft = Offset(width / 2, width / 2)
            drawArc(StepperBg, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
            if (total <= 0) return@Canvas
            val gap = if (used.size > 1) GAP_DEGREES else 0f
            var start = -90f
            used.forEach { slice ->
                val sweep = 360f * slice.millis / total
                drawArc(
                    color = slice.category.uiColor,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "USAGE", color = Slate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Text(text = formatDuration(total), color = Ink, fontSize = if (size >= 160.dp) 24.sp else 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryLegendRow(total: CategoryTotal, modifier: Modifier = Modifier, share: Float? = null) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(total.category.uiColor)
        )
        Text(
            text = total.category.label,
            modifier = Modifier.weight(1f),
            color = if (total.millis > 0) Ink else Mist,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        if (share != null && total.millis > 0) {
            val percent = (share * 100).roundToInt()
            Text(text = if (percent < 1) "<1%" else "$percent%", color = Slate, fontSize = 11.sp)
        }
        Text(
            text = formatDuration(total.millis),
            color = if (total.millis > 0) Ink else Mist,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Today's screen time by category, for Home. */
@Composable
fun CategoryUsageCard(apps: List<AppUsage>?, onViewAll: () -> Unit, modifier: Modifier = Modifier) {
    val breakdown = rememberCategoryBreakdown(apps)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Usage by category", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                text = "View all ›",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StepperBg)
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Forest,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDonut(totals = breakdown.orEmpty(), size = 124.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (breakdown == null) {
                    Text(text = "Loading…", color = Slate, fontSize = 12.sp)
                } else {
                    breakdown.forEach { CategoryLegendRow(it) }
                }
            }
        }
    }
}

private const val GAP_DEGREES = 3f
