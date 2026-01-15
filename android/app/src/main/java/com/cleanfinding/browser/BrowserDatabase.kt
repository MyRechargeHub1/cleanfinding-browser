package com.cleanfinding.browser

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return DownloadStatus.valueOf(value)
    }
}

/**
 * Room database for CleanFinding Browser
 * Contains history, downloads, and other persistent data
 *
 * Database version 2 - Added Downloads support
 */
@Database(
    entities = [HistoryItem::class, Download::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrowserDatabase : RoomDatabase() {

    /**
     * DAO for history operations
     */
    abstract fun historyDao(): HistoryDao

    /**
     * DAO for download operations
     */
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        /**
         * Get singleton instance of the database
         * Uses double-checked locking pattern for thread safety
         */
        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "cleanfinding_browser_db"
                )
                    .fallbackToDestructiveMigration() // For v1, recreate DB on schema changes
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Close database connection (call when app is destroyed)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
