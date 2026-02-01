package com.hazron.sequencetimer.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditTimerUiState(
    val timerId: Long? = null,
    val label: String = "",
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val notificationType: NotificationType = NotificationType.SOUND,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isNewTimer: Boolean = true
) {
    val totalSeconds: Long
        get() = (hours * 3600L) + (minutes * 60L) + seconds

    val isValid: Boolean
        get() = label.isNotBlank() && totalSeconds > 0
}

@HiltViewModel
class EditTimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerRepository: TimerRepository
) : ViewModel() {

    private val timerId: Long = savedStateHandle.get<Long>("timerId") ?: -1

    private val _uiState = MutableStateFlow(EditTimerUiState())
    val uiState: StateFlow<EditTimerUiState> = _uiState.asStateFlow()

    init {
        if (timerId > 0) {
            loadTimer()
        } else {
            _uiState.update { it.copy(isLoading = false, isNewTimer = true) }
        }
    }

    private fun loadTimer() {
        viewModelScope.launch {
            val timer = timerRepository.getTimer(timerId)
            if (timer != null) {
                val hours = (timer.durationSeconds / 3600).toInt()
                val minutes = ((timer.durationSeconds % 3600) / 60).toInt()
                val seconds = (timer.durationSeconds % 60).toInt()

                _uiState.update {
                    it.copy(
                        timerId = timer.id,
                        label = timer.label,
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        notificationType = timer.notificationType,
                        isLoading = false,
                        isNewTimer = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isNewTimer = true) }
            }
        }
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(label = label) }
    }

    fun updateHours(hours: Int) {
        _uiState.update { it.copy(hours = hours.coerceIn(0, 23)) }
    }

    fun updateMinutes(minutes: Int) {
        _uiState.update { it.copy(minutes = minutes.coerceIn(0, 59)) }
    }

    fun updateSeconds(seconds: Int) {
        _uiState.update { it.copy(seconds = seconds.coerceIn(0, 59)) }
    }

    fun updateNotificationType(type: NotificationType) {
        _uiState.update { it.copy(notificationType = type) }
    }

    fun saveTimer(onComplete: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val timer = Timer(
                id = state.timerId ?: 0,
                label = state.label.trim(),
                durationSeconds = state.totalSeconds,
                notificationType = state.notificationType
            )

            if (state.isNewTimer) {
                timerRepository.insertTimer(timer)
            } else {
                timerRepository.updateTimer(timer)
            }

            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}
