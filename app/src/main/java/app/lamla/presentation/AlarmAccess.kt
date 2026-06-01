package app.lamla.presentation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.lamla.notifications.RescheduleAllWorker

/**
 * Exact-alarm access: the second runtime gate the reminder system depends on.
 *
 * On Android 12+ a class-reminder app needs the user's permission to fire alarms
 * at an exact minute. Below API 31 it is implicit. From API 31 on, if the grant is
 * missing every reminder silently downgrades to an inexact alarm that Doze can delay
 * by minutes, so a "10 minutes before class" ping can land after class has started.
 *
 * These helpers report whether exact alarms are currently allowed (re-checked on
 * resume), route the user to the one system screen that grants it, and re-arm every
 * pending alarm the moment the grant flips on so they upgrade to exact immediately.
 */

/** True when we may schedule exact alarms. Always true below API 31. */
fun Context.canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = ContextCompat.getSystemService(this, AlarmManager::class.java) ?: return false
    return am.canScheduleExactAlarms()
}

/**
 * Open the system "Alarms & reminders" screen for this app, where the user grants
 * exact-alarm access. No-op below API 31 (the grant does not exist there). Falls
 * back to the app's details page if the dedicated screen is unavailable on the OEM.
 */
fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val direct = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.fromParts("package", packageName, null))
    val opened = runCatching { startActivity(direct) }.isSuccess
    if (!opened) {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
        runCatching { startActivity(details) }
    }
}

/**
 * Tracks whether exact alarms are allowed, re-reading on every resume so the value
 * updates the moment the user returns from the system screen after granting it.
 */
@Composable
fun rememberExactAlarmAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.canScheduleExactAlarms()) }
    LifecycleResumeEffect(Unit) {
        allowed = context.canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    return allowed
}

/**
 * Re-arm every reminder. Called when exact-alarm access flips on so already-queued
 * inexact alarms get replaced by exact ones without waiting for the next cold start.
 */
fun Context.enqueueExactAlarmReschedule() {
    WorkManager.getInstance(this).enqueueUniqueWork(
        WORK_NAME_EXACT_ALARM_RESCHEDULE,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<RescheduleAllWorker>().build()
    )
}

private const val WORK_NAME_EXACT_ALARM_RESCHEDULE = "lamla-exact-alarm-reschedule"
