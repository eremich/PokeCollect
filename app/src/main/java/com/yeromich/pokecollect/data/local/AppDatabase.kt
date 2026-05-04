package com.yeromich.pokecollect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CollectedCardEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectedCardDao(): CollectedCardDao
}
