package com.hazron.sequencetimer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.CategoryRepository
import com.hazron.sequencetimer.data.repository.SequenceRepository
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.RunningTimerState
import com.hazron.sequencetimer.domain.SequenceStateManager
import com.hazron.sequencetimer.domain.TimerStateManager
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.SequencePlaybackState
import com.hazron.sequencetimer.domain.model.SequenceWithSteps
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab {
    TIMERS, SEQUENCES
}

data class HomeUiState(
    val timers: List<Timer> = emptyList(),
    val sequences: List<SequenceWithSteps> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null, // null means "All"
    val selectedTab: HomeTab = HomeTab.TIMERS,
    val runningStates: Map<Long, RunningTimerState> = emptyMap(),
    val sequenceStates: Map<Long, SequencePlaybackState> = emptyMap(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sequenceRepository: SequenceRepository,
    private val categoryRepository: CategoryRepository,
    private val timerStateManager: TimerStateManager,
    private val sequenceStateManager: SequenceStateManager
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedTab = MutableStateFlow(HomeTab.TIMERS)

    init {
        // Ensure default categories exist
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }
    }

    private val timersFlow = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) {
            timerRepository.getAllTimers()
        } else {
            timerRepository.getTimersByCategory(categoryId)
        }
    }

    private val sequencesFlow = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) {
            sequenceRepository.getAllSequencesWithSteps()
        } else {
            sequenceRepository.getSequencesByCategoryWithSteps(categoryId)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        timersFlow,
        sequencesFlow,
        categoryRepository.getAllCategories(),
        timerStateManager.timerStates,
        sequenceStateManager.sequenceStates,
        _selectedCategoryId,
        _selectedTab
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val timers = values[0] as List<Timer>
        val sequences = values[1] as List<SequenceWithSteps>
        val categories = values[2] as List<Category>
        val runningStates = values[3] as Map<Long, RunningTimerState>
        val sequenceStates = values[4] as Map<Long, SequencePlaybackState>
        val selectedCategoryId = values[5] as Long?
        val selectedTab = values[6] as HomeTab

        HomeUiState(
            timers = timers,
            sequences = sequences,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            selectedTab = selectedTab,
            runningStates = runningStates,
            sequenceStates = sequenceStates,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            timerStateManager.clearTimer(timer.id)
            timerRepository.deleteTimer(timer)
        }
    }

    fun deleteSequence(sequenceId: Long) {
        viewModelScope.launch {
            sequenceStateManager.clearSequence(sequenceId)
            sequenceRepository.deleteSequence(sequenceId)
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.createCategory(name)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            // Move timers to General before deleting
            timerRepository.moveTimersToCategory(categoryId, 1L)
            categoryRepository.deleteCategory(categoryId)
            // If we were viewing the deleted category, switch to All
            if (_selectedCategoryId.value == categoryId) {
                _selectedCategoryId.value = null
            }
        }
    }
}
