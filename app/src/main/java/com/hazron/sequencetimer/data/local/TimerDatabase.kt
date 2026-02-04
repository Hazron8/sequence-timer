package com.hazron.sequencetimer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.DefaultCategories
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Sequence
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Timer::class, Category::class, Sequence::class, SequenceStep::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun sequenceDao(): SequenceDao

    companion object {
        /**
         * Migration from version 1 to 2: Add categories table and categoryId to timers
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT,
                        color INTEGER,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Insert default categories
                db.execSQL("INSERT INTO categories (id, name, icon, sortOrder, isDefault) VALUES (1, 'General', 'timer', 0, 1)")
                db.execSQL("INSERT INTO categories (id, name, icon, sortOrder, isDefault) VALUES (2, 'Yoga', 'self_improvement', 1, 1)")
                db.execSQL("INSERT INTO categories (id, name, icon, sortOrder, isDefault) VALUES (3, 'Workout', 'fitness_center', 2, 1)")
                db.execSQL("INSERT INTO categories (id, name, icon, sortOrder, isDefault) VALUES (4, 'Cooking', 'restaurant', 3, 1)")
                db.execSQL("INSERT INTO categories (id, name, icon, sortOrder, isDefault) VALUES (5, 'Pomodoro', 'work', 4, 1)")

                // Add categoryId column to timers table with default value
                db.execSQL("ALTER TABLE timers ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 1")

                // Create index on categoryId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timers_categoryId ON timers (categoryId)")
            }
        }

        /**
         * Migration from version 2 to 3: Add sequences and sequence_steps tables
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sequences table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sequences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        categoryId INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE SET DEFAULT
                    )
                """)

                // Create index on sequences categoryId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sequences_categoryId ON sequences (categoryId)")

                // Create sequence_steps table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sequence_steps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sequenceId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        notificationType TEXT NOT NULL DEFAULT 'SOUND',
                        stepOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (sequenceId) REFERENCES sequences(id) ON DELETE CASCADE
                    )
                """)

                // Create index on sequence_steps sequenceId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sequence_steps_sequenceId ON sequence_steps (sequenceId)")
            }
        }

        /**
         * Callback to populate default categories on database creation
         */
        class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert default categories when database is first created
                scope.launch(Dispatchers.IO) {
                    DefaultCategories.categories.forEach { category ->
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO categories (id, name, icon, sortOrder, isDefault)
                            VALUES (${category.id}, '${category.name}', '${category.icon}', ${category.sortOrder}, ${if (category.isDefault) 1 else 0})
                            """
                        )
                    }
                }
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = NotificationType.valueOf(value)
}
