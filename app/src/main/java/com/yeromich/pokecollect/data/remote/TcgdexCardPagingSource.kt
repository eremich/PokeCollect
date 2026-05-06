package com.yeromich.pokecollect.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.toDomain

class TcgdexCardPagingSource(
    private val api: TcgdexApi,
    private val query: String?,
    private val setId: String?
) : PagingSource<Int, Card>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Card> {
        return try {
            val cards: List<TcgdexCardBriefDto> = when {
                setId != null -> api.getSetCards(setId)
                query != null && query.isNotBlank() -> api.searchCards(name = query)
                else -> emptyList()
            }
            val setName = setId ?: ""
            LoadResult.Page(
                data = cards.map { it.toDomain(setName = setName) },
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Card>): Int? = null
}
