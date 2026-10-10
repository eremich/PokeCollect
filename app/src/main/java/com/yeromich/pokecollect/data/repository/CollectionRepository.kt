package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.local.CollectedCardEntity
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.local.FolderCardEntity
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow

/** What a bulk delete took away, kept so it can be put back exactly as it was. */
class RemovedEntries internal constructor(
    internal val entries: List<CollectedCardEntity>,
    internal val memberships: List<FolderCardEntity>
) {
    val count: Int get() = entries.size
}

interface CollectionRepository {
    fun getAll(): Flow<List<Card>>
    fun isInCollection(id: String): Flow<Boolean>
    fun getQuantity(id: String): Flow<Int>
    fun getCount(): Flow<Int>
    fun getValue(): Flow<CollectionValue>
    suspend fun savePrice(cardId: String, variant: String, priceEur: Double?)
    /** Re-prices entries whose price is missing or older than a day. Failures are skipped quietly. */
    suspend fun refreshStalePrices()
    suspend fun addOrIncrement(card: Card)
    suspend fun addOrIncrementVariant(card: Card, variant: String)
    suspend fun removeOne(cardId: String, variant: String)
    suspend fun moveVariant(cardId: String, from: String, to: String)
    fun getVariantCounts(cardId: String): Flow<Map<String, Int>>
    suspend fun remove(id: String)
    /** Removes these card variants entirely, copies and folder filings included. */
    suspend fun deleteEntries(cards: List<Card>): RemovedEntries
    suspend fun restoreEntries(removed: RemovedEntries)
}
