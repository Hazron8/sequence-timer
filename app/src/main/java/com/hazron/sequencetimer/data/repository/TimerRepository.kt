package com.hazron.sequencetimer.data.repository

import com.hazron.sequencetimer.data.local.TimerDao
import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timerDao: TimerDao
) {
    fun getAllTimers(): Flow<List<Timer>> = timerDao.getAllTimers()

    fun getTimersByCategory(categoryId: Long): Flow<List<Timer>> = timerDao.getTimersByCategory(categoryId)

    fun getTimerById(id: Long): Flow<Timer?> = timerDao.getTimerByIdFlow(id)

    suspend fun getTimer(id: Long): Timer? = timerDao.getTimerById(id)

    suspend fun insertTimer(timer: Timer): Long {
        val maxOrder = timerDao.getMaxSortOrder() ?: -1
        return timerDao.insertTimer(timer.copy(sortOrder = maxOrder + 1))
    }

    suspend fun updateTimer(timer: Timer) = timerDao.updateTimer(timer)

    suspend fun deleteTimer(timer: Timer) = timerDao.deleteTimer(timer)

    suspend fun deleteTimerById(id: Long) = timerDao.deleteTimerById(id)

    suspend fun moveTimersToCategory(fromCategoryId: Long, toCategoryId: Long) =
        timerDao.moveTimersToCategory(fromCategoryId, toCategoryId)

    /**
     * Duplicate an existing timer.
     * Returns the new timer ID, or null if the original timer was not found.
     */
    suspend fun duplicateTimer(timerId: Long): Long? {
        val original = getTimer(timerId) ?: return null
        val newTimer = original.copy(
            id = 0,
            label = "${original.label} (copy)",
            createdAt = System.currentTimeMillis()
        )
        return insertTimer(newTimer)
    }
}
