package com.yeromich.pokecollect.data.remote

import com.yeromich.pokecollect.domain.model.nameQueries
import java.util.Locale
import java.text.Collator
import com.yeromich.pokecollect.domain.model.CardOrder
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.toDomain

// German names: Ä sorts with A, Ü with U, the way a German reader expects.
private val germanCollator: Collator = Collator.getInstance(Locale.GERMAN)

class TcgdexCardPagingSource(
    private val api: TcgdexApi,
    private val nameIndex: CardNameIndex,
    private val query: String?,
    private val setId: String?,
    private val order: CardOrder
) : PagingSource<Int, Card>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Card> {
        return try {
            val cards: List<TcgdexCardBriefDto> = when {
                // A set arrives whole, so the search box filters it here.
                setId != null -> api.getSet(setId).cards.filter { query.isNullOrBlank() || it.name.contains(query.trim(), ignoreCase = true) }
                // Typed names are as loose as scanned ones: try the forms TCGdex understands.
                query != null && query.isNotBlank() ->
                    nameQueries(query).firstNotNullOfOrNull { q -> api.searchCards(name = q).ifEmpty { null } }
                        // Nothing exact: a typo, so show the closest real names.
                        ?: nameIndex.closest(query).flatMap { api.searchCards(name = it) }.distinctBy { it.id }
                else -> emptyList()
            }
            val setName = setId ?: ""
            val ordered = when (order) {
                // TCGdex already lists set cards by their number.
                CardOrder.Number -> cards
                CardOrder.Name -> cards.sortedWith(compareBy(germanCollator) { it.name })
            }
            LoadResult.Page(
                data = ordered.map { it.toDomain(setName = setName) },
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Card>): Int? = null
}
