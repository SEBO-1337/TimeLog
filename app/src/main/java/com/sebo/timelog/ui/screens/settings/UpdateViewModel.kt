package com.sebo.timelog.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.BuildConfig
import com.sebo.timelog.utils.ReleaseInfo
import com.sebo.timelog.utils.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

class UpdateViewModel : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkForUpdates() {
        if (_state.value is UpdateUiState.Checking) return
        _state.value = UpdateUiState.Checking

        viewModelScope.launch {
            val release = UpdateChecker.fetchLatestRelease()
            if (release == null) {
                _state.value = UpdateUiState.Error("Update-Prüfung fehlgeschlagen.\nBitte Internetverbindung prüfen.")
                return@launch
            }

            val currentVersion = BuildConfig.VERSION_NAME
            _state.value = if (UpdateChecker.isNewer(currentVersion, release.version)) {
                UpdateUiState.UpdateAvailable(release)
            } else {
                UpdateUiState.UpToDate
            }
        }
    }

    fun dismissResult() {
        _state.value = UpdateUiState.Idle
    }
}

