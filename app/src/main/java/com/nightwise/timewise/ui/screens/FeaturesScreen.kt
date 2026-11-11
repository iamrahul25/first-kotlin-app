package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Blue
import com.nightwise.timewise.ui.theme.BlueTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.RedTint
import com.nightwise.timewise.ui.theme.Slate

private data class Feature(val title: String, val description: String, /** Where to find it in the app. */ val where: String)

private data class FeatureGroup(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
    val features: List<Feature>
)

/** Everything the app does. Add new features here as they ship. */
private val FeatureGroups = listOf(
    FeatureGroup(
        "Screen time", Icons.Rounded.PhoneAndroid, Forest, Mint, listOf(
            Feature("Today's screen time", "Total time today vs yesterday, with a small meter showing today's level and time left before the next one.", "Home"),
            Feature("Most used apps", "Every app you used, ranked, with icons and time.", "Home · Stats"),
            Feature("Usage by category", "Social, video & music, games, browsing, productivity: today, 7 or 30 days. Move any app to another category.", "Home"),
            Feature("Unlocks", "How many times you unlocked your phone (Android 9+).", "Home · Stats"),
            Feature("Hourly timeline", "When in the day you used your phone, with the busiest hour.", "Home · Stats"),
            Feature("Week & month charts", "Daily bars for 7 or 30 days. Tap a day to see its apps.", "Stats"),
            Feature("Monthly trend", "Average screen time a day, month by month.", "Stats > Month")
        )
    ),
    FeatureGroup(
        "App insights", Icons.Rounded.Leaderboard, Red, RedTint, listOf(
            Feature("Top time-wasters", "Apps ranked by time for the week, month or all time, with trends.", "Stats > App insights"),
            Feature("App detail", "For any app: time lost so far, today, week, 30 days, daily average and a daily graph.", "Tap any app")
        )
    ),
    FeatureGroup(
        "Profile & progress", Icons.Rounded.Person, Purple, PurpleTint, listOf(
            Feature("Addiction level", "Monk, Mindful, Hooked, Addict or Zombie, from your 7-day average.", "Home · Settings"),
            Feature("Level up", "How much less a day takes you to the next level, and what your habit costs a year.", "Profile"),
            Feature("Is TimeWise helping?", "Your usage now against the week before you installed, with time saved.", "Profile"),
            Feature("Milestones", "When you installed TimeWise and turned on each permission.", "Profile")
        )
    ),
    FeatureGroup(
        "Badges", Icons.Rounded.EmojiEvents, Amber, AmberTint, listOf(
            Feature("Hall of Fame", "15 badges from Bronze to Legendary, like Zen Master and Digital Monk.", "Badges"),
            Feature("Hall of Shame", "11 warning badges, like Screen Addict and Doom Spiral, plus your recent form.", "Badges"),
            Feature("Win counts & progress", "How many times you've earned each badge, and how close you are today.", "Badges")
        )
    ),
    FeatureGroup(
        "Focus", Icons.Rounded.CenterFocusStrong, Forest, Mint, listOf(
            Feature("Focus challenges", "1-hour focus (5m grace), 8-hour focus (30m grace) and a 1-day detox (1h grace).", "Focus"),
            Feature("Time left", "A live countdown for the running challenge on Focus, Home and the notification shade, plus a floating timer over other apps.", "Focus · Home"),
            Feature("Streaks & focus badges", "Daily streaks and badges for finished challenges.", "Focus"),
            Feature("Challenge alerts", "A notification when your challenge ends or fails.", "Focus"),
            Feature("App limits", "A daily time limit per app. When it runs out, the app closes.", "Focus"),
            Feature("Earn 15 more minutes", "Type a phrase, see your people, type a dream, read the real cost, pick a to-do and listen to your music first.", "Focus"),
            Feature("The real cost", "Facts, quotes and what cutting back gives you, with your own numbers.", "Focus")
        )
    ),
    FeatureGroup(
        "Bedtime & sleep", Icons.Rounded.Bedtime, Purple, PurpleTint, listOf(
            Feature("Bedtime reminder", "Pick a bedtime and get a reminder before it.", "Bedtime"),
            Feature("Sleep log", "Worked out from when you put your phone down and picked it up (Android 9+).", "Bedtime")
        )
    ),
    FeatureGroup(
        "Reports", Icons.Rounded.Summarize, Blue, BlueTint, listOf(
            Feature("Weekly & monthly reports", "A recap of your screen time, top apps, focus wins and sleep.", "Stats > Reports"),
            Feature("Share as image", "Send your report to friends as a picture.", "Stats > Reports")
        )
    ),
    FeatureGroup(
        "Privacy & history", Icons.Rounded.Shield, Slate, NeutralChip, listOf(
            Feature("Stays on your phone", "Everything is worked out on the device. Nothing is uploaded.", "Everywhere"),
            Feature("Nightly save", "Saves each day after midnight so you keep a year of history.", "Settings")
        )
    )
)

/** Count shown in Settings. */
val featureCount: Int get() = FeatureGroups.sumOf { it.features.size }

@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "All features", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(ForestDeep, Forest)))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "EVERYTHING TIMEWISE DOES", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(text = "$featureCount features", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "In ${FeatureGroups.size} areas, all built to help you spend less time on your phone.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        FeatureGroups.forEach { group -> GroupCard(group) }
    }
}

@Composable
private fun GroupCard(group: FeatureGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = group.icon, tint = group.tint, background = group.background, size = 34.dp, iconSize = 19.dp)
            Text(text = group.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                text = group.features.size.toString(),
                modifier = Modifier
                    .background(group.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = group.tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        group.features.forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(6.dp)
                        .background(group.tint, CircleShape)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = feature.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            text = feature.where,
                            modifier = Modifier
                                .background(NeutralChip, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Slate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Text(text = feature.description, color = Slate, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}
