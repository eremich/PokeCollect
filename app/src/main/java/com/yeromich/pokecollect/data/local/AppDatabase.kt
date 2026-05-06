package com.yeromich.pokecollect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CollectedCardEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectedCardDao(): CollectedCardDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE `collected_cards_new` (
                        `id` TEXT NOT NULL,
                        `variant` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `number` TEXT NOT NULL,
                        `setName` TEXT NOT NULL,
                        `rarity` TEXT,
                        `imageSmall` TEXT NOT NULL,
                        `imageLarge` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `condition` TEXT,
                        PRIMARY KEY(`id`, `variant`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `collected_cards_new`
                    SELECT `id`, 'Normal', `name`, `number`, `setName`, `rarity`,
                           `imageSmall`, `imageLarge`, `addedAt`, `quantity`, `condition`
                    FROM `collected_cards`
                """.trimIndent())
                db.execSQL("DROP TABLE `collected_cards`")
                db.execSQL("ALTER TABLE `collected_cards_new` RENAME TO `collected_cards`")
            }
        }
    }
}
