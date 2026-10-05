package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAll(): Flow<List<Card>>
    fun isInCollection(id: String): Flow<Boolean>
    fun getQuantity(id: String): Flow<Int>
    fun getCount(): Flow<Int>
    suspend fun addOrIncrement(card: Card)
    suspend fun addOrIncrementVariant(card: Card, variant: String)
    suspend fun removeOne(cardId: String, variant: String)
    suspend fun moveVariant(cardId: String, from: String, to: String)
    fun getVariantCounts(cardId: String): Flow<Map<String, Int>>
    suspend fun remove(id: String)
}
