package `fun`.cybercode.uniblox.os.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `fun`.cybercode.uniblox.os.data.DesktopConfig
import `fun`.cybercode.uniblox.os.data.OSRepository
import `fun`.cybercode.uniblox.os.data.OSSettings
import `fun`.cybercode.uniblox.os.data.WebApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import `fun`.cybercode.uniblox.os.data.WidgetConfig
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DesktopUiState(
    val settings: OSSettings?,
    val desktops: List<DesktopConfig>,
    val webApps: List<WebApp>,
    val widgets: List<WidgetConfig> = emptyList(),
    val isEditMode: Boolean = false
)

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val desktopState: DesktopUiState) : MainUiState()
}

class MainViewModel(private val repository: OSRepository) : ViewModel() {
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.settings,
        repository.allDesktops,
        repository.allWebApps,
        repository.allDesktops.flatMapLatest { desktops ->
            val selectedId = desktops.find { it.isSelected }?.id ?: desktops.firstOrNull()?.id ?: 0L
            repository.getWidgetsForDesktop(selectedId)
        },
        _isEditMode
    ) { settings, desktops, webApps, widgets, isEditMode ->
        MainUiState.Success(DesktopUiState(settings, desktops, webApps, widgets, isEditMode))
    }.stateIn(
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
            // Create default desktop after setup
            repository.saveDesktop(
                DesktopConfig(
                    name = "Default Desktop",
                    isSelected = true
                )
            )
        }
    }

    fun createDesktop(name: String) {
        viewModelScope.launch {
            repository.saveDesktop(DesktopConfig(name = name))
        }
    }

    fun selectDesktop(id: Long) {
        viewModelScope.launch {
            repository.selectDesktop(id)
        }
    }

    fun updateDesktop(desktop: DesktopConfig) {
        viewModelScope.launch {
            repository.updateDesktop(desktop)
        }
    }

    fun installWebApp(name: String, url: String) {
        viewModelScope.launch {
            repository.saveWebApp(WebApp(name = name, url = url))
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun addWidget(type: String, desktopId: Long, metadata: String? = null) {
        viewModelScope.launch {
            repository.saveWidget(WidgetConfig(desktopId = desktopId, type = type, metadata = metadata, x = 100f, y = 100f))
        }
    }

    fun updateWidgetPosition(id: Long, x: Float, y: Float) {
        viewModelScope.launch {
            repository.updateWidgetPosition(id, x, y)
        }
    }

    fun deleteWidget(widget: WidgetConfig) {
        viewModelScope.launch {
            repository.deleteWidget(widget)
        }
    }
}
