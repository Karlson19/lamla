package app.lamla.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.lamla.data.repo.VenueLocationRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers a geofence per pinned venue so arriving at a lecture hall can auto-mark
 * attendance — even with the app closed (needs background-location).
 *
 * Idempotent: [refresh] tears down the old set and re-adds from the current venue
 * table, so callers just fire it whenever venues change, on app start, and on boot.
 * Geofence transitions land in [GeofenceReceiver] via a single PendingIntent.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val venueRepo: VenueLocationRepository
) {
    private val client by lazy { LocationServices.getGeofencingClient(context) }

    fun hasForegroundLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java).setAction(ACTION_GEOFENCE)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /** Re-register geofences from the venue table. No-op without foreground location. */
    @SuppressLint("MissingPermission")
    suspend fun refresh() {
        if (!hasForegroundLocation()) return
        val venues = venueRepo.all()
        // Remove the previous set first; then add the current one once that's done.
        client.removeGeofences(pendingIntent).addOnCompleteListener {
            if (venues.isEmpty()) return@addOnCompleteListener
            val geofences = venues.map { v ->
                Geofence.Builder()
                    .setRequestId(v.venueKey)
                    .setCircularRegion(v.latitude, v.longitude, v.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(3 * 60 * 1000)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofences)
                .build()
            try {
                client.addGeofences(request, pendingIntent)
            } catch (_: SecurityException) {
                // Background location not granted yet — surfaced in the Attendance UI.
            }
        }
    }

    /** Drop all geofences (e.g. the user turned the feature off). */
    fun clear() {
        runCatching { client.removeGeofences(pendingIntent) }
    }

    companion object {
        const val ACTION_GEOFENCE = "app.lamla.attendance.GEOFENCE"
    }
}
