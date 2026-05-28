package app.lamla.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.lamla.notifications.NotificationChannels
import app.lamla.ui.theme.AppTheme
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
 * Only stores things that are *user choice* — not derived state. Everything
 * else lives in Room.
 *
 * Per-channel sound URIs are stored as plain strings (uri.toString()) keyed by
 * channel id. Defaults to null = use channel's bundled default.
 *
 * Per-course sound overrides are keyed `courseSound::<courseId>` — DataStore
 * gives us no nested maps; this stays simple and migrates well.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val onboarded = booleanPreferencesKey("onboarded")
        val userName = stringPreferencesKey("user_name")
        val voiceAnnouncements = booleanPreferencesKey("voice_announcements")
        val examMode = booleanPreferencesKey("exam_mode")
        val pomodoroFocusMin = intPreferencesKey("pomodoro_focus_min")
        val pomodoroShortBreakMin = intPreferencesKey("pomodoro_short_break_min")
        val pomodoroLongBreakMin = intPreferencesKey("pomodoro_long_break_min")
        val pomodoroCyclesUntilLong = intPreferencesKey("pomodoro_cycles")
        val batteryGuideShown = booleanPreferencesKey("battery_guide_shown")
        fun channelSound(channelId: String) = stringPreferencesKey("channel_sound::$channelId")
        fun courseSound(courseId: Long) = stringPreferencesKey("course_sound::$courseId")
        val lastRescheduleAt = androidx.datastore.preferences.core.longPreferencesKey("last_reschedule_at")
        val lastBootAt = androidx.datastore.preferences.core.longPreferencesKey("last_boot_at")
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[Keys.theme]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.System
    }
    suspend fun setTheme(value: AppTheme) {
        context.dataStore.edit { it[Keys.theme] = value.name }
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboarded] ?: false }
    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.onboarded] = value }
    }

    /**
     * Display name shown in the home greeting ("Good morning, Karlson").
     *
     * Optional — defaults to empty string. When blank, the UI falls back to
     * a name-less greeting. Locally stored, never sent anywhere — this is *not*
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

    fun channelSound(channelId: String): Flow<String?> =
        context.dataStore.data.map { it[Keys.channelSound(channelId)] }
    suspend fun setChannelSound(channelId: String, uri: String?) {
        context.dataStore.edit { p ->
            if (uri == null) p.remove(Keys.channelSound(channelId)) else p[Keys.channelSound(channelId)] = uri
        }
    }

    fun courseSound(courseId: Long): Flow<String?> =
        context.dataStore.data.map { it[Keys.courseSound(courseId)] }
    suspend fun setCourseSound(courseId: Long, uri: String?) {
        context.dataStore.edit { p ->
            if (uri == null) p.remove(Keys.courseSound(courseId)) else p[Keys.courseSound(courseId)] = uri
        }
    }

    /**
     * Telemetry timestamps for the in-app Diagnostics screen.
     * Local-only — never sent anywhere. Lets the user see "yes, the daily worker
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

    /** One-shot snapshot (use sparingly — prefer Flow observation). */
    suspend fun snapshot(): Preferences = context.dataStore.data.first()
}
