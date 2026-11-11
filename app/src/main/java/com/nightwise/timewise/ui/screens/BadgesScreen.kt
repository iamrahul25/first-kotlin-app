package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.badges.BadgeEngine
import com.nightwise.timewise.data.badges.BadgeKind
import com.nightwise.timewise.data.badges.BadgeProgress
import com.nightwise.timewise.data.badges.BadgeRecord
import com.nightwise.timewise.data.badges.BadgeTier
import com.nightwise.timewise.data.badges.Measure
import com.nightwise.timewise.data.badges.ProgressScope
import com.nightwise.timewise.data.badges.UsageBadge
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.components.BadgeCountChip
import com.nightwise.timewise.ui.components.BadgeMedal
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.tierStyle
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import java.util.Calendar

private const val GRID_COLUMNS = 3
private const val RECENT_DAYS = 30
private const val DAY_MS = 24L * 60 * 60 * 1000

/** Recent form from the tier-weighted count of warnings in the last 30 days. */
private enum class RecentForm(val label: String, val minScore: Int, val message: String) {
    CLEAN("Clean", 0, "No warnings in the last 30 days. Keep this wall empty."),
    SLIPPING("Slipping", 1, "A few bad days lately. Catch it before it becomes a habit."),
    SLIDING("Sliding", 6, "Your phone is winning most days. Pick one warning and beat it this week."),
    SPIRALING("Spiraling", 16, "The screen is eating your days. Start with a 1-hour focus challenge today.")
}

@Composable
fun BadgesScreen(
    usage: UsageSnapshot?,
    records: Map<UsageBadge, BadgeRecord>,
    onSeen: () -> Unit,
    onBack: () -> Unit
) {
    var kind by rememberSaveable { mutableStateOf(BadgeKind.ACHIEVEMENT) }
    var selected by remember { mutableStateOf<UsageBadge?>(null) }
    val newOnOpen = remember { records.filterValues { it.unseen > 0 }.keys }
    LaunchedEffect(Unit) { onSeen() }

    val today = usage?.today?.epochDay
    val firstDayOfWeek = remember { Calendar.getInstance().firstDayOfWeek }
    val progress = remember(usage) {
        if (usage == null || today == null) emptyMap()
        else UsageBadge.entries.associateWith { BadgeEngine.progress(it, usage.history, today, firstDayOfWeek) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Badges", onBack = onBack)
        KindToggle(selected = kind, records = records, onSelect = { kind = it })

        val badges = UsageBadge.entries.filter { it.kind == kind }
        if (kind == BadgeKind.ACHIEVEMENT) {
            AchievementsHero(badges, records, progress)
        } else {
            ShameHero(badges, records, usage)
        }

        BadgeTier.entries.filter { it.kind == kind }.forEach { tier ->
            val inTier = badges.filter { it.tier == tier }
            TierSection(
                tier = tier,
                badges = inTier,
                records = records,
                progress = progress,
                newBadges = newOnOpen,
                onSelect = { selected = it }
            )
        }

        Text(
            text = "Days count once they're over. Warnings count the moment you cross the line. " +
                "Weeks and months need every day on record.",
            color = Mist,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    selected?.let { badge ->
        BadgeDetailSheet(
            badge = badge,
            record = records[badge] ?: BadgeRecord(),
            progress = progress[badge],
            today = today,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun KindToggle(selected: BadgeKind, records: Map<UsageBadge, BadgeRecord>, onSelect: (BadgeKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeutralChip)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BadgeKind.entries.forEach { kind ->
            val isSelected = kind == selected
            val earned = UsageBadge.entries.count { it.kind == kind && records[it]?.earned == true }
            val label = if (kind == BadgeKind.ACHIEVEMENT) "Achievements" else "Hall of Shame"
            Text(
                text = "$label · $earned",
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) CardWhite else NeutralChip)
                    .clickable { onSelect(kind) }
                    .padding(vertical = 8.dp),
                color = when {
                    !isSelected -> Slate
                    kind == BadgeKind.WARNING -> Red
                    else -> Forest
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AchievementsHero(
    badges: List<UsageBadge>,
    records: Map<UsageBadge, BadgeRecord>,
    progress: Map<UsageBadge, BadgeProgress?>
) {
    val unlocked = badges.count { records[it]?.earned == true }
    val totalWins = badges.sumOf { records[it]?.count ?: 0 }
    val nextUp = badges.firstOrNull { records[it]?.earned != true && progress[it]?.let { p -> isOnTrack(it, p) } == true }
        ?: badges.firstOrNull { records[it]?.earned != true }

    HeroCard(
        brush = Brush.linearGradient(listOf(ForestDeep, Forest)),
        eyebrow = "ACHIEVEMENTS",
        headline = "$unlocked / ${badges.size}",
        caption = "unlocked",
        fraction = unlocked.toFloat() / badges.size,
        barColor = Color(0xFFFFDE73),
        stats = listOf("Total wins" to totalWins.toString(), "Legendary" to badges.count { it.tier == BadgeTier.LEGENDARY && records[it]?.earned == true }.toString())
    ) {
        nextUp?.let { badge ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeMedal(badge = badge, earned = true, size = 36.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Next up: ${badge.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = progress[badge]?.let { progressLabel(badge, it) } ?: badge.description,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShameHero(badges: List<UsageBadge>, records: Map<UsageBadge, BadgeRecord>, usage: UsageSnapshot?) {
    val collected = badges.count { records[it]?.earned == true }
    val today = usage?.today?.epochDay
    val score = if (today == null) 0 else badges.sumOf { badge ->
        val recent = records[badge]?.periods.orEmpty().count { it > today - RECENT_DAYS }
        recent * when (badge.tier) {
            BadgeTier.EXTREME -> 6
            BadgeTier.SEVERE -> 3
            else -> 1
        }
    }
    val level = RecentForm.entries.last { score >= it.minScore }
    val last30 = usage?.last30Days?.sumOf { it.totalMillis } ?: 0L

    HeroCard(
        brush = Brush.linearGradient(listOf(Color(0xFF1C0A0A), Color(0xFF7F1D1D))),
        eyebrow = "HALL OF SHAME",
        headline = "$collected / ${badges.size}",
        caption = "collected",
        fraction = collected.toFloat() / badges.size,
        barColor = Color(0xFFF87171),
        stats = listOf("Recent form" to level.label, "Last 30 days" to formatDuration(last30))
    ) {
        Text(
            text = buildString {
                append(level.message)
                val fullDays = last30 / DAY_MS
                if (fullDays >= 1) append(" That's $fullDays full ${if (fullDays == 1L) "day" else "days"} of your life on a screen this month.")
            },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun HeroCard(
    brush: Brush,
    eyebrow: String,
    headline: String,
    caption: String,
    fraction: Float,
    barColor: Color,
    stats: List<Pair<String, String>>,
    footer: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = eyebrow, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = headline, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(text = caption, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        ProgressBar(fraction = fraction, color = barColor, track = Color.White.copy(alpha = 0.18f), height = 8)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            stats.forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        footer()
    }
}

@Composable
private fun TierSection(
    tier: BadgeTier,
    badges: List<UsageBadge>,
    records: Map<UsageBadge, BadgeRecord>,
    progress: Map<UsageBadge, BadgeProgress?>,
    newBadges: Set<UsageBadge>,
    onSelect: (UsageBadge) -> Unit
) {
    val style = tierStyle(tier)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Brush.linearGradient(listOf(style.light, style.dark)), RoundedCornerShape(5.dp))
            )
            Text(text = tier.label, color = style.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                text = "${badges.count { records[it]?.earned == true }} of ${badges.size}",
                modifier = Modifier
                    .background(style.tint, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = style.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        badges.chunked(GRID_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { badge ->
                    BadgeTile(
                        badge = badge,
                        record = records[badge] ?: BadgeRecord(),
                        progress = progress[badge],
                        isNew = badge in newBadges,
                        onClick = { onSelect(badge) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(GRID_COLUMNS - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BadgeTile(
    badge: UsageBadge,
    record: BadgeRecord,
    progress: BadgeProgress?,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = tierStyle(badge.tier)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusCard(background = if (record.earned) style.tint else CardWhite, onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(contentAlignment = Alignment.BottomCenter) {
                BadgeMedal(badge = badge, earned = record.earned, size = 56.dp, highlight = isNew, modifier = Modifier.padding(bottom = 8.dp))
                if (record.earned) BadgeCountChip(badge = badge, count = record.count)
            }
            if (isNew) {
                Text(
                    text = "NEW",
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-4).dp)
                        .background(if (badge.kind == BadgeKind.WARNING) Red else Forest, RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = badge.title,
            color = if (record.earned) Ink else Slate,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            maxLines = 2
        )
        Text(
            text = when {
                record.earned -> if (record.count == 1) "Earned once" else "Earned ${record.count} times"
                badge.kind == BadgeKind.WARNING -> "Avoided"
                else -> "Locked"
            },
            color = when {
                record.earned -> style.text
                badge.kind == BadgeKind.WARNING -> Forest
                else -> Mist
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Box(modifier = Modifier.weight(1f))
        if (progress != null) {
            ProgressBar(
                fraction = barFraction(progress),
                color = barColor(badge, progress),
                track = StepperBg,
                height = 4
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeDetailSheet(
    badge: UsageBadge,
    record: BadgeRecord,
    progress: BadgeProgress?,
    today: Long?,
    onDismiss: () -> Unit
) {
    val style = tierStyle(badge.tier)
    val isWarning = badge.kind == BadgeKind.WARNING
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeMedal(badge = badge, earned = record.earned, size = 104.dp)
            Text(
                text = "${badge.tier.label} ${if (isWarning) "warning" else "achievement"}".uppercase(),
                modifier = Modifier
                    .background(style.tint, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                color = style.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(text = badge.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = badge.description, color = Slate, fontSize = 14.sp, textAlign = TextAlign.Center)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailStat(
                    label = if (isWarning) "Times hit" else "Times earned",
                    value = record.count.toString(),
                    modifier = Modifier.weight(1f)
                )
                DetailStat(
                    label = "Last time",
                    value = record.lastEarnedDay?.let { relativeDayLabel(it, today ?: it) } ?: if (isWarning) "Never" else "Not yet",
                    modifier = Modifier.weight(1f)
                )
            }

            if (progress != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StepperBg)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = progressLabel(badge, progress), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(text = statusLabel(badge, progress), color = barColor(badge, progress), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    ProgressBar(fraction = barFraction(progress), color = barColor(badge, progress), track = CardWhite, height = 8)
                }
            }

            Text(
                text = encouragement(badge, record),
                color = Mist,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(StepperBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, track: Color, height: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape((height / 2).dp))
                .background(color)
        )
    }
}

/** Under-limit achievements are on track while there's budget left; streaks always build up. */
private fun isOnTrack(badge: UsageBadge, progress: BadgeProgress): Boolean = when {
    badge.kind == BadgeKind.WARNING -> progress.current < progress.target
    progress.scope == ProgressScope.STREAK -> true
    else -> progress.current < progress.target
}

private fun barFraction(progress: BadgeProgress): Float =
    if (progress.target <= 0) 0f else progress.current.toFloat() / progress.target

private fun barColor(badge: UsageBadge, progress: BadgeProgress): Color = when {
    progress.scope == ProgressScope.STREAK && badge.kind == BadgeKind.ACHIEVEMENT -> tierStyle(badge.tier).dark
    !isOnTrack(badge, progress) -> Red
    badge.kind == BadgeKind.WARNING -> if (barFraction(progress) >= 0.75f) Color(0xFFEA580C) else Forest
    barFraction(progress) >= 0.85f -> Color(0xFFEA580C)
    else -> Forest
}

private fun progressLabel(badge: UsageBadge, progress: BadgeProgress): String {
    if (progress.scope == ProgressScope.STREAK) return "Streak: ${progress.current} of ${progress.target} days"
    val format: (Long) -> String = if (badge.rule.measure == Measure.UNLOCKS) { n -> "$n unlocks" } else ::formatLimit
    val scope = when (progress.scope) {
        ProgressScope.TODAY -> "Today"
        ProgressScope.THIS_WEEK -> "This week"
        else -> "This month"
    }
    val joiner = if (badge.kind == BadgeKind.ACHIEVEMENT) "of" else "/"
    return "$scope: ${format(progress.current)} $joiner ${format(progress.target)}"
}

private fun statusLabel(badge: UsageBadge, progress: BadgeProgress): String = when {
    badge.kind == BadgeKind.WARNING -> if (isOnTrack(badge, progress)) "Safe" else "Triggered"
    progress.scope == ProgressScope.STREAK -> if (progress.current == 0L) "Start today" else "Keep going"
    isOnTrack(badge, progress) -> "On track"
    else -> "Missed this time"
}

private fun encouragement(badge: UsageBadge, record: BadgeRecord): String = when {
    badge.kind == BadgeKind.WARNING && record.earned -> "Every one of these is time the screen took from you. Make this the last."
    badge.kind == BadgeKind.WARNING -> "You've stayed clear of this one. Keep it that way."
    record.earned -> "Earn it again to grow your count."
    badge.tier == BadgeTier.LEGENDARY -> "Few people ever earn this one. Will you?"
    else -> "Locked. Hit the goal to unlock it."
}

/** "30h", "1h 30m", "45m". */
private fun formatLimit(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes >= 60 && minutes % 60 == 0L) "${minutes / 60}h" else formatDuration(millis)
}
