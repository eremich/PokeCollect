package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    val cards: StateFlow<List<Card>> = collectionRepository
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val count: StateFlow<Int> = collectionRepository
        .getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
