package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.usage.AppCategory
import com.nightwise.timewise.data.usage.CategoryLogic
import com.nightwise.timewise.data.usage.CategoryRepository
import com.nightwise.timewise.data.usage.CategoryTotal
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.CategoryDonut
import com.nightwise.timewise.ui.components.CategoryLegendRow
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SegmentedToggle
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.components.rememberCategoryBreakdown
import com.nightwise.timewise.ui.components.uiColor
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CategoryPeriod(val label: String, val caption: String) {
    TODAY("Today", "Today"),
    WEEK("7 days", "Last 7 days"),
    MONTH("30 days", "Last 30 days")
}

/** Screen time by category for a period, with the apps in each. Tap an app to move it. */
@Composable
fun CategoriesScreen(usage: UsageSnapshot?, onBack: () -> Unit) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<String?>(null) }
    val selected = CategoryPeriod.entries[period]
    val apps = remember(usage, selected) {
        usage?.let {
            when (selected) {
                CategoryPeriod.TODAY -> it.today.apps
                CategoryPeriod.WEEK -> CategoryLogic.totalsAcross(it.thisWeek)
                CategoryPeriod.MONTH -> CategoryLogic.totalsAcross(it.last30Days)
            }
        }
    }
    val breakdown = rememberCategoryBreakdown(apps)
    val total = breakdown?.sumOf { it.millis } ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Usage by category", onBack = onBack)
        SegmentedToggle(
            options = CategoryPeriod.entries.map { it.label },
            selectedIndex = period,
            onSelect = { period = it }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = selected.caption, color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            CategoryDonut(totals = breakdown.orEmpty(), size = 180.dp)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                breakdown?.forEach { item ->
                    CategoryLegendRow(item, share = if (total > 0) item.millis.toFloat() / total else null)
                }
            }
        }

        Text(
            text = "Tap an app to move it to another category.",
            color = Mist,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        breakdown?.filter { it.millis > 0 }?.forEach { item ->
            CategoryAppsCard(item, onSelectApp = { editing = it })
        }
    }

    editing?.let { pkg ->
        CategoryPickerDialog(packageName = pkg, onDismiss = { editing = null })
    }
}

@Composable
private fun CategoryAppsCard(total: CategoryTotal, onSelectApp: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryLegendRow(total)
        val (shown, brief) = total.apps.partition { it.millis >= MINUTE_MS }
        shown.forEach { app ->
            val info = rememberAppInfo(app.packageName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectApp(app.packageName) }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(info, size = 30.dp)
                Text(
                    text = info?.label ?: "",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = formatDuration(app.millis), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (brief.isNotEmpty()) {
            Text(
                text = "+${brief.size} ${if (brief.size == 1) "app" else "apps"} under 1 minute",
                color = Mist,
                fontSize = 12.sp
            )
        }
    }
}

private const val MINUTE_MS = 60_000L

/** Moves one app to a category, or back to the automatic one. */
@Composable
private fun CategoryPickerDialog(packageName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { CategoryRepository.get(context) }
    val overrides by repository.overrides.collectAsState()
    val override = overrides[packageName]
    val automatic by produceState<AppCategory?>(null, packageName) {
        value = withContext(Dispatchers.IO) { repository.categoryOf(packageName, overrides = emptyMap()) }
    }
    val info = rememberAppInfo(packageName)
    val current = override ?: automatic

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(info, size = 40.dp) },
        title = { Text(info?.label ?: "Category", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppCategory.entries.forEach { category ->
                    val isSelected = category == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                repository.setOverride(packageName, category.takeIf { it != automatic })
                                onDismiss()
                            }
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) Forest else CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Forest))
                        }
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(category.uiColor))
                        Text(text = category.label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (category == automatic) Text(text = "Automatic", color = Mist, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (override != null) {
                TextButton(onClick = { repository.setOverride(packageName, null); onDismiss() }) {
                    Text("Reset to automatic", color = Forest)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) } }
    )
}
