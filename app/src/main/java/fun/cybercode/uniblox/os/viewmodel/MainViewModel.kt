package `fun`.cybercode.uniblox.os.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `fun`.cybercode.uniblox.os.data.OSRepository
import `fun`.cybercode.uniblox.os.data.OSSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: OSRepository) : ViewModel() {
    val settings: StateFlow<OSSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
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
