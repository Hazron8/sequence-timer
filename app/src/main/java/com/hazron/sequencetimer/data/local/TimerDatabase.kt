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
import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Timer::class, Category::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun categoryDao(): CategoryDao

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
