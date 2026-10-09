package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.data.repository.FolderRepository
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CollectionSort { Newest, MostValuable }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    init {
        viewModelScope.launch { collectionRepository.refreshStalePrices() }
    }

    val folders: StateFlow<List<Folder>> = folderRepository.getFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null shows every owned card; otherwise only the cards filed in that folder. */
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _sort = MutableStateFlow(CollectionSort.Newest)
    val sort: StateFlow<CollectionSort> = _sort.asStateFlow()

    val allValue: StateFlow<CollectionValue?> = collectionRepository.getValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val value: StateFlow<CollectionValue?> = _selectedFolderId
        .flatMapLatest { id -> if (id == null) collectionRepository.getValue() else folderRepository.getValue(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allPreviewImages: StateFlow<List<String>> = collectionRepository.getAll()
        .map { cards -> cards.take(PREVIEW_CARDS).map { it.imageSmall } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards: StateFlow<List<Card>> = _selectedFolderId
        .flatMapLatest { id -> if (id == null) collectionRepository.getAll() else folderRepository.getCards(id) }
        .combine(_sort) { cards, sort ->
            when (sort) {
                CollectionSort.Newest -> cards
                // Unpriced cards sink to the end instead of posing as the cheapest.
                CollectionSort.MostValuable -> cards.sortedWith(
                    compareBy<Card> { it.priceEur == null }.thenByDescending { it.priceEur ?: 0.0 }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSort(sort: CollectionSort) {
        _sort.value = sort
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { _selectedFolderId.value = folderRepository.create(name) }
    }

    fun renameFolder(folderId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { folderRepository.rename(folderId, name) }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folderId) _selectedFolderId.value = null
            folderRepository.delete(folderId)
        }
    }

    private companion object {
        const val PREVIEW_CARDS = 3
    }
}
