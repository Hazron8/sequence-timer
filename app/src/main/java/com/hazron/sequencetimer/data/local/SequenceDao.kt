package com.hazron.sequencetimer.data.local

import androidx.room.*
import com.hazron.sequencetimer.domain.model.Sequence
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.SequenceWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface SequenceDao {

    // Sequence operations
    @Query("SELECT * FROM sequences ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllSequences(): Flow<List<Sequence>>

    @Query("SELECT * FROM sequences WHERE categoryId = :categoryId ORDER BY sortOrder ASC, createdAt ASC")
    fun getSequencesByCategory(categoryId: Long): Flow<List<Sequence>>

    @Transaction
    @Query("SELECT * FROM sequences ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllSequencesWithSteps(): Flow<List<SequenceWithSteps>>

    @Transaction
    @Query("SELECT * FROM sequences WHERE categoryId = :categoryId ORDER BY sortOrder ASC, createdAt ASC")
    fun getSequencesByCategoryWithSteps(categoryId: Long): Flow<List<SequenceWithSteps>>

    @Transaction
    @Query("SELECT * FROM sequences WHERE id = :id")
    suspend fun getSequenceWithSteps(id: Long): SequenceWithSteps?

    @Transaction
    @Query("SELECT * FROM sequences WHERE id = :id")
    fun getSequenceWithStepsFlow(id: Long): Flow<SequenceWithSteps?>

    @Query("SELECT * FROM sequences WHERE id = :id")
    suspend fun getSequence(id: Long): Sequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: Sequence): Long

    @Update
    suspend fun updateSequence(sequence: Sequence)

    @Delete
    suspend fun deleteSequence(sequence: Sequence)

    @Query("DELETE FROM sequences WHERE id = :id")
    suspend fun deleteSequenceById(id: Long)

    @Query("SELECT MAX(sortOrder) FROM sequences")
    suspend fun getMaxSortOrder(): Int?

    // Step operations
    @Query("SELECT * FROM sequence_steps WHERE sequenceId = :sequenceId ORDER BY stepOrder ASC")
    fun getStepsForSequence(sequenceId: Long): Flow<List<SequenceStep>>

    @Query("SELECT * FROM sequence_steps WHERE sequenceId = :sequenceId ORDER BY stepOrder ASC")
    suspend fun getStepsForSequenceSync(sequenceId: Long): List<SequenceStep>

    @Query("SELECT * FROM sequence_steps WHERE id = :id")
    suspend fun getStep(id: Long): SequenceStep?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: SequenceStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<SequenceStep>)

    @Update
    suspend fun updateStep(step: SequenceStep)

    @Delete
    suspend fun deleteStep(step: SequenceStep)

    @Query("DELETE FROM sequence_steps WHERE id = :id")
    suspend fun deleteStepById(id: Long)

    @Query("DELETE FROM sequence_steps WHERE sequenceId = :sequenceId")
    suspend fun deleteAllStepsForSequence(sequenceId: Long)

    @Query("SELECT MAX(stepOrder) FROM sequence_steps WHERE sequenceId = :sequenceId")
    suspend fun getMaxStepOrder(sequenceId: Long): Int?

    @Query("UPDATE sequence_steps SET stepOrder = :stepOrder WHERE id = :id")
    suspend fun updateStepOrder(id: Long, stepOrder: Int)

    @Query("UPDATE sequences SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun moveSequencesToCategory(oldCategoryId: Long, newCategoryId: Long)
}
