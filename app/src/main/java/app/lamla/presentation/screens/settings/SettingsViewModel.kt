package app.lamla.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.System,
    val userName: String = "",
    val voiceAnnouncements: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        prefs.theme,
        prefs.userName,
        prefs.voiceAnnouncements
    ) { theme, name, voice ->
        SettingsUiState(theme = theme, userName = name, voiceAnnouncements = voice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsUiState())

    fun setTheme(t: AppTheme) { viewModelScope.launch { prefs.setTheme(t) } }
    fun setUserName(name: String) { viewModelScope.launch { prefs.setUserName(name) } }
    fun setVoiceAnnouncements(on: Boolean) { viewModelScope.launch { prefs.setVoiceAnnouncements(on) } }
}
