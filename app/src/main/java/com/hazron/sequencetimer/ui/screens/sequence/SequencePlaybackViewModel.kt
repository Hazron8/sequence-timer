package com.hazron.sequencetimer.ui.screens.sequence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.data.repository.SequenceRepository
import com.hazron.sequencetimer.domain.SequenceStateManager
import com.hazron.sequencetimer.domain.model.SequencePlaybackState
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.SequenceWithSteps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SequencePlaybackUiState(
    val sequenceWithSteps: SequenceWithSteps? = null,
    val playbackState: SequencePlaybackState? = null,
    val currentStep: SequenceStep? = null,
    val nextStep: SequenceStep? = null,
    val isLoading: Boolean = true,
    val progress: Float = 1f,
    val overallProgress: Float = 0f
) {
    val isRunning: Boolean
        get() = playbackState?.isRunning == true && playbackState.isPaused == false

    val isPaused: Boolean
        get() = playbackState?.isPaused == true

    val isComplete: Boolean
        get() = playbackState?.isComplete == true

    val remainingSeconds: Long
        get() = playbackState?.currentStepRemainingSeconds ?: currentStep?.durationSeconds ?: 0

    val currentStepIndex: Int
        get() = playbackState?.currentStepIndex ?: 0

    val totalSteps: Int
        get() = sequenceWithSteps?.stepCount ?: 0
}

@HiltViewModel
class SequencePlaybackViewModel @Inject constructor(
    private val sequenceRepository: SequenceRepository,
    private val sequenceStateManager: SequenceStateManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sequenceId: Long = savedStateHandle.get<Long>("sequenceId") ?: -1L

    private val _sequenceWithSteps = MutableStateFlow<SequenceWithSteps?>(null)

    val uiState: StateFlow<SequencePlaybackUiState> = combine(
        _sequenceWithSteps,
        sequenceStateManager.sequenceStates
    ) { sequenceWithSteps, allStates ->
        val playbackState = allStates[sequenceId]
        val steps = sequenceWithSteps?.sortedSteps ?: emptyList()
        val currentStep = playbackState?.getCurrentStep(steps)
        val nextStep = playbackState?.getNextStep(steps)

        // Calculate progress for current step
        val stepProgress = if (currentStep != null && currentStep.durationSeconds > 0) {
            (playbackState?.currentStepRemainingSeconds ?: 0).toFloat() / currentStep.durationSeconds.toFloat()
        } else {
            1f
        }

        // Calculate overall progress
        val totalDuration = sequenceWithSteps?.totalDurationSeconds ?: 0
        val completedDuration = steps
            .take(playbackState?.currentStepIndex ?: 0)
            .sumOf { it.durationSeconds }
        val currentStepElapsed = (currentStep?.durationSeconds ?: 0) -
            (playbackState?.currentStepRemainingSeconds ?: 0)
        val overallProgress = if (totalDuration > 0) {
            (completedDuration + currentStepElapsed).toFloat() / totalDuration.toFloat()
        } else {
            0f
        }

        SequencePlaybackUiState(
            sequenceWithSteps = sequenceWithSteps,
            playbackState = playbackState,
            currentStep = currentStep,
            nextStep = nextStep,
            isLoading = sequenceWithSteps == null,
            progress = stepProgress,
            overallProgress = overallProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SequencePlaybackUiState()
    )

    init {
        loadSequence()
    }

    private fun loadSequence() {
        viewModelScope.launch {
            val sequenceWithSteps = sequenceRepository.getSequenceWithSteps(sequenceId)
            _sequenceWithSteps.value = sequenceWithSteps

            if (sequenceWithSteps != null) {
                sequenceStateManager.initializeSequence(sequenceWithSteps)
            }
        }
    }

    fun startSequence() {
        val sequenceWithSteps = _sequenceWithSteps.value ?: return
        sequenceStateManager.startSequence(sequenceWithSteps)
    }

    fun pauseSequence() {
        sequenceStateManager.pauseSequence(sequenceId)
    }

    fun resumeSequence() {
        sequenceStateManager.resumeSequence(sequenceId)
    }

    fun resetSequence() {
        sequenceStateManager.resetSequence(sequenceId)
    }

    fun skipToNextStep() {
        sequenceStateManager.skipToNextStep(sequenceId)
    }

    fun skipToPreviousStep() {
        sequenceStateManager.skipToPreviousStep(sequenceId)
    }

    override fun onCleared() {
        super.onCleared()
        // Don't clear state on navigation - let it persist
    }
}
