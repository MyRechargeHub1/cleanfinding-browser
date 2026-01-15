package com.cleanfinding.browser

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for CleanFinding Browser
 * Contains history, downloads, and other persistent data
 *
 * Database version 1 - Initial release with History support
 */
@Database(
    entities = [HistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {

    /**
     * DAO for history operations
     */
    abstract fun historyDao(): HistoryDao

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
