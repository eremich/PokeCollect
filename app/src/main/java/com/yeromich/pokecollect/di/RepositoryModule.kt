package com.yeromich.pokecollect.di

import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.data.repository.CardRepositoryImpl
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.data.repository.CollectionRepositoryImpl
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
}
