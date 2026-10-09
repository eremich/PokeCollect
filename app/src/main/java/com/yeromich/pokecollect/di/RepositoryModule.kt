package com.yeromich.pokecollect.di

import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.data.repository.CardRepositoryImpl
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.data.repository.CollectionRepositoryImpl
import com.yeromich.pokecollect.data.repository.FolderRepository
import com.yeromich.pokecollect.data.repository.FolderRepositoryImpl
import com.yeromich.pokecollect.data.repository.SetsRepository
import com.yeromich.pokecollect.data.repository.SetsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindSetsRepository(impl: SetsRepositoryImpl): SetsRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository
}
