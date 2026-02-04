package com.hazron.sequencetimer.ui.screens.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.hazron.sequencetimer.ui.theme.SequenceTimerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen alarm activity shown when a timer with ALARM notification type completes.
 * Plays alarm sound, vibrates, and keeps screen on until dismissed.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val timerLabel = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1)

        // Start alarm sound and vibration
        startAlarm()

        setContent {
            SequenceTimerTheme {
                AlarmScreen(
                    timerLabel = timerLabel,
                    onDismiss = {
                        stopAlarm()
                        // Cancel the alarm notification
                        if (timerId != -1L) {
                            NotificationManagerCompat.from(this)
                                .cancel(NOTIFICATION_ID_ALARM_BASE + timerId.toInt())
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    private fun startAlarm() {
        // Start sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback: try notification sound
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmActivity, notificationUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        // Start vibration
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Vibrate in a pattern: wait 0ms, vibrate 500ms, wait 200ms, repeat
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 500)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from index 0
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
        isVibrating = true
    }

    private fun stopAlarm() {
        // Stop sound
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        // Stop vibration
        if (isVibrating) {
            vibrator?.cancel()
            isVibrating = false
        }
    }

    companion object {
        const val EXTRA_TIMER_LABEL = "timer_label"
        const val EXTRA_TIMER_ID = "timer_id"
        private const val NOTIFICATION_ID_ALARM_BASE = 3000
    }
}

@Composable
private fun AlarmScreen(
    timerLabel: String,
    onDismiss: () -> Unit
) {
    // Pulsing animation for the alarm text
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Time's Up!",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = timerLabel,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
