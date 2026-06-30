package com.primaraya.inspectra.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.primaraya.inspectra.core.data.local.entity.*
import com.primaraya.inspectra.core.data.local.dao.*

@Database(
    entities = [
        PartEntity::class, 
        MaterialEntity::class, 
        DefectEntity::class,
        ChecksheetQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class InSpectraDatabase : RoomDatabase() {
    abstract fun masterDao(): MasterDao
    abstract fun checksheetQueueDao(): ChecksheetQueueDao

    companion object {
        @Volatile
        private var INSTANCE: InSpectraDatabase? = null

        fun getDatabase(context: Context): InSpectraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InSpectraDatabase::class.java,
                    "inspectra_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

typealias InspectraDatabase = InSpectraDatabase
