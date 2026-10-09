package com.yeromich.pokecollect.di

import android.content.Context
import androidx.room.Room
import com.yeromich.pokecollect.data.local.AppDatabase
import com.yeromich.pokecollect.data.local.CollectedCardDao
import com.yeromich.pokecollect.data.local.FolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pokecollect.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

    @Provides
    @Singleton
    fun provideCollectedCardDao(db: AppDatabase): CollectedCardDao = db.collectedCardDao()

    @Provides
    @Singleton
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
}
