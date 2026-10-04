package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.data.repository.SetsRepository
import com.yeromich.pokecollect.domain.model.CardSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SetsViewModel @Inject constructor(
    setsRepository: SetsRepository
) : ViewModel() {

    /** A series name of null means the set belongs to no known era (promos, special releases). */
    data class SeriesGroup(val name: String?, val sets: List<CardSet>)

    sealed class UiState {
        object Loading : UiState()
        data class Success(val groups: List<SeriesGroup>) : UiState()
        object Error : UiState()
    }

    private data class Era(val prefix: String, val name: String)

    // The /sets list endpoint omits the series, but every set id starts with its series id.
    // Newest era first; names match TCGdex /de/series.
    private val eras = listOf(
        Era("me", "Mega-Entwicklung"),
        Era("sv", "Karmesin & Purpur"),
        Era("swsh", "Schwert & Schild"),
        Era("sm", "Sonne & Mond"),
        Era("xy", "XY"),
        Era("bw", "Schwarz & Weiß"),
        Era("hgss", "HeartGold SoulSilver"),
        Era("col", "Ruf der Legenden"),
        Era("pl", "Platin"),
        Era("dp", "Diamant & Perl"),
        Era("ex", "EX"),
        Era("neo", "Neo"),
        Era("ecard", "e-Card"),
        Era("base", "Grund")
    )

    private fun CardSet.eraIndex(): Int =
        eras.indexOfFirst { id.startsWith(it.prefix) }.takeIf { it >= 0 } ?: eras.size

    private fun CardSet.numberInEra(): Double =
        id.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

    val uiState: StateFlow<UiState> = setsRepository.getAllSets()
        .map { result ->
            result.fold(
                onSuccess = { sets ->
                    val groups = sets
                        .sortedWith(compareBy<CardSet> { it.eraIndex() }.thenByDescending { it.numberInEra() })
                        .groupBy { it.eraIndex() }
                        .map { (index, eraSets) -> SeriesGroup(eras.getOrNull(index)?.name, eraSets) }
                    UiState.Success(groups)
                },
                onFailure = { UiState.Error }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
