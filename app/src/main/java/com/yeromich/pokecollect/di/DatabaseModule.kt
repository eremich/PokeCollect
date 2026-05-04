package com.yeromich.pokecollect.di

import android.content.Context
import androidx.room.Room
import com.yeromich.pokecollect.data.local.AppDatabase
import com.yeromich.pokecollect.data.local.CollectedCardDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "pokecollect.db").build()

    @Provides
    @Singleton
    fun provideCollectedCardDao(db: AppDatabase): CollectedCardDao = db.collectedCardDao()
}
