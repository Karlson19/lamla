package app.lamla.attendance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Receives geofence ENTER / DWELL transitions and hands off to [AttendanceGeofenceWorker].
 *
 * The receiver itself stays tiny (its 10s budget is precious): it just extracts which
 * venues were triggered and enqueues WorkManager, which survives Doze and can touch the
 * database safely.
 */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_DWELL
        ) return

        val venueKeys = event.triggeringGeofences?.map { it.requestId }?.distinct().orEmpty()
        if (venueKeys.isEmpty()) return

        val data = Data.Builder()
            .putStringArray(AttendanceGeofenceWorker.KEY_VENUE_KEYS, venueKeys.toTypedArray())
            .build()
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<AttendanceGeofenceWorker>()
                .setInputData(data)
                .build()
        )
    }
}
