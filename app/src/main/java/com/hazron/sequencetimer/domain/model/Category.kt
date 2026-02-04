package com.hazron.sequencetimer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a category for organizing timers.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String? = null,  // Material icon name or emoji
    val color: Long? = null,   // ARGB color value
    val sortOrder: Int = 0,
    val isDefault: Boolean = false  // Cannot be deleted if true
)

/**
 * Default categories that are created on first app launch.
 */
object DefaultCategories {
    val categories = listOf(
        Category(id = 1, name = "General", icon = "timer", sortOrder = 0, isDefault = true),
        Category(id = 2, name = "Yoga", icon = "self_improvement", sortOrder = 1, isDefault = true),
        Category(id = 3, name = "Workout", icon = "fitness_center", sortOrder = 2, isDefault = true),
        Category(id = 4, name = "Cooking", icon = "restaurant", sortOrder = 3, isDefault = true),
        Category(id = 5, name = "Pomodoro", icon = "work", sortOrder = 4, isDefault = true)
    )

    const val GENERAL_CATEGORY_ID = 1L
}
