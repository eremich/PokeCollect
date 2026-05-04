package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CardDetailUiState {
    object Loading : CardDetailUiState()
    data class Success(val card: Card) : CardDetailUiState()
    data class Error(val message: String) : CardDetailUiState()
}

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val cardId: String = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<CardDetailUiState>(CardDetailUiState.Loading)
    val uiState: StateFlow<CardDetailUiState> = _uiState

    val isInCollection: StateFlow<Boolean> = collectionRepository
        .isInCollection(cardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val quantity: StateFlow<Int> = collectionRepository
        .getQuantity(cardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            try {
                _uiState.value = CardDetailUiState.Success(cardRepository.getCardById(cardId))
            } catch (e: Exception) {
                _uiState.value = CardDetailUiState.Error(e.localizedMessage ?: "Ошибка загрузки")
            }
        }
    }

    fun addOrIncrement() {
        val state = _uiState.value
        if (state !is CardDetailUiState.Success) return
        viewModelScope.launch {
            collectionRepository.addOrIncrement(state.card)
        }
    }
}
