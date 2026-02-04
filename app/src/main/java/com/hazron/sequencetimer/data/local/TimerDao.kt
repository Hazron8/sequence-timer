package com.hazron.sequencetimer.data.local

import androidx.room.*
import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllTimers(): Flow<List<Timer>>

    @Query("SELECT * FROM timers WHERE categoryId = :categoryId ORDER BY sortOrder ASC, createdAt ASC")
    fun getTimersByCategory(categoryId: Long): Flow<List<Timer>>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getTimerById(id: Long): Timer?

    @Query("SELECT * FROM timers WHERE id = :id")
    fun getTimerByIdFlow(id: Long): Flow<Timer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: Timer): Long

    @Update
    suspend fun updateTimer(timer: Timer)

    @Delete
    suspend fun deleteTimer(timer: Timer)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimerById(id: Long)

    @Query("SELECT COUNT(*) FROM timers")
    suspend fun getTimerCount(): Int

    @Query("SELECT MAX(sortOrder) FROM timers")
    suspend fun getMaxSortOrder(): Int?

    @Query("UPDATE timers SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun moveTimersToCategory(oldCategoryId: Long, newCategoryId: Long)
}
