package com.hazron.sequencetimer.ui.screens.sequence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.CategoryRepository
import com.hazron.sequencetimer.data.repository.SequenceRepository
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.DefaultCategories
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Sequence
import com.hazron.sequencetimer.domain.model.SequenceStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditableStep(
    val id: Long = 0,
    val tempId: Long = System.nanoTime(), // For new steps not yet in DB
    val label: String = "",
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val notificationType: NotificationType = NotificationType.SOUND
) {
    val durationSeconds: Long
        get() = hours * 3600L + minutes * 60L + seconds

    val isValid: Boolean
        get() = label.isNotBlank() && durationSeconds > 0

    val displayLabel: String
        get() = if (label.isBlank()) formatDuration(durationSeconds) else label
}

data class SequenceBuilderUiState(
    val sequenceName: String = "",
    val categoryId: Long = DefaultCategories.GENERAL_CATEGORY_ID,
    val steps: List<EditableStep> = listOf(EditableStep()),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isNewSequence: Boolean = true
) {
    val isValid: Boolean
        get() = sequenceName.isNotBlank() && steps.any { it.isValid }

    val totalDurationSeconds: Long
        get() = steps.sumOf { it.durationSeconds }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

@HiltViewModel
class SequenceBuilderViewModel @Inject constructor(
    private val sequenceRepository: SequenceRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sequenceId: Long = savedStateHandle.get<Long>("sequenceId") ?: -1L

    private val _uiState = MutableStateFlow(SequenceBuilderUiState())
    val uiState: StateFlow<SequenceBuilderUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        if (sequenceId > 0) {
            loadSequence(sequenceId)
        } else {
            _uiState.update { it.copy(isLoading = false, isNewSequence = true) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }

    private fun loadSequence(id: Long) {
        viewModelScope.launch {
            val sequenceWithSteps = sequenceRepository.getSequenceWithSteps(id)
            if (sequenceWithSteps != null) {
                val editableSteps = sequenceWithSteps.sortedSteps.map { step ->
                    EditableStep(
                        id = step.id,
                        tempId = step.id,
                        label = step.label,
                        hours = (step.durationSeconds / 3600).toInt(),
                        minutes = ((step.durationSeconds % 3600) / 60).toInt(),
                        seconds = (step.durationSeconds % 60).toInt(),
                        notificationType = step.notificationType
                    )
                }.ifEmpty { listOf(EditableStep()) }

                _uiState.update {
                    it.copy(
                        sequenceName = sequenceWithSteps.sequence.name,
                        categoryId = sequenceWithSteps.sequence.categoryId,
                        steps = editableSteps,
                        isLoading = false,
                        isNewSequence = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isNewSequence = true) }
            }
        }
    }

    fun updateSequenceName(name: String) {
        _uiState.update { it.copy(sequenceName = name) }
    }

    fun updateCategory(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updateStepLabel(stepIndex: Int, label: String) {
        _uiState.update { state ->
            val newSteps = state.steps.toMutableList()
            if (stepIndex in newSteps.indices) {
                newSteps[stepIndex] = newSteps[stepIndex].copy(label = label)
            }
            state.copy(steps = newSteps)
        }
    }

    fun updateStepDuration(stepIndex: Int, hours: Int, minutes: Int, seconds: Int) {
        _uiState.update { state ->
            val newSteps = state.steps.toMutableList()
            if (stepIndex in newSteps.indices) {
                newSteps[stepIndex] = newSteps[stepIndex].copy(
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds
                )
            }
            state.copy(steps = newSteps)
        }
    }

    fun updateStepNotificationType(stepIndex: Int, notificationType: NotificationType) {
        _uiState.update { state ->
            val newSteps = state.steps.toMutableList()
            if (stepIndex in newSteps.indices) {
                newSteps[stepIndex] = newSteps[stepIndex].copy(notificationType = notificationType)
            }
            state.copy(steps = newSteps)
        }
    }

    fun addStep() {
        _uiState.update { state ->
            state.copy(steps = state.steps + EditableStep())
        }
    }

    fun removeStep(stepIndex: Int) {
        _uiState.update { state ->
            if (state.steps.size > 1 && stepIndex in state.steps.indices) {
                state.copy(steps = state.steps.toMutableList().apply { removeAt(stepIndex) })
            } else {
                state
            }
        }
    }

    fun moveStep(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (fromIndex in state.steps.indices && toIndex in state.steps.indices) {
                val newSteps = state.steps.toMutableList()
                val step = newSteps.removeAt(fromIndex)
                newSteps.add(toIndex, step)
                state.copy(steps = newSteps)
            } else {
                state
            }
        }
    }

    fun saveSequence(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val state = _uiState.value
            val validSteps = state.steps.filter { it.isValid }

            if (validSteps.isEmpty()) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }

            if (state.isNewSequence) {
                // Create new sequence
                val sequence = Sequence(
                    name = state.sequenceName,
                    categoryId = state.categoryId
                )
                val newSequenceId = sequenceRepository.insertSequence(sequence)

                // Insert steps
                val sequenceSteps = validSteps.mapIndexed { index, step ->
                    SequenceStep(
                        sequenceId = newSequenceId,
                        label = step.label,
                        durationSeconds = step.durationSeconds,
                        notificationType = step.notificationType,
                        stepOrder = index
                    )
                }
                sequenceRepository.insertSteps(sequenceSteps)
            } else {
                // Update existing sequence
                val existingSequence = sequenceRepository.getSequence(sequenceId)
                if (existingSequence != null) {
                    sequenceRepository.updateSequence(
                        existingSequence.copy(
                            name = state.sequenceName,
                            categoryId = state.categoryId
                        )
                    )

                    // Delete all existing steps and re-insert
                    sequenceRepository.deleteAllStepsForSequence(sequenceId)

                    val sequenceSteps = validSteps.mapIndexed { index, step ->
                        SequenceStep(
                            sequenceId = sequenceId,
                            label = step.label,
                            durationSeconds = step.durationSeconds,
                            notificationType = step.notificationType,
                            stepOrder = index
                        )
                    }
                    sequenceRepository.insertSteps(sequenceSteps)
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}
