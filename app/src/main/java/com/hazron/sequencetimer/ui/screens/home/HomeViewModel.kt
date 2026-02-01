package com.hazron.sequencetimer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.RunningTimerState
import com.hazron.sequencetimer.domain.TimerStateManager
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val timers: List<Timer> = emptyList(),
    val runningStates: Map<Long, RunningTimerState> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val timerStateManager: TimerStateManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        timerRepository.getAllTimers(),
        timerStateManager.timerStates
    ) { timers, runningStates ->
        HomeUiState(
            timers = timers,
            runningStates = runningStates,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            timerStateManager.clearTimer(timer.id)
            timerRepository.deleteTimer(timer)
        }
    }
}
