package com.yeromich.pokecollect.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.toDomain

class CardPagingSource(
    private val api: PokemonTcgApi,
    private val query: String?
) : PagingSource<Int, Card>() {

    override fun getRefreshKey(state: PagingState<Int, Card>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Card> {
        val page = params.key ?: 1
        return try {
            val queryStr = query?.takeIf { it.isNotBlank() }?.let { "name:\"*$it*\"" }
            val response = api.getCards(query = queryStr, page = page, pageSize = PAGE_SIZE)
            LoadResult.Page(
                data = response.data.map { it.toDomain() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.data.size < PAGE_SIZE) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
