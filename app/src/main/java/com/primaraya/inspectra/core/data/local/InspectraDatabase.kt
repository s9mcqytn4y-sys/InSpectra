package com.primaraya.inspectra.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.primaraya.inspectra.core.data.local.dao.ChecksheetQueueDao
import com.primaraya.inspectra.core.data.local.dao.MaterialDao
import com.primaraya.inspectra.core.data.local.dao.PartDao
import com.primaraya.inspectra.core.data.local.entity.ChecksheetQueueEntity
import com.primaraya.inspectra.core.data.local.entity.MaterialEntity
import com.primaraya.inspectra.core.data.local.entity.PartEntity

@Database(
    entities = [
        PartEntity::class,
        MaterialEntity::class,
        ChecksheetQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InspectraDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun materialDao(): MaterialDao
    abstract fun checksheetQueueDao(): ChecksheetQueueDao

    companion object {
        @Volatile
        private var INSTANCE: InspectraDatabase? = null

        fun getDatabase(context: Context): InspectraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InspectraDatabase::class.java,
                    "inspectra_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
