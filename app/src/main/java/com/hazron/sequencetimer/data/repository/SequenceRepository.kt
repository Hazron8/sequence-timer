package com.hazron.sequencetimer.data.repository

import com.hazron.sequencetimer.data.local.SequenceDao
import com.hazron.sequencetimer.domain.model.Sequence
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.SequenceWithSteps
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SequenceRepository @Inject constructor(
    private val sequenceDao: SequenceDao
) {
    fun getAllSequences(): Flow<List<Sequence>> = sequenceDao.getAllSequences()

    fun getSequencesByCategory(categoryId: Long): Flow<List<Sequence>> =
        sequenceDao.getSequencesByCategory(categoryId)

    fun getAllSequencesWithSteps(): Flow<List<SequenceWithSteps>> =
        sequenceDao.getAllSequencesWithSteps()

    fun getSequencesByCategoryWithSteps(categoryId: Long): Flow<List<SequenceWithSteps>> =
        sequenceDao.getSequencesByCategoryWithSteps(categoryId)

    suspend fun getSequenceWithSteps(id: Long): SequenceWithSteps? =
        sequenceDao.getSequenceWithSteps(id)

    fun getSequenceWithStepsFlow(id: Long): Flow<SequenceWithSteps?> =
        sequenceDao.getSequenceWithStepsFlow(id)

    suspend fun getSequence(id: Long): Sequence? = sequenceDao.getSequence(id)

    suspend fun insertSequence(sequence: Sequence): Long {
        val maxOrder = sequenceDao.getMaxSortOrder() ?: -1
        return sequenceDao.insertSequence(sequence.copy(sortOrder = maxOrder + 1))
    }

    suspend fun updateSequence(sequence: Sequence) = sequenceDao.updateSequence(sequence)

    suspend fun deleteSequence(id: Long) = sequenceDao.deleteSequenceById(id)

    // Step operations
    fun getStepsForSequence(sequenceId: Long): Flow<List<SequenceStep>> =
        sequenceDao.getStepsForSequence(sequenceId)

    suspend fun getStepsForSequenceSync(sequenceId: Long): List<SequenceStep> =
        sequenceDao.getStepsForSequenceSync(sequenceId)

    suspend fun getStep(id: Long): SequenceStep? = sequenceDao.getStep(id)

    suspend fun insertStep(step: SequenceStep): Long {
        val maxOrder = sequenceDao.getMaxStepOrder(step.sequenceId) ?: -1
        return sequenceDao.insertStep(step.copy(stepOrder = maxOrder + 1))
    }

    suspend fun insertSteps(steps: List<SequenceStep>) = sequenceDao.insertSteps(steps)

    suspend fun updateStep(step: SequenceStep) = sequenceDao.updateStep(step)

    suspend fun deleteStep(id: Long) = sequenceDao.deleteStepById(id)

    suspend fun deleteAllStepsForSequence(sequenceId: Long) =
        sequenceDao.deleteAllStepsForSequence(sequenceId)

    suspend fun reorderSteps(steps: List<SequenceStep>) {
        steps.forEachIndexed { index, step ->
            sequenceDao.updateStepOrder(step.id, index)
        }
    }

    suspend fun moveSequencesToCategory(fromCategoryId: Long, toCategoryId: Long) =
        sequenceDao.moveSequencesToCategory(fromCategoryId, toCategoryId)

    /**
     * Create a new sequence with steps in a single transaction.
     */
    suspend fun createSequenceWithSteps(
        name: String,
        categoryId: Long,
        steps: List<Pair<String, Long>> // label to durationSeconds
    ): Long {
        val sequenceId = insertSequence(Sequence(name = name, categoryId = categoryId))
        val sequenceSteps = steps.mapIndexed { index, (label, duration) ->
            SequenceStep(
                sequenceId = sequenceId,
                label = label,
                durationSeconds = duration,
                stepOrder = index
            )
        }
        sequenceDao.insertSteps(sequenceSteps)
        return sequenceId
    }
}
