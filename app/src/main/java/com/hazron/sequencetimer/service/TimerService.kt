package com.hazron.sequencetimer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hazron.sequencetimer.MainActivity
import com.hazron.sequencetimer.R
import com.hazron.sequencetimer.SequenceTimerApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for running timers in the background.
 * TODO: Implement full timer management in Phase 2
 */
@AndroidEntryPoint
class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SequenceTimerApp.CHANNEL_ONGOING)
            .setContentTitle("Timer Running")
            .setContentText("Tap to view active timers")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.hazron.sequencetimer.START"
        const val ACTION_STOP = "com.hazron.sequencetimer.STOP"
        private const val NOTIFICATION_ID = 1001
    }
}
