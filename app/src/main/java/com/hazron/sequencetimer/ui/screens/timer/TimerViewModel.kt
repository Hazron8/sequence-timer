package com.hazron.sequencetimer.ui.screens.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.TimerStateManager
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val timer: Timer? = null,
    val remainingSeconds: Long = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
    val isLoading: Boolean = true
) {
    val progress: Float
        get() = if (timer != null && timer.durationSeconds > 0) {
            remainingSeconds.toFloat() / timer.durationSeconds.toFloat()
        } else 0f
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerRepository: TimerRepository,
    private val timerStateManager: TimerStateManager
) : ViewModel() {

    private val timerId: Long = savedStateHandle.get<Long>("timerId") ?: -1

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        loadTimer()
        observeTimerState()
    }

    private fun loadTimer() {
        viewModelScope.launch {
            val timer = timerRepository.getTimer(timerId)
            if (timer != null) {
                // Initialize in state manager if not already
                timerStateManager.initializeTimer(timer)

                // Get current running state
                val runningState = timerStateManager.getTimerState(timerId)

                _uiState.update {
                    it.copy(
                        timer = timer,
                        remainingSeconds = runningState?.remainingSeconds ?: timer.durationSeconds,
                        isRunning = runningState?.isRunning ?: false,
                        isPaused = runningState?.isPaused ?: false,
                        isComplete = runningState?.isComplete ?: false,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeTimerState() {
        viewModelScope.launch {
            timerStateManager.timerStates.collect { states ->
                val state = states[timerId]
                if (state != null) {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = state.remainingSeconds,
                            isRunning = state.isRunning,
                            isPaused = state.isPaused,
                            isComplete = state.isComplete
                        )
                    }
                }
            }
        }
    }

    fun startTimer() {
        val timer = _uiState.value.timer ?: return
        timerStateManager.startTimer(timerId, timer.durationSeconds)
    }

    fun pauseTimer() {
        timerStateManager.pauseTimer(timerId)
    }

    fun resumeTimer() {
        timerStateManager.resumeTimer(timerId)
    }

    fun resetTimer() {
        val timer = _uiState.value.timer ?: return
        timerStateManager.resetTimer(timerId, timer.durationSeconds)
    }
}
