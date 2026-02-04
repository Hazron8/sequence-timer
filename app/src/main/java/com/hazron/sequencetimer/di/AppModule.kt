package com.hazron.sequencetimer.di

import android.content.Context
import androidx.room.Room
import com.hazron.sequencetimer.data.local.CategoryDao
import com.hazron.sequencetimer.data.local.SequenceDao
import com.hazron.sequencetimer.data.local.TimerDao
import com.hazron.sequencetimer.data.local.TimerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideTimerDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): TimerDatabase {
        return Room.databaseBuilder(
            context,
            TimerDatabase::class.java,
            "timer_database"
        )
            .addMigrations(TimerDatabase.MIGRATION_1_2, TimerDatabase.MIGRATION_2_3)
            .addCallback(TimerDatabase.Companion.DatabaseCallback(scope))
            .build()
    }

    @Provides
    @Singleton
    fun provideTimerDao(database: TimerDatabase): TimerDao {
        return database.timerDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: TimerDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideSequenceDao(database: TimerDatabase): SequenceDao {
        return database.sequenceDao()
    }
}
