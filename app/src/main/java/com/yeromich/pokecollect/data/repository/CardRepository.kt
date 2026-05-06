package com.yeromich.pokecollect.data.repository

import androidx.paging.PagingData
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsPaged(query: String? = null, setId: String? = null): Flow<PagingData<Card>>
    suspend fun searchCards(query: String, pageSize: Int): Result<List<Card>>
}
