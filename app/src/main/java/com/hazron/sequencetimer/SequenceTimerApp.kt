package com.hazron.sequencetimer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SequenceTimerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Silent notification channel
        val silentChannel = NotificationChannel(
            CHANNEL_SILENT,
            "Timer Complete (Silent)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Silent notifications when a timer completes"
            setSound(null, null)
            enableVibration(false)
        }

        // Sound notification channel
        val soundChannel = NotificationChannel(
            CHANNEL_SOUND,
            "Timer Complete (Sound)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications with sound when a timer completes"
            enableVibration(true)
        }

        // Alarm notification channel (high priority, full-screen intent)
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Timer Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen alarm when a timer completes"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        // Ongoing timer channel
        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING,
            "Active Timers",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows currently running timers"
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannels(
            listOf(silentChannel, soundChannel, alarmChannel, ongoingChannel)
        )
    }

    companion object {
        const val CHANNEL_SILENT = "timer_silent"
        const val CHANNEL_SOUND = "timer_sound"
        const val CHANNEL_ALARM = "timer_alarm"
        const val CHANNEL_ONGOING = "timer_ongoing"
    }
}
