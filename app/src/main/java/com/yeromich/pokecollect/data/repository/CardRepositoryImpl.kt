package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.remote.CardNameIndex
import com.yeromich.pokecollect.domain.model.CardOrder
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.remote.TcgdexCardPagingSource
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

class CardRepositoryImpl @Inject constructor(
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi,
    private val nameIndex: CardNameIndex
) : CardRepository {

    override fun getCardsPaged(query: String?, setId: String?, order: CardOrder): Flow<PagingData<Card>> = Pager(
        config = PagingConfig(pageSize = 50, enablePlaceholders = false),
        pagingSourceFactory = { TcgdexCardPagingSource(tcgdexApi, nameIndex, query, setId, order) }
    ).flow

    override suspend fun searchCards(query: String, pageSize: Int): Result<List<Card>> =
        runCatching {
            tcgdexApi.searchCards(name = query).take(pageSize).map { it.toDomain() }
        }
}
