package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.local.CollectedCardDao
import com.yeromich.pokecollect.data.local.toDomain
import com.yeromich.pokecollect.data.local.toEntity
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val dao: CollectedCardDao
) : CollectionRepository {

    override fun getAll(): Flow<List<Card>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun isInCollection(id: String): Flow<Boolean> =
        dao.getById(id).map { it != null }

    override fun getQuantity(id: String): Flow<Int> =
        dao.getById(id).map { it?.quantity ?: 0 }

    override fun getCount(): Flow<Int> = dao.getCount()

    override suspend fun addOrIncrement(card: Card) = addOrIncrementVariant(card, "Normal")

    override suspend fun addOrIncrementVariant(card: Card, variant: String) {
        val existing = dao.getByIdAndVariant(card.id, variant).first()
        if (existing != null) {
            dao.incrementByVariant(card.id, variant)
        } else {
            dao.upsert(card.toEntity().copy(variant = variant))
        }
    }

    override suspend fun removeOne(cardId: String, variant: String) {
        val existing = dao.getByIdAndVariant(cardId, variant).first()
        if (existing != null) {
            if (existing.quantity > 1) {
                dao.decrementByVariant(cardId, variant)
            } else {
                dao.deleteByIdAndVariant(cardId, variant)
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

    override suspend fun remove(id: String) = dao.deleteById(id)
}
