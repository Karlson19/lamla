package app.lamla.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lamla.attendance.GeofenceManager
import app.lamla.data.prefs.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Reads the entire database and re-issues every reminder.
 *
 * Trigger paths (all enqueue this same worker):
 *   - BootReceiver after device reboot / app update / time change (input: trigger=boot)
 *   - ReminderReceiver after a class/office-hours alarm fires (input: none)
 *   - Daily PeriodicWorkRequest from LamlaApplication (input: none)
 *   - MainActivity on cold start (input: none)
 *   - Manual "Run reschedule now" from Settings → Diagnostics (input: trigger=manual)
 *
 * Always writes lastRescheduleAt on success. When trigger=boot, also writes
 * lastBootAt - so the Diagnostics screen can show the user "the boot receiver
 * really did fire after your phone rebooted on Friday."
 *
 * Cost: ~one DB-wide read pass + N scheduler.schedule() IPC calls. Cheap.
 */
@HiltWorker
class RescheduleAllWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: ReminderEngine,
    private val prefs: AppPreferences,
    private val geofenceManager: GeofenceManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        engine.rescheduleAll()
        // Geofences are lost across reboot/app-update just like alarms — re-register
        // them on the same heartbeat so attendance auto-marking survives a restart.
        runCatching { geofenceManager.refresh() }
        val now = System.currentTimeMillis()
        prefs.setLastRescheduleAt(now)
        if (inputData.getString(KEY_TRIGGER) == TRIGGER_BOOT) {
            prefs.setLastBootAt(now)
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_TRIGGER = "trigger"
        const val TRIGGER_BOOT = "boot"
        const val TRIGGER_MANUAL = "manual"
    }
}
