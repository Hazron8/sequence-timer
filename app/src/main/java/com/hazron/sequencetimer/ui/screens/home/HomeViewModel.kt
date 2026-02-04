package com.hazron.sequencetimer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.CategoryRepository
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.RunningTimerState
import com.hazron.sequencetimer.domain.TimerStateManager
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val timers: List<Timer> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null, // null means "All"
    val runningStates: Map<Long, RunningTimerState> = emptyMap(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val categoryRepository: CategoryRepository,
    private val timerStateManager: TimerStateManager
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    init {
        // Ensure default categories exist
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedCategoryId.flatMapLatest { categoryId ->
            if (categoryId == null) {
                timerRepository.getAllTimers()
            } else {
                timerRepository.getTimersByCategory(categoryId)
            }
        },
        categoryRepository.getAllCategories(),
        timerStateManager.timerStates,
        _selectedCategoryId
    ) { timers, categories, runningStates, selectedCategoryId ->
        HomeUiState(
            timers = timers,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            runningStates = runningStates,
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

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            timerStateManager.clearTimer(timer.id)
            timerRepository.deleteTimer(timer)
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
