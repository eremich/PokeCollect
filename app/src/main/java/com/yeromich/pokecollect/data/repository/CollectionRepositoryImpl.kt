package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.local.CollectedCardDao
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.local.FolderDao
import com.yeromich.pokecollect.data.remote.CardDetailsCache
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.local.toDomain
import com.yeromich.pokecollect.data.local.toEntity
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.trendEur
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

private val PRICE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(1)
private const val PRICE_FETCH_PARALLELISM = 6

class CollectionRepositoryImpl @Inject constructor(
    private val dao: CollectedCardDao,
    private val folderDao: FolderDao,
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi,
    private val detailsCache: CardDetailsCache
) : CollectionRepository {

    override fun getAll(): Flow<List<Card>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun isInCollection(id: String): Flow<Boolean> =
        dao.getById(id).map { it != null }

    override fun getQuantity(id: String): Flow<Int> =
        dao.getById(id).map { it?.quantity ?: 0 }

    override fun getCount(): Flow<Int> = dao.getCount()

    override fun getValue(): Flow<CollectionValue> = dao.getValue()

    override suspend fun savePrice(cardId: String, variant: String, priceEur: Double?) =
        dao.updatePrice(cardId, variant, priceEur, System.currentTimeMillis())

    override suspend fun refreshStalePrices() = coroutineScope {
        val stale = dao.getWithStalePrices(System.currentTimeMillis() - PRICE_MAX_AGE_MS)
        val gate = Semaphore(PRICE_FETCH_PARALLELISM)
        stale.groupBy { it.id }.map { (cardId, entries) ->
            launch {
                val details = gate.withPermit {
                    detailsCache[cardId] ?: runCatching { tcgdexApi.getCard(cardId) }.getOrNull()
                        ?.also { detailsCache[cardId] = it }
                } ?: return@launch
                entries.forEach { savePrice(cardId, it.variant, details.pricing?.cardmarket.trendEur(it.variant)) }
            }
        }.joinAll()
    }

    override suspend fun addOrIncrement(card: Card) = addOrIncrementVariant(card, "Normal")

    override suspend fun addOrIncrementVariant(card: Card, variant: String) {
        val existing = dao.getByIdAndVariant(card.id, variant).first()
        if (existing != null) {
            dao.incrementByVariant(card.id, variant)
        } else {
            dao.upsert(card.toEntity().copy(variant = variant))
        }
    }

    override suspend fun moveVariant(cardId: String, from: String, to: String) =
        dao.moveVariant(cardId, from, to)

    override suspend fun removeOne(cardId: String, variant: String) {
        val existing = dao.getByIdAndVariant(cardId, variant).first()
        if (existing != null) {
            if (existing.quantity > 1) {
                dao.decrementByVariant(cardId, variant)
            } else {
                dao.deleteEntry(cardId, variant)
            }
        }
    }

    override fun getVariantCounts(cardId: String): Flow<Map<String, Int>> =
        dao.getAll().map { entities ->
            entities
                .filter { it.id == cardId }
                .groupBy { it.variant }
                .mapValues { (_, list) -> list.sumOf { it.quantity } }
        }

    override suspend fun remove(id: String) = dao.deleteCard(id)

    override suspend fun deleteEntries(cards: List<Card>): RemovedEntries {
        val entries = cards.mapNotNull { dao.findByIdAndVariant(it.id, it.variant) }
        val memberships = entries.flatMap { folderDao.getMemberships(it.id, it.variant) }
        entries.forEach { dao.deleteEntry(it.id, it.variant) }
        return RemovedEntries(entries, memberships)
    }

    override suspend fun restoreEntries(removed: RemovedEntries) {
        removed.entries.forEach { dao.upsert(it) }
        folderDao.insertMemberships(removed.memberships)
    }
}
