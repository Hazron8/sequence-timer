package com.hazron.sequencetimer.di

import android.content.Context
import androidx.room.Room
import com.hazron.sequencetimer.data.local.TimerDao
import com.hazron.sequencetimer.data.local.TimerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimerDatabase(
        @ApplicationContext context: Context
    ): TimerDatabase {
        return Room.databaseBuilder(
            context,
            TimerDatabase::class.java,
            "timer_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTimerDao(database: TimerDatabase): TimerDao {
        return database.timerDao()
    }
}
