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

    val uiState: StateFlow<UiState> = setsRepository.getAllSets()
        .map { result ->
            result.fold(
                onSuccess = { sets ->
                    val groups = sets
                        .sortedByDescending { it.releaseDate }
                        .groupBy { it.series }
                        .entries
                        .sortedByDescending { (_, seriesSets) ->
                            seriesSets.maxOfOrNull { it.releaseDate } ?: ""
                        }
                        .map { (series, seriesSets) ->
                            series to seriesSets.sortedByDescending { it.releaseDate }
                        }
                    android.util.Log.d("SetsSort", "First series: ${groups.firstOrNull()?.first}, first set: ${groups.firstOrNull()?.second?.firstOrNull()?.name} date=${groups.firstOrNull()?.second?.firstOrNull()?.releaseDate}")
                    UiState.Success(groups)
                },
                onFailure = { UiState.Error(it.localizedMessage ?: "Ошибка загрузки") }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
