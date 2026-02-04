package com.hazron.sequencetimer.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a timer that can be started, paused, and completed.
 */
@Entity(
    tableName = "timers",
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
data class Timer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val durationSeconds: Long,
    val notificationType: NotificationType = NotificationType.SOUND,
    val categoryId: Long = DefaultCategories.GENERAL_CATEGORY_ID,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/**
 * The type of notification to show when a timer completes.
 */
enum class NotificationType {
    /** Silent notification only - no sound or vibration */
    SILENT,
    /** Notification with a short sound/beep */
    SOUND,
    /** Full-screen alarm that must be dismissed */
    ALARM
}

/**
 * Represents the current state of a running timer.
 */
data class TimerState(
    val timerId: Long,
    val remainingSeconds: Long,
    val isRunning: Boolean,
    val isPaused: Boolean = false
) {
    val isComplete: Boolean get() = remainingSeconds <= 0

    val progress: Float get() = 0f // Will be calculated with original duration
}
