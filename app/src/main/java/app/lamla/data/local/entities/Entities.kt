package app.lamla.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lamla.domain.model.AttendanceStatus
import app.lamla.domain.model.CaptureType
import app.lamla.domain.model.DeadlineStatus
import app.lamla.domain.model.OfficeHourSlot
import java.time.DayOfWeek

/**
 * Room entities - pure persistence shape. Conversions to/from domain happen in
 * mappers (see [Mappers.kt]). UI never sees these directly.
 *
 * Foreign keys cascade DELETE for child rows (deleting a course deletes its sessions,
 * deadlines, captures, exams). Lecturer deletions SET NULL so courses/questions stay.
 *
 * Indexed columns: every FK + commonly-queried fields (courseId, lecturerId, status,
 * createdAt). Saves us from full table scans on hot queries.
 */

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "lecturers",
    indices = [Index(value = ["name"])]
)
data class LecturerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val officeLocation: String,
    val officeHours: List<OfficeHourSlot>,
    val notes: String
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = LecturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["lecturerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("lecturerId"),
        Index("semesterId"),
        Index(value = ["code", "semesterId"], unique = true)
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val lecturerId: Long?,
    val colorArgb: Int,
    val creditHours: Int,
    val semesterId: Long
)

@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("dayOfWeek")]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int,
    val venue: String,
    val reminderOffsetsMinutes: List<Int>
)

@Entity(
    tableName = "deadlines",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("status"), Index("dueAtEpochMs")]
)
data class DeadlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val description: String,
    val dueAtEpochMs: Long,
    val weightPercent: Float,
    val status: DeadlineStatus,
    val reminderOffsetsMinutes: List<Int>,
    // Grade fields (added in schema v2). scoreObtained null = not yet graded.
    // scoreMax carries a SQL default so the v1→v2 migration can backfill rows.
    val scoreObtained: Float? = null,
    @ColumnInfo(defaultValue = "100") val scoreMax: Float = 100f
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = LecturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["lecturerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("lecturerId"), Index("courseId"), Index("answeredAtEpochMs")]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lecturerId: Long,
    val courseId: Long?,
    val text: String,
    val createdAtEpochMs: Long,
    val answeredAtEpochMs: Long?
)

@Entity(tableName = "personal_events")
data class PersonalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val recurrenceRule: String?,
    val notes: String
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("courseId"), Index("scheduledStartEpochMs")]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long?,
    val scheduledStartEpochMs: Long,
    val scheduledEndEpochMs: Long,
    val actualMinutesStudied: Int,
    val completedAtEpochMs: Long?
)

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("courseId"), Index("createdAtEpochMs")]
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long?,
    val type: CaptureType,
    val filePath: String,
    val createdAtEpochMs: Long,
    val note: String
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("examDateEpochMs")]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val examDateEpochMs: Long,
    val venue: String,
    val topics: List<String>,
    val pastPaperPaths: List<String>
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["classSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("courseId"),
        // One verdict per class meeting.
        Index(value = ["classSessionId", "dateEpochDay"], unique = true)
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classSessionId: Long,
    val courseId: Long,
    val dateEpochDay: Long,
    val status: AttendanceStatus,
    val markedAtEpochMs: Long,
    @ColumnInfo(defaultValue = "0") val auto: Boolean = false
)

@Entity(
    tableName = "venue_locations",
    indices = [Index(value = ["venueKey"], unique = true)]
)
data class VenueLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueKey: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)
