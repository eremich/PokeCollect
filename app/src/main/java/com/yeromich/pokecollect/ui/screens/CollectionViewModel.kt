package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CollectionSort { Newest, MostValuable }

@HiltViewModel
class CollectionViewModel @Inject constructor(
    collectionRepository: CollectionRepository
) : ViewModel() {

    val value: StateFlow<CollectionValue?> = collectionRepository
        .getValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { collectionRepository.refreshStalePrices() }
    }

    private val _sort = MutableStateFlow(CollectionSort.Newest)
    val sort: StateFlow<CollectionSort> = _sort.asStateFlow()

    val cards: StateFlow<List<Card>> = combine(collectionRepository.getAll(), _sort) { cards, sort ->
        when (sort) {
            CollectionSort.Newest -> cards
            // Unpriced cards sink to the end instead of posing as the cheapest.
            CollectionSort.MostValuable -> cards.sortedWith(
                compareBy<Card> { it.priceEur == null }.thenByDescending { it.priceEur ?: 0.0 }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSort(sort: CollectionSort) {
        _sort.value = sort
    }

    val count: StateFlow<Int> = collectionRepository
        .getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
