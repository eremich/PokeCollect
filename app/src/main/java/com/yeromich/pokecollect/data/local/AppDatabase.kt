package com.yeromich.pokecollect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CollectedCardEntity::class, FolderEntity::class, FolderCardEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectedCardDao(): CollectedCardDao
    abstract fun folderDao(): FolderDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folder_cards` (`folderId` INTEGER NOT NULL, `cardId` TEXT NOT NULL, " +
                        "`variant` TEXT NOT NULL, PRIMARY KEY(`folderId`, `cardId`, `variant`), " +
                        "FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_folder_cards_cardId_variant` ON `folder_cards` (`cardId`, `variant`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `collected_cards` ADD COLUMN `priceEur` REAL")
                db.execSQL("ALTER TABLE `collected_cards` ADD COLUMN `priceUpdatedAt` INTEGER")
            }
        }

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
