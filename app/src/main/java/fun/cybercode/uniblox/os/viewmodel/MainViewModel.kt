package `fun`.cybercode.uniblox.os.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `fun`.cybercode.uniblox.os.data.OSRepository
import `fun`.cybercode.uniblox.os.data.OSSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val settings: OSSettings?) : MainUiState()
}

class MainViewModel(private val repository: OSRepository) : ViewModel() {
    val uiState: StateFlow<MainUiState> = repository.settings
        .map { MainUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState.Loading
        )

    fun completeSetup(name: String, country: String) {
        viewModelScope.launch {
            repository.saveSettings(
                OSSettings(
                    isSetupComplete = true,
                    userName = name,
                    userCountry = country
                )
            )
        }
    }
}
