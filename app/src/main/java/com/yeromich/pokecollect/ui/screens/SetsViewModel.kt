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

    sealed class UiState {
        object Loading : UiState()
        data class Success(val groups: List<Pair<String, List<CardSet>>>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val epochOrder = mapOf(
        "me" to 0, "sv" to 1, "swsh" to 2, "sm" to 3,
        "xy" to 4, "bw" to 5, "hgss" to 6, "col" to 7,
        "pl" to 8, "dp" to 9, "ex" to 10, "neo" to 11,
        "ecard" to 12, "base" to 13
    )

    private fun CardSet.epochIndex(): Int {
        epochOrder.forEach { (prefix, index) ->
            if (id.startsWith(prefix)) return index
        }
        return 99
    }

    private fun CardSet.numberInEpoch(): Double {
        return id.filter { it.isDigit() || it == '.' }
            .toDoubleOrNull() ?: 0.0
    }

    val uiState: StateFlow<UiState> = setsRepository.getAllSets()
        .map { result ->
            result.fold(
                onSuccess = { sets ->
                    val sortedSets = sets.sortedWith(
                        compareBy<CardSet> { it.epochIndex() }
                            .thenByDescending { it.numberInEpoch() }
                    )

                    val groups = sortedSets
                        .groupBy { it.series }
                        .entries
                        .sortedBy { (_, seriesSets) ->
                            seriesSets.minOfOrNull { it.epochIndex() } ?: 99
                        }
                        .map { (series, seriesSets) ->
                            series to seriesSets.sortedWith(
                                compareBy<CardSet> { it.epochIndex() }
                                    .thenByDescending { it.numberInEpoch() }
                            )
                        }

                    android.util.Log.d("SetsSort", "First: ${groups.firstOrNull()?.second?.firstOrNull()?.let { "${it.name} id=${it.id}" }}")
                    UiState.Success(groups)
                },
                onFailure = { UiState.Error(it.localizedMessage ?: "Ошибка загрузки") }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
