package app.lamla.domain.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * Domain layer types.
 *
 * Strictly distinct from Room entities - UI/business code only sees these.
 * Repositories map entity ↔ domain. This keeps Room annotations from leaking
 * into ViewModels (and lets us swap the persistence layer one day without
 * touching presentation).
 *
 * Time conventions:
 *   - "Date-time" → [Instant] (epoch ms via toEpochMilli when stored)
 *   - "Time of day" → [LocalTime] (no tz info; for class start/end which is relative to local clock)
 *   - "Day" → [DayOfWeek]
 */

@Serializable
data class Course(
    val id: Long = 0,
    val code: String,                 // e.g. "COE271"
    val name: String,                 // "Computer Networking"
    val lecturerId: Long? = null,
    val colorArgb: Int,               // per-course accent
    val creditHours: Int = 3,
    val semesterId: Long
)

@Serializable
data class ClassSession(
    val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: DayOfWeek,
    val startMinutes: Int,            // minutes since 00:00, local
    val endMinutes: Int,
    val venue: String,
    /** Reminder offsets in minutes-before-start. Spec default: 30, 10, 0. */
    val reminderOffsetsMinutes: List<Int> = listOf(30, 10, 0)
) {
    val start: LocalTime get() = LocalTime.of(startMinutes / 60, startMinutes % 60)
    val end: LocalTime get() = LocalTime.of(endMinutes / 60, endMinutes % 60)
}

enum class DeadlineStatus { Pending, Done }

@Serializable
data class Deadline(
    val id: Long = 0,
    val courseId: Long,
    val title: String,
    val description: String = "",
    val dueAtEpochMs: Long,
    val weightPercent: Float,         // 0..100, contribution to course grade
    val status: DeadlineStatus = DeadlineStatus.Pending,
    /** Spec default: 24h, 6h, 1h, 15m. */
    val reminderOffsetsMinutes: List<Int> = listOf(24 * 60, 6 * 60, 60, 15),
    /**
     * Mark obtained on this assessment, out of [scoreMax]. Null = not graded yet.
     * Used by the CWA projection: a graded deadline contributes
     * (scoreObtained / scoreMax) * weightPercent points to the course mark.
     */
    val scoreObtained: Float? = null,
    val scoreMax: Float = 100f
) {
    val dueAt: Instant get() = Instant.ofEpochMilli(dueAtEpochMs)

    /** Score as a 0..100 percentage, or null if ungraded / [scoreMax] is non-positive. */
    val scorePercent: Float?
        get() = scoreObtained?.let { if (scoreMax > 0f) (it / scoreMax) * 100f else null }

    val isGraded: Boolean get() = scoreObtained != null && scoreMax > 0f
}

@Serializable
data class Lecturer(
    val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val officeLocation: String = "",
    /** Office hour ranges, e.g. "MON 14:00-16:00, WED 10:00-12:00". Stored as a list of [OfficeHourSlot]. */
    val officeHours: List<OfficeHourSlot> = emptyList(),
    val notes: String = ""
)

@Serializable
data class OfficeHourSlot(
    val dayOfWeek: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int
)

@Serializable
data class Question(
    val id: Long = 0,
    val lecturerId: Long,
    val courseId: Long?,
    val text: String,
    val createdAtEpochMs: Long,
    val answeredAtEpochMs: Long? = null
) {
    val isAnswered: Boolean get() = answeredAtEpochMs != null
}

@Serializable
data class PersonalEvent(
    val id: Long = 0,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    /** RFC 5545 RRULE if recurring, else null. */
    val recurrenceRule: String? = null,
    val notes: String = ""
)

@Serializable
data class StudySession(
    val id: Long = 0,
    val courseId: Long?,
    val scheduledStartEpochMs: Long,
    val scheduledEndEpochMs: Long,
    /** Actual focused minutes (sum across Pomodoro segments). */
    val actualMinutesStudied: Int = 0,
    val completedAtEpochMs: Long? = null
)

enum class CaptureType { Text, Photo, Voice }

@Serializable
data class Capture(
    val id: Long = 0,
    val courseId: Long?,
    val type: CaptureType,
    /** For Text: empty. For Photo/Voice: app-private file path. */
    val filePath: String,
    val createdAtEpochMs: Long,
    val note: String = ""
)

@Serializable
data class Exam(
    val id: Long = 0,
    val courseId: Long,
    val examDateEpochMs: Long,
    val venue: String,
    val topics: List<String> = emptyList(),
    val pastPaperPaths: List<String> = emptyList()
)

@Serializable
data class Semester(
    val id: Long = 0,
    val name: String,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val isActive: Boolean = false
)
