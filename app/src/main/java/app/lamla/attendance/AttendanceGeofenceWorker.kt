package app.lamla.attendance

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Off the receiver's hot path: resolves which class meeting the triggered venue(s)
 * belong to right now and marks attendance. Retries on failure (a transient DB hiccup
 * shouldn't lose a check-in).
 */
@HiltWorker
class AttendanceGeofenceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val autoMarker: AttendanceAutoMarker
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val keys = inputData.getStringArray(KEY_VENUE_KEYS)?.toList().orEmpty()
        if (keys.isNotEmpty()) autoMarker.markForVenues(keys)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_VENUE_KEYS = "venue_keys"
    }
}
