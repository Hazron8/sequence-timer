package com.hazron.sequencetimer.domain

import com.hazron.sequencetimer.domain.model.SequencePlaybackState
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.SequenceWithSteps
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for sequence playback state that persists across navigation.
 * Handles advancing through steps, pausing, resuming, and skipping.
 */
@Singleton
class SequenceStateManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sequenceJobs = mutableMapOf<Long, Job>()

    private val _sequenceStates = MutableStateFlow<Map<Long, SequencePlaybackState>>(emptyMap())
    val sequenceStates: StateFlow<Map<Long, SequencePlaybackState>> = _sequenceStates.asStateFlow()

    // Store steps for each running sequence
    private val sequenceSteps = mutableMapOf<Long, List<SequenceStep>>()

    // Callback for when a step completes (for notifications)
    private var onStepComplete: ((Long, SequenceStep) -> Unit)? = null

    // Callback for when entire sequence completes
    private var onSequenceComplete: ((Long) -> Unit)? = null

    fun setOnStepCompleteListener(listener: (Long, SequenceStep) -> Unit) {
        onStepComplete = listener
    }

    fun setOnSequenceCompleteListener(listener: (Long) -> Unit) {
        onSequenceComplete = listener
    }

    fun getSequenceState(sequenceId: Long): SequencePlaybackState? = _sequenceStates.value[sequenceId]

    fun getSequenceStateFlow(sequenceId: Long): StateFlow<SequencePlaybackState?> {
        return MutableStateFlow(_sequenceStates.value[sequenceId]).also { flow ->
            scope.launch {
                _sequenceStates.collect { states ->
                    flow.value = states[sequenceId]
                }
            }
        }
    }

    fun initializeSequence(sequenceWithSteps: SequenceWithSteps) {
        val sequenceId = sequenceWithSteps.sequence.id
        val existing = _sequenceStates.value[sequenceId]
        if (existing == null) {
            val sortedSteps = sequenceWithSteps.sortedSteps
            sequenceSteps[sequenceId] = sortedSteps
            val firstStepDuration = sortedSteps.firstOrNull()?.durationSeconds ?: 0

            _sequenceStates.update { states ->
                states + (sequenceId to SequencePlaybackState(
                    sequenceId = sequenceId,
                    currentStepIndex = 0,
                    currentStepRemainingSeconds = firstStepDuration,
                    isRunning = false,
                    isPaused = false,
                    isComplete = false
                ))
            }
        }
    }

    fun startSequence(sequenceWithSteps: SequenceWithSteps) {
        val sequenceId = sequenceWithSteps.sequence.id
        val sortedSteps = sequenceWithSteps.sortedSteps

        if (sortedSteps.isEmpty()) return

        // Cancel existing job if any
        sequenceJobs[sequenceId]?.cancel()

        // Store steps
        sequenceSteps[sequenceId] = sortedSteps

        // Initialize or resume state
        _sequenceStates.update { states ->
            val existing = states[sequenceId]
            val stepIndex = existing?.currentStepIndex ?: 0
            val remaining = existing?.currentStepRemainingSeconds
                ?: sortedSteps.getOrNull(stepIndex)?.durationSeconds
                ?: 0

            states + (sequenceId to SequencePlaybackState(
                sequenceId = sequenceId,
                currentStepIndex = stepIndex,
                currentStepRemainingSeconds = remaining,
                isRunning = true,
                isPaused = false,
                isComplete = false
            ))
        }

        // Start countdown
        sequenceJobs[sequenceId] = scope.launch {
            while (isActive) {
                delay(1000)
                val state = _sequenceStates.value[sequenceId] ?: break
                val steps = sequenceSteps[sequenceId] ?: break

                if (state.isRunning && !state.isPaused && !state.isComplete) {
                    if (state.currentStepRemainingSeconds > 1) {
                        // Continue current step countdown
                        _sequenceStates.update { states ->
                            val current = states[sequenceId] ?: return@update states
                            states + (sequenceId to current.copy(
                                currentStepRemainingSeconds = current.currentStepRemainingSeconds - 1
                            ))
                        }
                    } else {
                        // Current step complete
                        val completedStep = steps.getOrNull(state.currentStepIndex)
                        if (completedStep != null) {
                            onStepComplete?.invoke(sequenceId, completedStep)
                        }

                        // Check if there's a next step
                        val nextIndex = state.currentStepIndex + 1
                        if (nextIndex < steps.size) {
                            // Advance to next step
                            val nextStep = steps[nextIndex]
                            _sequenceStates.update { states ->
                                val current = states[sequenceId] ?: return@update states
                                states + (sequenceId to current.copy(
                                    currentStepIndex = nextIndex,
                                    currentStepRemainingSeconds = nextStep.durationSeconds
                                ))
                            }
                        } else {
                            // Sequence complete
                            _sequenceStates.update { states ->
                                val current = states[sequenceId] ?: return@update states
                                states + (sequenceId to current.copy(
                                    currentStepRemainingSeconds = 0,
                                    isRunning = false,
                                    isComplete = true
                                ))
                            }
                            onSequenceComplete?.invoke(sequenceId)
                            break
                        }
                    }
                } else if (!state.isRunning || state.isComplete) {
                    break
                }
            }
        }
    }

    fun pauseSequence(sequenceId: Long) {
        _sequenceStates.update { states ->
            val current = states[sequenceId] ?: return@update states
            states + (sequenceId to current.copy(isPaused = true))
        }
    }

    fun resumeSequence(sequenceId: Long) {
        _sequenceStates.update { states ->
            val current = states[sequenceId] ?: return@update states
            states + (sequenceId to current.copy(isPaused = false))
        }
    }

    fun skipToNextStep(sequenceId: Long) {
        val state = _sequenceStates.value[sequenceId] ?: return
        val steps = sequenceSteps[sequenceId] ?: return

        val nextIndex = state.currentStepIndex + 1
        if (nextIndex < steps.size) {
            val nextStep = steps[nextIndex]
            _sequenceStates.update { states ->
                val current = states[sequenceId] ?: return@update states
                states + (sequenceId to current.copy(
                    currentStepIndex = nextIndex,
                    currentStepRemainingSeconds = nextStep.durationSeconds
                ))
            }
        }
    }

    fun skipToPreviousStep(sequenceId: Long) {
        val state = _sequenceStates.value[sequenceId] ?: return
        val steps = sequenceSteps[sequenceId] ?: return

        val prevIndex = state.currentStepIndex - 1
        if (prevIndex >= 0) {
            val prevStep = steps[prevIndex]
            _sequenceStates.update { states ->
                val current = states[sequenceId] ?: return@update states
                states + (sequenceId to current.copy(
                    currentStepIndex = prevIndex,
                    currentStepRemainingSeconds = prevStep.durationSeconds
                ))
            }
        }
    }

    fun resetSequence(sequenceId: Long) {
        sequenceJobs[sequenceId]?.cancel()
        val steps = sequenceSteps[sequenceId] ?: return
        val firstStepDuration = steps.firstOrNull()?.durationSeconds ?: 0

        _sequenceStates.update { states ->
            states + (sequenceId to SequencePlaybackState(
                sequenceId = sequenceId,
                currentStepIndex = 0,
                currentStepRemainingSeconds = firstStepDuration,
                isRunning = false,
                isPaused = false,
                isComplete = false
            ))
        }
    }

    fun stopSequence(sequenceId: Long) {
        sequenceJobs[sequenceId]?.cancel()
        sequenceJobs.remove(sequenceId)
        _sequenceStates.update { states ->
            val current = states[sequenceId] ?: return@update states
            states + (sequenceId to current.copy(isRunning = false, isPaused = false))
        }
    }

    fun clearSequence(sequenceId: Long) {
        sequenceJobs[sequenceId]?.cancel()
        sequenceJobs.remove(sequenceId)
        sequenceSteps.remove(sequenceId)
        _sequenceStates.update { it - sequenceId }
    }

    /**
     * Check if any sequences are currently running.
     */
    fun hasRunningSequences(): Boolean {
        return _sequenceStates.value.values.any { it.isRunning && !it.isPaused }
    }

    /**
     * Get all running sequence IDs.
     */
    fun getRunningSequenceIds(): List<Long> {
        return _sequenceStates.value.filter { it.value.isRunning }.keys.toList()
    }
}
