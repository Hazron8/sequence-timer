package com.hazron.sequencetimer.ui.screens.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val timerRepository: TimerRepository
) : ViewModel() {

    private val timerId: Long = savedStateHandle.get<Long>("timerId") ?: -1

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTimer()
    }

    private fun loadTimer() {
        viewModelScope.launch {
            val timer = timerRepository.getTimer(timerId)
            if (timer != null) {
                _uiState.update {
                    it.copy(
                        timer = timer,
                        remainingSeconds = timer.durationSeconds,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startTimer() {
        val state = _uiState.value
        if (state.timer == null || state.isComplete) return

        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)
                if (_uiState.value.isRunning && !_uiState.value.isPaused) {
                    _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                }
            }

            if (_uiState.value.remainingSeconds <= 0) {
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        isComplete = true
                    )
                }
                // TODO: Trigger notification based on timer.notificationType
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeTimer() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val timer = _uiState.value.timer
        _uiState.update {
            it.copy(
                remainingSeconds = timer?.durationSeconds ?: 0,
                isRunning = false,
                isPaused = false,
                isComplete = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
