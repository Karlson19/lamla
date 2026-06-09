package app.lamla.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.lamla.R
import app.lamla.data.repo.AttendanceRepository
import app.lamla.data.repo.ClassSessionRepository
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.VenueLocationRepository
import app.lamla.domain.model.AttendanceRecord
import app.lamla.domain.model.AttendanceStatus
import app.lamla.notifications.NotificationChannels
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns "you arrived at venue X" into an attendance verdict.
 *
 * Matches the triggered venue(s) against the timetable: a class is *in progress* here
 * if it's today, at this venue, and now falls inside [start − grace, end]. The first
 * matching meeting that isn't already marked gets a present/late verdict — we never
 * overwrite an existing record, so a manual correction always wins.
 */
@Singleton
class AttendanceAutoMarker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classSessionRepo: ClassSessionRepository,
    private val courseRepo: CourseRepository,
    private val attendanceRepo: AttendanceRepository
) {
    suspend fun markForVenues(venueKeys: List<String>) {
        val keys = venueKeys.map { it.trim().lowercase() }.toSet()
        if (keys.isEmpty()) return

        val now = LocalDateTime.now()
        val dow = now.dayOfWeek
        val nowMin = now.hour * 60 + now.minute
        val epochDay = now.toLocalDate().toEpochDay()

        val inProgress = classSessionRepo.all().filter { s ->
            s.dayOfWeek == dow &&
                VenueLocationRepository.keyOf(s.venue) in keys &&
                nowMin >= s.startMinutes - ENTER_GRACE_MIN &&
                nowMin <= s.endMinutes
        }

        for (s in inProgress) {
            // Never clobber a verdict already on the books (manual or earlier auto).
            if (attendanceRepo.forOccurrence(s.id, epochDay) != null) continue
            val status = if (nowMin <= s.startMinutes + LATE_AFTER_MIN) {
                AttendanceStatus.Present
            } else {
                AttendanceStatus.Late
            }
            attendanceRepo.upsert(
                AttendanceRecord(
                    classSessionId = s.id,
                    courseId = s.courseId,
                    dateEpochDay = epochDay,
                    status = status,
                    markedAtEpochMs = System.currentTimeMillis(),
                    auto = true
                )
            )
            notifyMarked(s.courseId, status)
        }
    }

    private suspend fun notifyMarked(courseId: Long, status: AttendanceStatus) {
        val course = courseRepo.get(courseId) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val verdict = if (status == AttendanceStatus.Late) "Marked late" else "Marked present"
        val notification = NotificationCompat.Builder(context, NotificationChannels.General.id)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("$verdict · ${course.code}")
            .setContentText("Lamla saw you arrive for ${course.name}. Open Attendance to change it.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_BASE + courseId.toInt(), notification)
    }

    companion object {
        /** Arriving up to this many minutes before start still counts as for this class. */
        private const val ENTER_GRACE_MIN = 20
        /** Past this many minutes after start, the verdict is "late" rather than "present". */
        private const val LATE_AFTER_MIN = 10
        private const val NOTIF_BASE = 720_000
    }
}
