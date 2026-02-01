package com.hazron.sequencetimer.domain

import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class RunningTimerState(
    val timerId: Long,
    val totalSeconds: Long,
    val remainingSeconds: Long,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
) {
    val isComplete: Boolean get() = remainingSeconds <= 0
    val progress: Float get() = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
}

/**
 * Singleton manager for timer state that persists across navigation.
 * Timers continue running even when the user navigates away from the timer screen.
 */
@Singleton
class TimerStateManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timerJobs = mutableMapOf<Long, Job>()

    private val _timerStates = MutableStateFlow<Map<Long, RunningTimerState>>(emptyMap())
    val timerStates: StateFlow<Map<Long, RunningTimerState>> = _timerStates.asStateFlow()

    fun getTimerState(timerId: Long): RunningTimerState? = _timerStates.value[timerId]

    fun getTimerStateFlow(timerId: Long): StateFlow<RunningTimerState?> {
        return MutableStateFlow(_timerStates.value[timerId]).also { flow ->
            scope.launch {
                _timerStates.collect { states ->
                    flow.value = states[timerId]
                }
            }
        }
    }

    fun initializeTimer(timer: Timer) {
        val existing = _timerStates.value[timer.id]
        if (existing == null) {
            _timerStates.update { states ->
                states + (timer.id to RunningTimerState(
                    timerId = timer.id,
                    totalSeconds = timer.durationSeconds,
                    remainingSeconds = timer.durationSeconds,
                    isRunning = false,
                    isPaused = false
                ))
            }
        }
    }

    fun startTimer(timerId: Long, totalSeconds: Long) {
        // Cancel existing job if any
        timerJobs[timerId]?.cancel()

        // Initialize or update state
        _timerStates.update { states ->
            val existing = states[timerId]
            val remaining = existing?.remainingSeconds ?: totalSeconds
            states + (timerId to RunningTimerState(
                timerId = timerId,
                totalSeconds = totalSeconds,
                remainingSeconds = remaining,
                isRunning = true,
                isPaused = false
            ))
        }

        // Start countdown
        timerJobs[timerId] = scope.launch {
            while (isActive) {
                delay(1000)
                val state = _timerStates.value[timerId] ?: break

                if (state.isRunning && !state.isPaused && state.remainingSeconds > 0) {
                    _timerStates.update { states ->
                        val current = states[timerId] ?: return@update states
                        val newRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                        states + (timerId to current.copy(
                            remainingSeconds = newRemaining,
                            isRunning = newRemaining > 0,
                            isPaused = false
                        ))
                    }

                    // Check if complete
                    if (_timerStates.value[timerId]?.remainingSeconds == 0L) {
                        // Timer complete - notification will be triggered by observer
                        break
                    }
                } else if (!state.isRunning || state.isComplete) {
                    break
                }
            }
        }
    }

    fun pauseTimer(timerId: Long) {
        _timerStates.update { states ->
            val current = states[timerId] ?: return@update states
            states + (timerId to current.copy(isPaused = true))
        }
    }

    fun resumeTimer(timerId: Long) {
        _timerStates.update { states ->
            val current = states[timerId] ?: return@update states
            states + (timerId to current.copy(isPaused = false))
        }
    }

    fun resetTimer(timerId: Long, totalSeconds: Long) {
        timerJobs[timerId]?.cancel()
        _timerStates.update { states ->
            states + (timerId to RunningTimerState(
                timerId = timerId,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isRunning = false,
                isPaused = false
            ))
        }
    }

    fun stopTimer(timerId: Long) {
        timerJobs[timerId]?.cancel()
        timerJobs.remove(timerId)
        _timerStates.update { states ->
            val current = states[timerId] ?: return@update states
            states + (timerId to current.copy(isRunning = false, isPaused = false))
        }
    }

    fun clearTimer(timerId: Long) {
        timerJobs[timerId]?.cancel()
        timerJobs.remove(timerId)
        _timerStates.update { it - timerId }
    }
}
