package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.remote.TcgdexCardmarketDto
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    val pricing: StateFlow<TcgdexCardmarketDto?> = _cardDetails
        .map { it?.pricing?.cardmarket }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val availableVariants: StateFlow<List<String>> = _cardDetails
        .map { details ->
            if (details == null) return@map listOf("Normal")
            val detailed = details.variants_detailed
            if (!detailed.isNullOrEmpty()) {
                detailed.map { variantDisplayNames[it.type] ?: it.type }.distinct()
            } else {
                val v = details.variants
                if (v != null) {
                    buildList {
                        if (v.normal == true) add("Normal")
                        if (v.holo == true) add("Holo")
                        if (v.reverse == true) add("Reverse Holo")
                        if (v.firstEdition == true) add("1. Auflage")
                    }.takeIf { it.isNotEmpty() } ?: listOf("Normal")
                } else {
                    listOf("Normal")
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("Normal"))

    val selectedVariant = MutableStateFlow("Normal")

    init {
        viewModelScope.launch {
            android.util.Log.d("CardDetail", "Loading card: ${card.id}")
            runCatching { tcgdexApi.getCard(card.id) }
                .onSuccess {
                    android.util.Log.d("CardDetail", "Success: pricing=${it.pricing}")
                    _cardDetails.value = it
                }
                .onFailure {
                    android.util.Log.e("CardDetail", "Error: ${it.message}", it)
                }
        }
    }

    fun selectVariant(variant: String) {
        selectedVariant.value = variant
    }

    fun addOrIncrement() {
        viewModelScope.launch {
            collectionRepository.addOrIncrementVariant(card, selectedVariant.value)
        }
    }

    companion object {
        private val variantDisplayNames = mapOf(
            "Normal" to "Normal",
            "normal" to "Normal",
            "Reverse" to "Reverse Holo",
            "reverse" to "Reverse Holo",
            "Holo" to "Holo",
            "holo" to "Holo",
            "firstEdition" to "1. Auflage"
        )
    }
}
