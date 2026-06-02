package app.lamla.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.ui.theme.ThemeAccent
import app.lamla.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val themeAccent: ThemeAccent = ThemeAccent.Classic,
    val userName: String = "",
    val voiceAnnouncements: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        prefs.themeMode,
        prefs.themeAccent,
        prefs.userName,
        prefs.voiceAnnouncements
    ) { mode, accent, name, voice ->
        SettingsUiState(themeMode = mode, themeAccent = accent, userName = name, voiceAnnouncements = voice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsUiState())

    fun setThemeMode(m: ThemeMode) { viewModelScope.launch { prefs.setThemeMode(m) } }
    fun setThemeAccent(a: ThemeAccent) { viewModelScope.launch { prefs.setThemeAccent(a) } }
    fun setUserName(name: String) { viewModelScope.launch { prefs.setUserName(name) } }
    fun setVoiceAnnouncements(on: Boolean) { viewModelScope.launch { prefs.setVoiceAnnouncements(on) } }
}
