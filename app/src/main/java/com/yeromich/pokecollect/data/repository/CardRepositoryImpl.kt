package com.yeromich.pokecollect.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.yeromich.pokecollect.data.remote.CardPagingSource
import com.yeromich.pokecollect.data.remote.PokemonTcgApi
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val api: PokemonTcgApi
) : CardRepository {

    override fun getCardsPaged(query: String?): Flow<PagingData<Card>> = Pager(
        config = PagingConfig(
            pageSize = CardPagingSource.PAGE_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { CardPagingSource(api, query) }
    ).flow
}
