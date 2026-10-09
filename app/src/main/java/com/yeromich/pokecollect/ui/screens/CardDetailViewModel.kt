package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.remote.CardDetailsCache
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.remote.TcgdexCardmarketDto
import com.yeromich.pokecollect.data.repository.CollectionRepository
import com.yeromich.pokecollect.data.repository.FolderRepository
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.Folder
import com.yeromich.pokecollect.domain.model.trendEur
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val folderRepository: FolderRepository,
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi,
    private val detailsCache: CardDetailsCache
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

    // Seeded from cache so a card seen before renders its prices and variants on the first frame.
    private val _cardDetails = MutableStateFlow(detailsCache[card.id])
    val cardDetails: StateFlow<TcgdexCardDto?> = _cardDetails.asStateFlow()

    val pricing: StateFlow<TcgdexCardmarketDto?> = _cardDetails
        .map { it?.pricing?.cardmarket }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _cardDetails.value?.pricing?.cardmarket)

    val collectionCounts: StateFlow<Map<String, Int>> = collectionRepository
        .getVariantCounts(card.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Copies stored under a variant this card doesn't offer still get a pill, so they never vanish.
    val availableVariants: StateFlow<List<String>> = combine(_cardDetails, collectionCounts) { details, counts ->
        val offered = variantsOf(details)
        offered + counts.keys.filter { it !in offered }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), variantsOf(_cardDetails.value))

    val selectedVariant = MutableStateFlow("Normal")

    init {
        if (_cardDetails.value == null) {
            viewModelScope.launch {
                runCatching { tcgdexApi.getCard(card.id) }
                    .onSuccess {
                        detailsCache[card.id] = it
                        _cardDetails.value = it
                    }
            }
        }
        viewModelScope.launch { refileLegacyVariants() }
        viewModelScope.launch { syncOwnedPrices() }
        viewModelScope.launch {
            collectionRepository.getVariantCounts(card.id)
                .first { it.isNotEmpty() }
                .keys.firstOrNull()
                ?.let { if (selectedVariant.value == "Normal") selectedVariant.value = it }
        }
    }

    private fun variantsOf(details: TcgdexCardDto?): List<String> =
        if (details == null) {
            listOf("Normal")
        } else {
            val fromDetailed = details.variants_detailed
                ?.map { v ->
                    when (v.type.lowercase()) {
                        "reverse" -> "Reverse Holo"
                        "holo" -> "Holo"
                        "normal" -> "Normal"
                        else -> v.type
                    }
                }
                ?.distinct()
                ?.ifEmpty { null }

            fromDetailed ?: buildList {
                val v = details.variants
                if (v?.normal == true) add("Normal")
                if (v?.holo == true) add("Holo")
                if (v?.reverse == true) add("Reverse Holo")
                if (v?.firstEdition == true) add("1. Auflage")
                if (isEmpty()) add("Normal")
            }
        }

    // Cards added before variant selection existed were all stored as "Normal". When the card
    // offers exactly one variant, those copies can only be that variant, so re-file them.
    private suspend fun refileLegacyVariants() {
        val offered = variantsOf(_cardDetails.filterNotNull().first())
        val only = offered.singleOrNull() ?: return
        val orphans = collectionRepository.getVariantCounts(card.id).first().keys.filter { it !in offered }
        if (orphans.isEmpty()) return
        orphans.forEach { collectionRepository.moveVariant(card.id, it, only) }
        selectedVariant.value = only
    }

    fun selectVariant(variant: String) {
        selectedVariant.value = variant
    }

    // Keep the stored price of every owned variant in step with what this screen shows,
    // including variants added right here.
    private suspend fun syncOwnedPrices() {
        combine(_cardDetails.filterNotNull(), collectionRepository.getVariantCounts(card.id)) { details, counts ->
            details to counts.keys
        }
            .distinctUntilChanged()
            .collect { (details, variants) ->
                variants.forEach {
                    collectionRepository.savePrice(card.id, it, details.pricing?.cardmarket.trendEur(it))
                }
            }
    }

    val folders: StateFlow<List<Folder>> = folderRepository.getFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Collections the currently selected variant is filed in. */
    val memberFolderIds: StateFlow<Set<Long>> = selectedVariant
        .flatMapLatest { variant -> folderRepository.getFolderIds(card.id, variant) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Adds one copy of the selected variant and files it in exactly [folderIds]. */
    fun addToCollection(folderIds: Set<Long>) {
        viewModelScope.launch {
            val variant = selectedVariant.value
            collectionRepository.addOrIncrementVariant(card, variant)
            folderRepository.setMemberships(card.id, variant, folderIds)
        }
    }

    fun saveFolders(folderIds: Set<Long>) {
        viewModelScope.launch { folderRepository.setMemberships(card.id, selectedVariant.value, folderIds) }
    }

    suspend fun createFolder(name: String): Long = folderRepository.create(name)

    fun addOrIncrement() {
        viewModelScope.launch {
            collectionRepository.addOrIncrementVariant(card, selectedVariant.value)
        }
    }

    fun removeOne() {
        viewModelScope.launch {
            collectionRepository.removeOne(card.id, selectedVariant.value)
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
