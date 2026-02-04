package com.hazron.sequencetimer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hazron.sequencetimer.MainActivity
import com.hazron.sequencetimer.R
import com.hazron.sequencetimer.SequenceTimerApp
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.TimerStateManager
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Timer
import com.hazron.sequencetimer.ui.screens.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground service for running timers in the background.
 * Observes timer states and triggers appropriate notifications on completion.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerStateManager: TimerStateManager

    @Inject
    lateinit var timerRepository: TimerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null
    private val completedTimers = mutableSetOf<Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startObservingTimers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1)
                if (timerId != -1L) {
                    completedTimers.remove(timerId) // Reset completed state for this timer
                }
                startForegroundService()
            }
            ACTION_STOP -> {
                stopObservingTimers()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopObservingTimers()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        val notification = createOngoingNotification("Timer running...")
        startForeground(NOTIFICATION_ID_ONGOING, notification)
    }

    private fun startObservingTimers() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            timerStateManager.timerStates.collectLatest { states ->
                val runningTimers = states.values.filter { it.isRunning || it.isPaused }

                if (runningTimers.isEmpty()) {
                    // No running timers, can stop service after a delay
                    delay(5000)
                    if (timerStateManager.timerStates.value.values.none { it.isRunning || it.isPaused }) {
                        withContext(Dispatchers.Main) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                } else {
                    // Update ongoing notification with timer info
                    val activeTimer = runningTimers.firstOrNull { it.isRunning }
                    if (activeTimer != null) {
                        val timer = timerRepository.getTimer(activeTimer.timerId)
                        val label = timer?.label ?: "Timer"
                        val minutes = activeTimer.remainingSeconds / 60
                        val seconds = activeTimer.remainingSeconds % 60
                        val timeText = String.format("%d:%02d", minutes, seconds)

                        updateOngoingNotification("$label - $timeText remaining")
                    }
                }

                // Check for completed timers
                states.forEach { (timerId, state) ->
                    if (state.isComplete && !completedTimers.contains(timerId)) {
                        completedTimers.add(timerId)
                        val timer = timerRepository.getTimer(timerId)
                        if (timer != null) {
                            withContext(Dispatchers.Main) {
                                onTimerComplete(timer)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopObservingTimers() {
        observerJob?.cancel()
        observerJob = null
    }

    private fun createOngoingNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SequenceTimerApp.CHANNEL_ONGOING)
            .setContentTitle("Sequence Timer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateOngoingNotification(text: String) {
        try {
            val notification = createOngoingNotification(text)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_ONGOING, notification)
        } catch (e: SecurityException) {
            // Permission not granted, ignore
        }
    }

    private fun onTimerComplete(timer: Timer) {
        when (timer.notificationType) {
            NotificationType.SILENT -> showSilentNotification(timer)
            NotificationType.SOUND -> showSoundNotification(timer)
            NotificationType.ALARM -> showAlarmNotification(timer)
        }
    }

    private fun showSilentNotification(timer: Timer) {
        val notification = NotificationCompat.Builder(this, SequenceTimerApp.CHANNEL_SILENT)
            .setContentTitle("Timer Complete")
            .setContentText("${timer.label} has finished")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_COMPLETE_BASE + timer.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun showSoundNotification(timer: Timer) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, SequenceTimerApp.CHANNEL_SOUND)
            .setContentTitle("Timer Complete")
            .setContentText("${timer.label} has finished")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_COMPLETE_BASE + timer.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }

        // Also vibrate
        vibrate()
    }

    private fun showAlarmNotification(timer: Timer) {
        // Launch full-screen alarm activity
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_TIMER_LABEL, timer.label)
            putExtra(AlarmActivity.EXTRA_TIMER_ID, timer.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, timer.id.toInt(), alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SequenceTimerApp.CHANNEL_ALARM)
            .setContentTitle("Timer Alarm!")
            .setContentText("${timer.label} has finished")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_ALARM_BASE + timer.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted, try to start activity directly
        }

        // Start alarm activity directly as well
        startActivity(alarmIntent)
    }

    private fun vibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    companion object {
        const val ACTION_START = "com.hazron.sequencetimer.START"
        const val ACTION_STOP = "com.hazron.sequencetimer.STOP"
        const val EXTRA_TIMER_ID = "timer_id"

        private const val NOTIFICATION_ID_ONGOING = 1001
        private const val NOTIFICATION_ID_COMPLETE_BASE = 2000
        private const val NOTIFICATION_ID_ALARM_BASE = 3000

        fun startService(context: Context, timerId: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
