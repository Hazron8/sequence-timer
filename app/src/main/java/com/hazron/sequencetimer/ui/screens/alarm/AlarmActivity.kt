package com.hazron.sequencetimer.ui.screens.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hazron.sequencetimer.ui.theme.SequenceTimerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen alarm activity shown when a timer with ALARM notification type completes.
 * TODO: Implement full alarm UI with sound and dismiss functionality in Phase 2
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val timerLabel = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"

        setContent {
            SequenceTimerTheme {
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
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = timerLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = { finish() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Dismiss", modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TIMER_LABEL = "timer_label"
        const val EXTRA_TIMER_ID = "timer_id"
    }
}
