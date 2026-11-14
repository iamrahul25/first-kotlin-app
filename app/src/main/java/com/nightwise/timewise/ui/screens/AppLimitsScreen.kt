package com.nightwise.timewise.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.data.limits.LimitsState
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.AppLimitRow
import com.nightwise.timewise.ui.components.FocusTextField
import com.nightwise.timewise.ui.components.FocusToggle
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.LimitDialog
import com.nightwise.timewise.ui.components.OutlineButton
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SegmentedToggle
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The "App limits" part of the Focus tab. */
@Composable
fun AppLimitsSection(
    state: LimitsState,
    todayApps: List<AppUsage>,
    accessibilityEnabled: Boolean,
    frictionReadySteps: Int,
    frictionTotalSteps: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onAddLimit: () -> Unit,
    onEditLimit: (String) -> Unit,
    onEnableAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenFrictionSetup: () -> Unit,
    onOpenAwareness: () -> Unit
) {
    val today = FocusRepository.currentEpochDay()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "Daily app limits", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "When an app's time is up it closes. Getting ${LimitLogic.EXTRA_MINUTES} more minutes takes going through your steps.",
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            FocusToggle(checked = state.enabled, onCheckedChange = onToggleEnabled)
        }

        if (!accessibilityEnabled) AccessibilityNotice(onEnableAccessibility, onOpenAppInfo)

        if (state.limits.isEmpty()) {
            Text(
                text = "No limits yet. Start with the app that takes most of your time.",
                color = Mist,
                fontSize = 12.sp
            )
        } else {
            val usedByApp = todayApps.associate { it.packageName to it.millis }
            state.limits.forEach { limit ->
                AppLimitRow(
                    limit = limit,
                    usedMillis = usedByApp[limit.packageName] ?: 0L,
                    extraMinutes = state.extraMinutes(limit.packageName, today),
                    onClick = { onEditLimit(limit.packageName) }
                )
            }
        }
        OutlineButton(onClick = onAddLimit) {
            Text(text = "+ Add app limit", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    NavRow(
        icon = Icons.Rounded.Shield,
        tint = Forest,
        tileColor = Mint,
        title = "Extra-time steps",
        subtitle = "$frictionReadySteps of $frictionTotalSteps ready · photos, dreams, to-dos, music",
        onClick = onOpenFrictionSetup
    )
    NavRow(
        icon = Icons.Rounded.Lightbulb,
        tint = Purple,
        tileColor = PurpleTint,
        title = "The real cost",
        subtitle = "Facts, quotes and what cutting back gives you",
        onClick = onOpenAwareness
    )
}

@Composable
private fun AccessibilityNotice(onEnable: () -> Unit, onOpenAppInfo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmberTint)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Rounded.Accessibility, tint = Amber, background = Color.White, size = 32.dp, iconSize = 18.dp)
            Text(
                text = "Turn on TimeWise in Accessibility so limits can close apps. It only sees which app is open, never what's on screen.",
                modifier = Modifier.weight(1f),
                color = Amber,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            SmallActionButton(text = "Allow", onClick = onEnable)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Text(
                text = "Switch greyed out? Open App info, tap ⋮ and choose \"Allow restricted settings\", then try again.",
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenAppInfo),
                color = Amber,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, tint: Color, tileColor: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint, background = tileColor, size = 36.dp, iconSize = 20.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Slate, fontSize = 12.sp)
        }
        Text(text = "›", color = Mist, fontSize = 20.sp)
    }
}

/** Every launchable app, highest average a day over the last week first, to pick one to limit. */
@Composable
fun AppLimitPickerScreen(
    state: LimitsState,
    todayApps: List<AppUsage>,
    dailyAverages: Map<String, Long>,
    onSetLimit: (String, Int) -> Unit,
    onRemoveLimit: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val installed by produceState<List<Pair<String, String>>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        }
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var weekAverage by rememberSaveable { mutableStateOf(false) }
    val usedByApp = remember(todayApps) { todayApps.associate { it.packageName to it.millis } }
    val shown = if (weekAverage) dailyAverages else usedByApp
    val apps = remember(installed, shown, query) {
        installed.orEmpty()
            .filter { (_, label) -> query.isBlank() || label.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<Pair<String, String>> { shown[it.first] ?: 0L }.thenBy { it.second.lowercase() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
    ) {
        ScreenHeader(title = "Add app limit", onBack = onBack)
        FocusTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search apps",
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )
        SegmentedToggle(
            options = listOf("Today", "7-day average"),
            selectedIndex = if (weekAverage) 1 else 0,
            onSelect = { weekAverage = it == 1 },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = when {
                installed == null -> "Loading apps…"
                weekAverage -> "Average a day over the last 7 days, highest first"
                else -> "Time used today, highest first"
            },
            color = Slate,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusCard(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(apps, key = { it.first }) { (pkg, _) ->
                PickerRow(
                    packageName = pkg,
                    millis = shown[pkg] ?: 0L,
                    weekAverage = weekAverage,
                    limitMinutes = state.limitFor(pkg)?.minutes,
                    onClick = { editing = pkg }
                )
            }
        }
    }

    editing?.let { pkg ->
        val existing = state.limitFor(pkg)
        LimitDialog(
            packageName = pkg,
            initialMinutes = existing?.minutes ?: LimitLogic.DEFAULT_LIMIT_MINUTES,
            averageMillis = dailyAverages[pkg],
            onSave = { onSetLimit(pkg, it) },
            onRemove = existing?.let { { onRemoveLimit(pkg) } },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun PickerRow(packageName: String, millis: Long, weekAverage: Boolean, limitMinutes: Int?, onClick: () -> Unit) {
    val info = rememberAppInfo(packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = info?.label ?: "", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = when {
                    millis < 60_000L -> if (weekAverage) "Under 1m a day" else "Under 1m today"
                    weekAverage -> "${formatDuration(millis)} a day"
                    else -> "${formatDuration(millis)} today"
                },
                color = Slate,
                fontSize = 12.sp
            )
        }
        if (limitMinutes != null) {
            Text(
                text = "${formatLimitMinutes(limitMinutes)} a day",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Mint)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Forest,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
