package com.nunna.fitnessgym.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nunna.fitnessgym.MainActivity
import com.nunna.fitnessgym.R
import com.nunna.fitnessgym.data.AppClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RestState(val endsAt: Long, val totalSec: Int, val next: String?)

/**
 * Rest timer. The in-app banner reads [state]; a foreground service keeps a countdown notification on
 * the lock screen and buzzes when rest is over, even if the phone is locked or the app is in the background.
 */
object RestTimer {
    private val _state = MutableStateFlow<RestState?>(null)
    val state: StateFlow<RestState?> = _state

    /** Tests turn this off so no Android service is started. */
    @Volatile var useService = true

    fun start(ctx: Context, seconds: Int, next: String?) {
        if (seconds <= 0) { stop(ctx); return }
        _state.value = RestState(AppClock.now() + seconds * 1000L, seconds, next)
        sync(ctx)
    }

    fun add(ctx: Context, seconds: Int) {
        val s = _state.value ?: return
        val left = (s.endsAt - AppClock.now()).coerceAtLeast(0)
        if (left + seconds * 1000L <= 0) { stop(ctx); return }
        _state.value = s.copy(endsAt = AppClock.now() + left + seconds * 1000L, totalSec = (s.totalSec + seconds).coerceAtLeast(1))
        sync(ctx)
    }

    fun stop(ctx: Context) {
        _state.value = null
        if (useService) runCatching { ctx.stopService(Intent(ctx, RestTimerService::class.java)) }
    }

    fun remainingSec(): Int = _state.value?.let { (((it.endsAt - AppClock.now()) + 999) / 1000).toInt().coerceAtLeast(0) } ?: 0

    internal fun finishedFromService() { _state.value = null }

    private fun sync(ctx: Context) {
        if (!useService) return
        runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, RestTimerService::class.java)) }
    }
}

class RestTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val done = Runnable { onRestOver() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val st = RestTimer.state.value ?: run { stopSelf(); return START_NOT_STICKY }
        ensureChannels(this)
        val n = countdown(st)
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIF_ID, n)
        } catch (e: Exception) {
            // TMR-001: Android refused the foreground timer. The in-app countdown still works.
            stopSelf(); return START_NOT_STICKY
        }
        handler.removeCallbacks(done)
        handler.postDelayed(done, (st.endsAt - AppClock.now()).coerceAtLeast(0))
        return START_NOT_STICKY
    }

    private fun countdown(st: RestState): Notification {
        val left = (st.endsAt - AppClock.now()).coerceAtLeast(0)
        return NotificationCompat.Builder(this, CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Resting")
            .setContentText(st.next?.let { "Next: $it" } ?: "Next set coming up")
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + left)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(this))
            .build()
    }

    private fun onRestOver() {
        val next = RestTimer.state.value?.next
        RestTimer.finishedFromService()
        buzz()
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val n = NotificationCompat.Builder(this, CH_DONE)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("Rest over. Time to lift")
                .setContentText(next?.let { "Next: $it" } ?: "Next set")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).setTimeoutAfter(60_000)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openApp(this))
                .build()
            runCatching { NotificationManagerCompat.from(this).notify(NOTIF_DONE_ID, n) }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buzz() {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as? Vibrator)
        runCatching { v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250, 120, 400), -1)) }
    }

    override fun onDestroy() { handler.removeCallbacks(done); super.onDestroy() }

    companion object {
        const val CH_TIMER = "rest_timer"
        const val CH_DONE = "rest_done"
        const val NOTIF_ID = 41
        const val NOTIF_DONE_ID = 42

        fun ensureChannels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Rest timer", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_DONE, "Rest over", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) })
        }

        private fun openApp(ctx: Context) = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
