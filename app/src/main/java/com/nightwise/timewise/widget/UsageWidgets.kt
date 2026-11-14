package com.nightwise.timewise.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.nightwise.timewise.R
import com.nightwise.timewise.data.profile.ProfileLogic
import com.nightwise.timewise.data.profile.UsageProfile
import com.nightwise.timewise.data.usage.AppInfoCache
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import com.nightwise.timewise.permissions.PermissionUtils
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.Trend
import com.nightwise.timewise.ui.components.profileStyle
import com.nightwise.timewise.ui.formatDuration
import java.util.concurrent.Executors

/** What the home-screen widgets show; a smaller widget just uses less of it. */
private data class WidgetData(
    val hasAccess: Boolean,
    val todayMillis: Long = 0L,
    val yesterdayMillis: Long = 0L,
    val unlocks: Int? = null,
    val topApps: List<AppUsage> = emptyList()
) {
    val level: UsageProfile get() = ProfileLogic.profileFor(todayMillis)
}

private enum class WidgetKind(val provider: Class<out AppWidgetProvider>, val layout: Int) {
    TINY(TinyUsageWidget::class.java, R.layout.widget_tiny),
    SMALL(SmallUsageWidget::class.java, R.layout.widget_small),
    MEDIUM(MediumUsageWidget::class.java, R.layout.widget_medium)
}

object UsageWidgets {

    private const val ACTION_REFRESH = "com.nightwise.timewise.action.REFRESH_WIDGETS"
    private const val REQUEST_CODE = 7004
    /** Same 0–10h scale as the meter on the home screen. */
    private const val METER_MAX_MS = 10 * 60 * 60 * 1000L
    private const val CACHE_MS = 5_000L
    private const val TOP_APPS = 3

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var cached: Pair<Long, WidgetData>? = null

    /** Redraws every placed widget off the main thread; cheap when none are placed. */
    fun refresh(context: Context, pending: BroadcastReceiver.PendingResult? = null) {
        val appContext = context.applicationContext
        executor.execute {
            try {
                runCatching { updateAll(appContext) }
            } finally {
                pending?.finish()
            }
        }
    }

    /** Keeps widgets fresh between app opens. Not a wake-up alarm, so it only runs while the phone is in use. */
    fun scheduleRefresh(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            refreshIntent(context)
        )
    }

    fun cancelRefreshIfUnused(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (WidgetKind.entries.any { ids(context, manager, it).isNotEmpty() }) return
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ids(context: Context, manager: AppWidgetManager, kind: WidgetKind): IntArray =
        manager.getAppWidgetIds(ComponentName(context, kind.provider))

    private fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val placed = WidgetKind.entries.associateWith { ids(context, manager, it) }
        if (placed.values.all { it.isEmpty() }) return
        val data = load(context)
        placed.forEach { (kind, widgetIds) ->
            widgetIds.forEach { id ->
                val views = RemoteViews(context.packageName, kind.layout)
                val options = manager.getAppWidgetOptions(id)
                when (kind) {
                    WidgetKind.TINY -> bindTiny(context, views, data, options)
                    WidgetKind.SMALL -> bindCard(context, views, data, options, columns = 1)
                    WidgetKind.MEDIUM -> {
                        bindCard(context, views, data, options, columns = 2)
                        bindApps(context, views, data)
                    }
                }
                views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
                manager.updateAppWidget(id, views)
            }
        }
    }

    private fun load(context: Context): WidgetData {
        val now = System.currentTimeMillis()
        cached?.let { (at, data) -> if (now - at in 0..CACHE_MS) return data }
        val data = if (!PermissionUtils.hasUsageAccess(context)) {
            WidgetData(hasAccess = false)
        } else {
            val usage = ScreenTimeRepository(context).load(now)
            WidgetData(
                hasAccess = true,
                todayMillis = usage.today.totalMillis,
                yesterdayMillis = usage.yesterday.totalMillis,
                unlocks = usage.today.unlocks,
                topApps = usage.today.apps.take(TOP_APPS)
            )
        }
        cached = now to data
        return data
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        context.packageManager.getLaunchIntentForPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ---- Tiny: a ring that fills over the day, with the time in the middle ----

    private fun bindTiny(context: Context, views: RemoteViews, data: WidgetData, options: Bundle) {
        val sizeDp = minOf(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        ).takeIf { it > 0 }?.coerceIn(48, 160) ?: 72
        views.setImageViewBitmap(R.id.widget_ring, ringBitmap(context, sizeDp, data))

        val totalMinutes = data.todayMillis / 60_000
        val (big, small) = when {
            !data.hasAccess -> "—" to "set up"
            totalMinutes >= 60 -> "${totalMinutes / 60}h" to "${totalMinutes % 60}m"
            else -> "$totalMinutes" to "min"
        }
        views.setTextViewText(R.id.widget_time, big)
        views.setTextViewText(R.id.widget_time_sub, small)
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_DIP, sizeDp * 0.25f)
        views.setTextViewTextSize(R.id.widget_time_sub, TypedValue.COMPLEX_UNIT_DIP, sizeDp * 0.15f)
    }

    private fun ringBitmap(context: Context, sizeDp: Int, data: WidgetData): Bitmap {
        val size = (sizeDp * context.resources.displayMetrics.density).toInt()
        val s = size.toFloat()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(0f, 0f, s, s, 0xFF2A6249.toInt(), 0xFF153628.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(s / 2, s / 2, s / 2, paint)
        paint.shader = null

        val stroke = s * 0.075f
        val inset = s * 0.085f + stroke / 2
        val arc = RectF(inset, inset, s - inset, s - inset)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = 0x26FFFFFF
        canvas.drawArc(arc, 0f, 360f, false, paint)
        if (data.hasAccess && data.todayMillis > 0) {
            val fraction = (data.todayMillis.toFloat() / METER_MAX_MS).coerceIn(0f, 1f)
            paint.color = profileStyle(data.level).light.toArgb()
            canvas.drawArc(arc, -90f, (360f * fraction).coerceAtLeast(4f), false, paint)
        }
        return bitmap
    }

    // ---- Small and medium: today's total, level, change and the level meter ----

    private fun bindCard(context: Context, views: RemoteViews, data: WidgetData, options: Bundle, columns: Int) {
        val details = if (data.hasAccess) View.VISIBLE else View.GONE
        views.setViewVisibility(R.id.widget_level, details)
        views.setViewVisibility(R.id.widget_change_row, details)
        views.setViewVisibility(R.id.widget_meter, details)
        if (!data.hasAccess) {
            views.setTextViewText(R.id.widget_time, "—")
            views.setTextViewText(R.id.widget_caption, "Tap to allow usage access")
            return
        }

        val level = data.level
        val levelColor = profileStyle(level).light.toArgb()
        views.setTextViewText(R.id.widget_time, formatDuration(data.todayMillis))
        views.setTextViewText(
            R.id.widget_level,
            SpannableString("●  ${level.title}").apply {
                setSpan(ForegroundColorSpan(levelColor), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )

        val (change, trend) = changeVsPrevious(data.todayMillis, data.yesterdayMillis)
        views.setTextViewText(R.id.widget_change, change)
        views.setTextColor(
            R.id.widget_change,
            when (trend) {
                Trend.Better -> 0xFF8EE3B4.toInt()
                Trend.Worse -> 0xFFFFB3A7.toInt()
                Trend.Neutral -> 0xCCFFFFFF.toInt()
            }
        )

        val next = UsageProfile.entries.getOrNull(level.ordinal + 1)
        views.setTextViewText(
            R.id.widget_caption,
            if (next == null) "Past 8 hours today"
            else "${formatDuration(level.maxDailyMillis - data.todayMillis)} left before ${next.title}"
        )

        val widgetWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            .takeIf { it > 0 } ?: (150 * columns)
        val meterWidthDp = ((widgetWidthDp - CARD_PADDING_DP * 2 - COLUMN_GAP_DP * (columns - 1)) / columns)
            .coerceIn(60, 400)
        views.setImageViewBitmap(R.id.widget_meter, meterBitmap(context, meterWidthDp, data.todayMillis, level))
    }

    /** Five level bands with a marker at today's total, matching the in-app meter. */
    private fun meterBitmap(context: Context, widthDp: Int, todayMillis: Long, level: UsageProfile): Bitmap {
        val d = context.resources.displayMetrics.density
        val w = widthDp * d
        val h = 12 * d
        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val trackHeight = 4 * d
        val top = (h - trackHeight) / 2
        val gap = 2 * d
        val bands = UsageProfile.entries
        val bandWidth = (w - gap * (bands.size - 1)) / bands.size
        canvas.save()
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, top, w, top + trackHeight), trackHeight / 2, trackHeight / 2, Path.Direction.CW)
        })
        bands.forEachIndexed { index, band ->
            paint.color = profileStyle(band).light.toArgb()
            paint.alpha = if (band.ordinal <= level.ordinal) 242 else 77
            val left = index * (bandWidth + gap)
            canvas.drawRect(left, top, left + bandWidth, top + trackHeight, paint)
        }
        canvas.restore()

        val radius = h / 2
        val fraction = (todayMillis.toFloat() / METER_MAX_MS).coerceIn(0f, 1f)
        val cx = radius + (w - h) * fraction
        paint.alpha = 255
        paint.color = profileStyle(level).light.toArgb()
        canvas.drawCircle(cx, radius, radius, paint)
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(cx, radius, radius - 3 * d, paint)
        return bitmap
    }

    // ---- Medium only: most used apps ----

    private val APP_ROWS = listOf(
        intArrayOf(R.id.widget_app1, R.id.widget_app1_icon, R.id.widget_app1_name, R.id.widget_app1_time),
        intArrayOf(R.id.widget_app2, R.id.widget_app2_icon, R.id.widget_app2_name, R.id.widget_app2_time),
        intArrayOf(R.id.widget_app3, R.id.widget_app3_icon, R.id.widget_app3_name, R.id.widget_app3_time)
    )

    private fun bindApps(context: Context, views: RemoteViews, data: WidgetData) {
        views.setTextViewText(
            R.id.widget_unlocks,
            data.unlocks?.takeIf { data.hasAccess }?.let { if (it == 1) "1 unlock" else "$it unlocks" }.orEmpty()
        )
        APP_ROWS.forEachIndexed { index, (row, icon, name, time) ->
            val app = data.topApps.getOrNull(index)
            views.setViewVisibility(row, if (app == null) View.GONE else View.VISIBLE)
            if (app == null) return@forEachIndexed
            val info = AppInfoCache.get(context, app.packageName)
            views.setTextViewText(name, info.label)
            views.setTextViewText(time, formatDuration(app.millis))
            info.icon?.let { views.setImageViewBitmap(icon, it.asAndroidBitmap()) }
                ?: views.setImageViewResource(icon, R.drawable.widget_app_placeholder)
        }
        views.setViewVisibility(R.id.widget_apps_empty, if (data.topApps.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(
            R.id.widget_apps_empty,
            if (data.hasAccess) "No apps used yet today" else "Your top apps show up here"
        )
    }

    private const val CARD_PADDING_DP = 14
    private const val COLUMN_GAP_DP = 25
}

abstract class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        UsageWidgets.scheduleRefresh(context)
        UsageWidgets.refresh(context, goAsync())
    }

    /** Resizing changes how wide the meter and how big the ring should be drawn. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        UsageWidgets.refresh(context, goAsync())
    }

    override fun onDisabled(context: Context) {
        UsageWidgets.cancelRefreshIfUnused(context)
    }
}

/** 1×1 ring with today's screen time. */
class TinyUsageWidget : UsageWidgetProvider()

/** 2×2 card: today's total, level, change vs yesterday and the level meter. */
class SmallUsageWidget : UsageWidgetProvider()

/** 4×2 card: everything in the small one plus unlocks and the most used apps. */
class MediumUsageWidget : UsageWidgetProvider()

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UsageWidgets.refresh(context, goAsync())
    }
}
