package com.yeromich.pokecollect.data.repository

import androidx.paging.PagingData
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsPaged(query: String?): Flow<PagingData<Card>>
    suspend fun getCardById(id: String): Card
}
