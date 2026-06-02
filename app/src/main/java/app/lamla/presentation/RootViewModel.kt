package app.lamla.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.ui.theme.ThemeAccent
import app.lamla.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds app-shell state (theme + onboarding gate).
 *
 * `onboarded` is `StateFlow<Boolean?>` - null means "not loaded yet from DataStore",
 * the first real emission flips to false/true. MainActivity uses the null state
 * to keep the splash up so the nav graph doesn't briefly mount the wrong start
 * destination on cold launch.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    prefs: AppPreferences
) : ViewModel() {
    val themeMode: Flow<ThemeMode> = prefs.themeMode
    val themeAccent: Flow<ThemeAccent> = prefs.themeAccent

    val onboarded: StateFlow<Boolean?> = prefs.onboarded
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)
}
