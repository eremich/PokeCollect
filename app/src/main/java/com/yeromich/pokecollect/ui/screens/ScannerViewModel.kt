package com.yeromich.pokecollect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeromich.pokecollect.domain.usecase.RecognizeCardUseCase
import com.yeromich.pokecollect.domain.usecase.RecognitionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

sealed class ScannerUiState {
    object NoPermission : ScannerUiState()
    object PermissionDenied : ScannerUiState()
    object Ready : ScannerUiState()
    object Capturing : ScannerUiState()
    data class Captured(val file: File) : ScannerUiState()
    data class Recognizing(val file: File) : ScannerUiState()
    data class Recognized(val file: File, val result: RecognitionResult) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val recognizeCardUseCase: RecognizeCardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ScannerUiState>(ScannerUiState.NoPermission)
    val state: StateFlow<ScannerUiState> = _state

    fun onPermissionGranted() { _state.value = ScannerUiState.Ready }
    fun onPermissionDenied() { _state.value = ScannerUiState.PermissionDenied }
    fun onCapturing() { _state.value = ScannerUiState.Capturing }
    fun onRetake() { _state.value = ScannerUiState.Ready }

    fun onCaptured(file: File) {
        _state.value = ScannerUiState.Recognizing(file)
        recognizeCardUseCase(file)
            .onEach { result -> _state.value = ScannerUiState.Recognized(file, result) }
            .launchIn(viewModelScope)
    }
}
