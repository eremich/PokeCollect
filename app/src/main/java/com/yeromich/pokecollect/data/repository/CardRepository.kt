package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.domain.model.CardOrder
import androidx.paging.PagingData
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsPaged(query: String? = null, setId: String? = null, order: CardOrder = CardOrder.Number): Flow<PagingData<Card>>
    suspend fun searchCards(query: String, pageSize: Int): Result<List<Card>>
}
