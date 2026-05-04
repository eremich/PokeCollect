package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    val card: Card = Json.decodeFromString(
        URLDecoder.decode(checkNotNull(savedStateHandle.get<String>("cardJson")), "UTF-8")
    )

    val isInCollection: StateFlow<Boolean> = collectionRepository
        .isInCollection(card.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val quantity: StateFlow<Int> = collectionRepository
        .getQuantity(card.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addOrIncrement() {
        viewModelScope.launch {
            collectionRepository.addOrIncrement(card)
        }
    }
}
