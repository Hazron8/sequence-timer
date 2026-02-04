package com.hazron.sequencetimer.domain.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A sequence is a group of timer steps that run consecutively.
 * Used for routines like yoga flows, HIIT workouts, or Pomodoro cycles.
 */
@Entity(
    tableName = "sequences",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("categoryId")]
)
data class Sequence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long = DefaultCategories.GENERAL_CATEGORY_ID,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/**
 * A single step within a sequence.
 */
@Entity(
    tableName = "sequence_steps",
    foreignKeys = [
        ForeignKey(
            entity = Sequence::class,
            parentColumns = ["id"],
            childColumns = ["sequenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sequenceId")]
)
data class SequenceStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequenceId: Long,
    val label: String,
    val durationSeconds: Long,
    val notificationType: NotificationType = NotificationType.SOUND,
    val stepOrder: Int = 0
)

/**
 * Sequence with all its steps loaded.
 */
data class SequenceWithSteps(
    @Embedded val sequence: Sequence,
    @Relation(
        parentColumn = "id",
        entityColumn = "sequenceId"
    )
    val steps: List<SequenceStep>
) {
    val totalDurationSeconds: Long
        get() = steps.sumOf { it.durationSeconds }

    val stepCount: Int
        get() = steps.size

    val sortedSteps: List<SequenceStep>
        get() = steps.sortedBy { it.stepOrder }
}

/**
 * Represents the current playback state of a running sequence.
 */
data class SequencePlaybackState(
    val sequenceId: Long,
    val currentStepIndex: Int = 0,
    val currentStepRemainingSeconds: Long = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false
) {
    fun getCurrentStep(steps: List<SequenceStep>): SequenceStep? {
        val sorted = steps.sortedBy { it.stepOrder }
        return sorted.getOrNull(currentStepIndex)
    }

    fun getNextStep(steps: List<SequenceStep>): SequenceStep? {
        val sorted = steps.sortedBy { it.stepOrder }
        return sorted.getOrNull(currentStepIndex + 1)
    }

    val progress: Float
        get() = 0f // Calculated externally with total duration
}
