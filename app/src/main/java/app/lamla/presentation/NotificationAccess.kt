package app.lamla.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Notification access: the runtime gate the whole reminder system depends on.
 *
 * Declaring POST_NOTIFICATIONS in the manifest is not enough on Android 13+. The
 * OS keeps it denied-by-default, so without an explicit runtime request every
 * reminder is silently dropped, no matter how well the channels and alarms are set
 * up. These helpers request the grant once, expose whether notifications are
 * currently allowed (re-checked on resume), and route the user to system settings
 * when a grant has been permanently denied or the app's notifications were blocked.
 */

/** True when the app may actually post notifications (covers the 13+ grant and a manual block). */
fun Context.notificationsAllowed(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

/**
 * Ask for POST_NOTIFICATIONS once per process, the first time we show real UI.
 * No-op below API 33, where the manifest declaration alone is sufficient.
 */
@Composable
fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* A denial is handled by the in-app banner, so nothing to do here. */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Tracks whether notifications are allowed, re-reading on every resume so the value
 * updates the moment the user returns from system settings after enabling them.
 */
@Composable
fun rememberNotificationsAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleResumeEffect(Unit) {
        allowed = context.notificationsAllowed()
        onPauseOrDispose { }
    }
    return allowed
}

/** Open this app's system notification settings: the route once a grant is permanently denied. */
fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    runCatching { startActivity(intent) }
}
