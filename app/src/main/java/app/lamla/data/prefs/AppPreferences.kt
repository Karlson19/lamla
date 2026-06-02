package app.lamla.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.lamla.notifications.NotificationChannels
import app.lamla.ui.theme.ThemeAccent
import app.lamla.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lamla_prefs")

/**
 * App preferences (DataStore Preferences).
 *
 * Only stores things that are *user choice* - not derived state. Everything
 * else lives in Room.
 *
 * Per-channel sound URIs are stored as plain strings (uri.toString()) keyed by
 * channel id. Defaults to null = use channel's bundled default.
 *
 * Per-course sound overrides are keyed `courseSound::<courseId>` - DataStore
 * gives us no nested maps; this stays simple and migrates well.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        // `theme` is the legacy single-axis key (pre-1.1.5). Kept only so we can
        // migrate an existing choice into the split mode/accent keys below.
        val theme = stringPreferencesKey("theme")
        val themeMode = stringPreferencesKey("theme_mode")
        val themeAccent = stringPreferencesKey("theme_accent")
        val onboarded = booleanPreferencesKey("onboarded")
        val userName = stringPreferencesKey("user_name")
        val voiceAnnouncements = booleanPreferencesKey("voice_announcements")
        val examMode = booleanPreferencesKey("exam_mode")
        val pomodoroFocusMin = intPreferencesKey("pomodoro_focus_min")
        val pomodoroShortBreakMin = intPreferencesKey("pomodoro_short_break_min")
        val pomodoroLongBreakMin = intPreferencesKey("pomodoro_long_break_min")
        val pomodoroCyclesUntilLong = intPreferencesKey("pomodoro_cycles")
        val batteryGuideShown = booleanPreferencesKey("battery_guide_shown")
        val lastRescheduleAt = androidx.datastore.preferences.core.longPreferencesKey("last_reschedule_at")
        val lastBootAt = androidx.datastore.preferences.core.longPreferencesKey("last_boot_at")
        val pomodoroDeadline = longPreferencesKey("pomodoro_deadline")
        val pomodoroPhase = stringPreferencesKey("pomodoro_phase")
        val priorCwa = floatPreferencesKey("prior_cwa")
        val priorCredits = intPreferencesKey("prior_credits")
    }

    /**
     * Theme is two independent axes now: [themeMode] (light/dark) and [themeAccent]
     * (hue). If the new keys are unset we fall back to migrating the legacy single
     * `theme` value, so an existing user keeps whatever they had picked.
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: legacyMode(prefs[Keys.theme])
    }
    val themeAccent: Flow<ThemeAccent> = context.dataStore.data.map { prefs ->
        prefs[Keys.themeAccent]?.let { runCatching { ThemeAccent.valueOf(it) }.getOrNull() }
            ?: legacyAccent(prefs[Keys.theme])
    }
    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = value.name }
    }
    suspend fun setThemeAccent(value: ThemeAccent) {
        context.dataStore.edit { it[Keys.themeAccent] = value.name }
    }

    /** Map a legacy `AppTheme` name to the mode axis. Light/Gold forced light; Dark forced dark. */
    private fun legacyMode(name: String?): ThemeMode = when (name) {
        "Light", "Gold" -> ThemeMode.Light
        "Dark" -> ThemeMode.Dark
        else -> ThemeMode.System
    }

    /** Map a legacy `AppTheme` name to the accent axis (neutral modes -> Classic). */
    private fun legacyAccent(name: String?): ThemeAccent = when (name) {
        "Gold" -> ThemeAccent.Gold
        "Monochrome" -> ThemeAccent.Monochrome
        "Indigo" -> ThemeAccent.Indigo
        "Emerald" -> ThemeAccent.Emerald
        "Teal" -> ThemeAccent.Teal
        "Ocean" -> ThemeAccent.Ocean
        "Sunset" -> ThemeAccent.Sunset
        "Crimson" -> ThemeAccent.Crimson
        "Rose" -> ThemeAccent.Rose
        "Lavender" -> ThemeAccent.Lavender
        "Plum" -> ThemeAccent.Plum
        else -> ThemeAccent.Classic
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboarded] ?: false }
    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.onboarded] = value }
    }

    /**
     * Display name shown in the home greeting ("Good morning, Karlson").
     *
     * Optional - defaults to empty string. When blank, the UI falls back to
     * a name-less greeting. Locally stored, never sent anywhere - this is *not*
     * an account, just a preference.
     */
    val userName: Flow<String> = context.dataStore.data.map { it[Keys.userName].orEmpty() }
    suspend fun setUserName(value: String) {
        context.dataStore.edit { it[Keys.userName] = value.trim() }
    }

    val voiceAnnouncements: Flow<Boolean> = context.dataStore.data.map { it[Keys.voiceAnnouncements] ?: false }
    suspend fun setVoiceAnnouncements(value: Boolean) {
        context.dataStore.edit { it[Keys.voiceAnnouncements] = value }
    }

    val examMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.examMode] ?: false }
    suspend fun setExamMode(value: Boolean) {
        context.dataStore.edit { it[Keys.examMode] = value }
    }

    val pomodoroFocusMin: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroFocusMin] ?: 25 }
    val pomodoroShortBreakMin: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroShortBreakMin] ?: 5 }
    val pomodoroLongBreakMin: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroLongBreakMin] ?: 15 }
    val pomodoroCyclesUntilLong: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroCyclesUntilLong] ?: 4 }
    suspend fun setPomodoro(focus: Int, shortBreak: Int, longBreak: Int, cycles: Int) {
        context.dataStore.edit {
            it[Keys.pomodoroFocusMin] = focus.coerceIn(5, 90)
            it[Keys.pomodoroShortBreakMin] = shortBreak.coerceIn(1, 30)
            it[Keys.pomodoroLongBreakMin] = longBreak.coerceIn(5, 60)
            it[Keys.pomodoroCyclesUntilLong] = cycles.coerceIn(2, 8)
        }
    }

    val batteryGuideShown: Flow<Boolean> = context.dataStore.data.map { it[Keys.batteryGuideShown] ?: false }
    suspend fun setBatteryGuideShown(value: Boolean) {
        context.dataStore.edit { it[Keys.batteryGuideShown] = value }
    }

    /**
     * Telemetry timestamps for the in-app Diagnostics screen.
     * Local-only - never sent anywhere. Lets the user see "yes, the daily worker
     * really did run last night at 03:14" without having to trust a code review.
     */
    val lastRescheduleAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastRescheduleAt] ?: 0L }
    suspend fun setLastRescheduleAt(ts: Long) {
        context.dataStore.edit { it[Keys.lastRescheduleAt] = ts }
    }

    val lastBootAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastBootAt] ?: 0L }
    suspend fun setLastBootAt(ts: Long) {
        context.dataStore.edit { it[Keys.lastBootAt] = ts }
    }

    val pomodoroDeadline: Flow<Long> = context.dataStore.data.map { it[Keys.pomodoroDeadline] ?: 0L }
    suspend fun setPomodoroDeadline(ts: Long) {
        context.dataStore.edit { it[Keys.pomodoroDeadline] = ts }
    }

    val pomodoroPhase: Flow<String?> = context.dataStore.data.map { it[Keys.pomodoroPhase] }
    suspend fun setPomodoroPhase(phase: String?) {
        context.dataStore.edit {
            if (phase == null) it.remove(Keys.pomodoroPhase) else it[Keys.pomodoroPhase] = phase
        }
    }

    /**
     * Prior academic standing for the CWA projection.
     *
     * The student already knows their cumulative average and how many credit hours
     * it covers; we layer this semester's projected marks on top of it. [priorCwa]
     * is null until set (first-semester students leave it blank), [priorCredits]
     * defaults to 0. Stored locally only - never an account.
     */
    val priorCwa: Flow<Float?> = context.dataStore.data.map { it[Keys.priorCwa] }
    val priorCredits: Flow<Int> = context.dataStore.data.map { it[Keys.priorCredits] ?: 0 }
    suspend fun setPriorStanding(cwa: Float?, credits: Int) {
        context.dataStore.edit {
            if (cwa == null) it.remove(Keys.priorCwa) else it[Keys.priorCwa] = cwa.coerceIn(0f, 100f)
            it[Keys.priorCredits] = credits.coerceAtLeast(0)
        }
    }

    /** One-shot snapshot (use sparingly - prefer Flow observation). */
    suspend fun snapshot(): Preferences = context.dataStore.data.first()
}
