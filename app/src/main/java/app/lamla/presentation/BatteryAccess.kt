package app.lamla.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Battery-optimization access: the third runtime gate the reminder system leans on.
 *
 * When the app is under Doze / battery optimization, the OS can defer our alarms and
 * background work to save power, so a "10 minutes before class" reminder may land late
 * or be coalesced into a maintenance window. Asking to be exempt lets class and exam
 * alerts fire on time.
 *
 * These helpers report whether we're currently exempt (re-checked on resume) and route
 * the user to the system battery-optimization screen. We deliberately open the settings
 * list rather than firing ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS directly — the
 * direct dialog needs a sensitive permission Play restricts, and the list screen needs
 * none while still getting the user to the right toggle.
 */

/** True when this app is exempt from battery optimization (reminders won't be deferred). */
fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val pm = ContextCompat.getSystemService(this, PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(packageName)
}

/**
 * Open the system battery-optimization screen. Falls back to this app's details page if
 * the dedicated list is unavailable on the OEM build.
 */
fun Context.openBatteryOptimizationSettings() {
    val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val opened = runCatching { startActivity(list) }.isSuccess
    if (!opened) {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
        runCatching { startActivity(details) }
    }
}

/**
 * Tracks whether the app is exempt from battery optimization, re-reading on every resume
 * so the value flips the moment the user returns from the system screen after allowing it.
 */
@Composable
fun rememberBatteryOptimizationIgnored(): Boolean {
    val context = LocalContext.current
    var ignored by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    LifecycleResumeEffect(Unit) {
        ignored = context.isIgnoringBatteryOptimizations()
        onPauseOrDispose { }
    }
    return ignored
}
