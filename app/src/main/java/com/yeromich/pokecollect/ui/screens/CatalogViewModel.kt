package com.yeromich.pokecollect.ui.screens

import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import com.yeromich.pokecollect.data.repository.SetsRepository
import com.yeromich.pokecollect.data.repository.CollectionRepository
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import java.net.URLDecoder
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CardRepository,
    collectionRepository: CollectionRepository,
    setsRepository: SetsRepository
) : ViewModel() {

    val ownedIds: StateFlow<Set<String>> = collectionRepository.getOwnedCardIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // The scanner opens the catalog with empty set arguments; treat them as "no set".
    val setId: String? = savedStateHandle.get<String>("setId")?.takeIf { it.isNotBlank() }

    /** Size of the open set, for the "31 / 84 collected" line; null outside a set. */
    val setTotal: StateFlow<Int?> = setsRepository.getAllSets()
        .map { result -> setId?.let { id -> result.getOrNull()?.firstOrNull { it.id == id }?.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val setName: String? = savedStateHandle.get<String>("setName")?.takeIf { it.isNotBlank() }
        ?.let { URLDecoder.decode(it, "UTF-8") }

    private val initialQuery = savedStateHandle.get<String>("query")
        ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    val cards: Flow<PagingData<Card>> = _query
        .debounce(400L)
        .flatMapLatest { q ->
            repository.getCardsPaged(
                query = q.ifBlank { null },
                setId = setId
            )
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(q: String) {
        _query.value = q
    }
}
