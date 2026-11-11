package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.usage.DayUsage
import com.nightwise.timewise.ui.components.AppUsageRow
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Slate

/** Every app used on one day, most used first. */
@Composable
fun AppsScreen(day: DayUsage?, epochDay: Long, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
    ) {
        ScreenHeader(title = "Apps · ${relativeDayLabel(epochDay)}", onBack = onBack)
        Text(
            text = day?.let { "${formatDuration(it.totalMillis)} across ${it.apps.size} apps" } ?: "Loading…",
            color = Slate,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )
        val apps = day?.apps.orEmpty()
        val max = apps.firstOrNull()?.millis ?: 1L
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusCard(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppUsageRow(packageName = app.packageName, millis = app.millis, maxMillis = max)
            }
        }
    }
}
