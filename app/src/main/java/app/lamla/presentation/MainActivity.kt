package app.lamla.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.lamla.notifications.RescheduleAllWorker
import app.lamla.presentation.navigation.LamlaNavHost
import app.lamla.ui.theme.LamlaTheme
import app.lamla.ui.theme.ThemeAccent
import app.lamla.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val WORK_NAME_APP_OPEN_REFRESH = "lamla-app-open-refresh"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge with transparent system bars. The Theme layer (LamlaTheme)
        // sets isAppearanceLightStatusBars based on the active surface luminance.
        enableEdgeToEdge()

        // App-open refresh: kick a one-shot reschedule. Cheap, idempotent, and
        // catches anything that drifted between launches (OS-killed alarms,
        // out-of-band data changes via JSON import, etc.). REPLACE policy means
        // back-to-back launches collapse to a single run.
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            WORK_NAME_APP_OPEN_REFRESH,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleAllWorker>().build()
        )

        // Hold the splash until we've resolved the onboarded preference, so we
        // don't briefly mount the wrong start destination on cold launch.
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        setContent {
            val rootVm: RootViewModel = hiltViewModel()
            val themeMode by rootVm.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val themeAccent by rootVm.themeAccent.collectAsStateWithLifecycle(initialValue = ThemeAccent.Classic)
            val onboarded by rootVm.onboarded.collectAsStateWithLifecycle()
            keepSplash = onboarded == null

            LamlaTheme(mode = themeMode, accent = themeAccent) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Wait until we know if onboarding is done before mounting nav graph.
                    onboarded?.let { done ->
                        LamlaNavHost(startOnboarded = done)
                        // Ask for the notification grant once the user is past
                        // onboarding (so the prompt lands with context, right as
                        // they reach a screen that depends on reminders).
                        if (done) RequestNotificationPermissionOnce()
                    }
                }
            }
        }
    }
}
