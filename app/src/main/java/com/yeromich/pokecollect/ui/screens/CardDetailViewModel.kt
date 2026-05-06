package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi
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

    private val _cardDetails = MutableStateFlow<TcgdexCardDto?>(null)
    val cardDetails: StateFlow<TcgdexCardDto?> = _cardDetails.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tcgdexApi.getCard(card.id) }
                .onSuccess { _cardDetails.value = it }
        }
    }

    fun addOrIncrement() {
        viewModelScope.launch {
            collectionRepository.addOrIncrement(card)
        }
    }
}
