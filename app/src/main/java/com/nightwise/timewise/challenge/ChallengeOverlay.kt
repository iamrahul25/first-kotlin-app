package com.nightwise.timewise.challenge

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import com.nightwise.timewise.MainActivity
import com.nightwise.timewise.R
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.challenge.ActiveChallenge
import com.nightwise.timewise.data.challenge.ChallengeLogic
import com.nightwise.timewise.data.challenge.ChallengeRepository
import com.nightwise.timewise.ui.formatCountdown
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import kotlin.math.abs

/**
 * A small floating pill over every app while a challenge runs: time left, and time spent in
 * other apps against the grace allowed. It's an accessibility overlay, so it needs no extra
 * permission. Drag it anywhere; tap it to open TimeWise. Use from the main thread only.
 */
class ChallengeOverlay(private val service: AccessibilityService, private val worker: ExecutorService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val prefs = service.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val challenges = ChallengeRepository.get(service)
    private val focus = FocusRepository.get(service)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val density = service.resources.displayMetrics.density

    private var view: LinearLayout? = null
    private lateinit var title: TextView
    private lateinit var detail: TextView
    private lateinit var params: WindowManager.LayoutParams

    private var active: ActiveChallenge? = null
    private var enabled = true
    private var overTimeWise = false
    private var distractionMillis: Long? = null
    private var lastProgressAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            val current = active ?: return
            val now = System.currentTimeMillis()
            if (current.remainingMillis(now) == 0L) {
                sync()
                return
            }
            render(current, now)
            if (now - lastProgressAt >= PROGRESS_EVERY_MS) refreshProgress(current, now)
            handler.postDelayed(this, TICK_MS)
        }
    }

    fun start() {
        scope.launch {
            combine(challenges.state, focus.settings) { state, settings -> state.active to settings.floatingTimerEnabled }
                .collect { (challenge, on) ->
                    if (challenge != active) {
                        distractionMillis = null
                        lastProgressAt = 0L
                    }
                    active = challenge
                    enabled = on
                    sync()
                }
        }
    }

    fun stop() {
        scope.cancel()
        handler.removeCallbacks(tick)
        remove()
    }

    /** TimeWise shows the timer itself, so the pill steps aside while it's in front. */
    fun onForegroundChanged(packageName: String?) {
        overTimeWise = packageName == service.packageName
        sync()
    }

    private fun sync() {
        handler.removeCallbacks(tick)
        val current = active
        val visible = current != null && enabled && !overTimeWise && current.remainingMillis(System.currentTimeMillis()) > 0
        if (!visible) {
            remove()
            return
        }
        if (view == null) add()
        tick.run()
    }

    /** Reads time in other apps off the main thread; ends the challenge as soon as the grace is used up. */
    private fun refreshProgress(current: ActiveChallenge, now: Long) {
        lastProgressAt = now
        if (worker.isShutdown) return
        worker.execute {
            val progress = challenges.progress(current, System.currentTimeMillis())
            if (!ChallengeLogic.isSuccess(current.type, progress.distractionMillis)) {
                challenges.evaluate()?.let { outcome ->
                    ChallengeScheduler.cancel(service)
                    ChallengeScheduler.notifyOutcome(service, outcome)
                }
            }
            handler.post {
                if (active == current) {
                    distractionMillis = progress.distractionMillis
                    render(current, System.currentTimeMillis())
                }
            }
        }
    }

    private fun render(current: ActiveChallenge, now: Long) {
        if (view == null) return
        val grace = current.type.graceMillis
        val used = distractionMillis
        title.text = "${current.type.label} · ${formatCountdown(current.remainingMillis(now))} left"
        detail.text = if (used == null) {
            "Other apps: checking…"
        } else {
            "Other apps ${formatDuration(used)} of ${formatLimitMinutes(current.type.graceMinutes)}"
        }
        val color = when {
            used == null || used < grace * WARN_FRACTION -> FOREST
            used <= grace -> AMBER
            else -> RED
        }
        (view?.background as? GradientDrawable)?.setColor(color)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun add() {
        val semibold = ResourcesCompat.getFont(service, R.font.inter_semibold)
        val regular = ResourcesCompat.getFont(service, R.font.inter_medium)
        title = TextView(service).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = semibold
        }
        detail = TextView(service).apply {
            setTextColor(0xD9FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = regular
        }
        val pill = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(7), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(FOREST)
            }
            elevation = dp(6).toFloat()
            addView(title)
            addView(detail)
            contentDescription = "Focus challenge timer. Tap to open TimeWise."
            setOnClickListener {
                service.startActivity(Intent(service, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, dp(DEFAULT_X_DP))
            y = prefs.getInt(KEY_Y, dp(DEFAULT_Y_DP))
        }
        pill.setOnTouchListener(DragListener())
        runCatching { windowManager.addView(pill, params) }.onSuccess { view = pill }
    }

    private fun remove() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        view = null
    }

    /** Moves the pill with the finger; a touch that barely moves is a tap. */
    private inner class DragListener : android.view.View.OnTouchListener {
        private val slop = ViewConfiguration.get(service).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).toInt().coerceAtLeast(0)
                        params.y = (startY + dy).toInt().coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        prefs.edit {
                            putInt(KEY_X, params.x)
                            putInt(KEY_Y, params.y)
                        }
                    } else {
                        v.performClick()
                    }
                }
            }
            return true
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    companion object {
        private const val PREFS_NAME = "timewise_overlay"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val DEFAULT_X_DP = 12
        private const val DEFAULT_Y_DP = 96
        private const val TICK_MS = 1_000L
        private const val PROGRESS_EVERY_MS = 20_000L
        private const val WARN_FRACTION = 0.8
        private const val FOREST = 0xEB2F6B4F.toInt()
        private const val AMBER = 0xEBB45309.toInt()
        private const val RED = 0xEBB42318.toInt()
    }
}
