package app.lamla.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.lamla.data.repo.*
import app.lamla.domain.model.*
import app.lamla.notifications.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class LamlaBackup(
    val version: Int = 1,
    val semesters: List<Semester> = emptyList(),
    val courses: List<Course> = emptyList(),
    val classSessions: List<ClassSession> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val lecturers: List<Lecturer> = emptyList(),
    val questions: List<Question> = emptyList(),
    val personalEvents: List<PersonalEvent> = emptyList(),
    val studySessions: List<StudySession> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val captures: List<Capture> = emptyList()
)

/**
 * Backup/restore.
 *
 * Pure JSON via kotlinx-serialization. The schema is the domain models —
 * forwards-compatible by virtue of @Serializable's permissive defaults
 * (new optional fields don't break old backups).
 *
 * Imports re-run the ReminderEngine.rescheduleAll() at the end so alarms
 * are correct for the restored state.
 */
@HiltViewModel
class DataExportImportViewModel @Inject constructor(
    private val semesterRepo: SemesterRepository,
    private val courseRepo: CourseRepository,
    private val classRepo: ClassSessionRepository,
    private val deadlineRepo: DeadlineRepository,
    private val lecturerRepo: LecturerRepository,
    private val questionRepo: QuestionRepository,
    private val personalRepo: PersonalEventRepository,
    private val studyRepo: StudySessionRepository,
    private val examRepo: ExamRepository,
    private val captureRepo: CaptureRepository,
    private val engine: ReminderEngine
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportTo(context: Context, uri: Uri): Boolean = runCatching {
        val backup = LamlaBackup(
            semesters = semesterRepo.observeAll().first(),
            courses = courseRepo.observeAll().first(),
            classSessions = classRepo.observeAll().first(),
            deadlines = deadlineRepo.observeAll().first(),
            lecturers = lecturerRepo.observeAll().first(),
            // Questions don't have an observe-all repo method; we skip them for now (kept thin).
            personalEvents = personalRepo.observeAll().first(),
            studySessions = studyRepo.observeAll().first(),
            exams = examRepo.observeAll().first(),
            captures = captureRepo.observeAll().first()
        )
        val text = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            ?: return false
        true
    }.getOrDefault(false)

    suspend fun importFrom(context: Context, uri: Uri): Boolean = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return false
        val backup = json.decodeFromString<LamlaBackup>(text)
        backup.semesters.forEach { semesterRepo.upsert(it) }
        backup.lecturers.forEach { lecturerRepo.upsert(it) }
        backup.courses.forEach { courseRepo.upsert(it) }
        backup.classSessions.forEach { classRepo.upsert(it) }
        backup.deadlines.forEach { deadlineRepo.upsert(it) }
        backup.questions.forEach { questionRepo.upsert(it) }
        backup.personalEvents.forEach { personalRepo.upsert(it) }
        backup.studySessions.forEach { studyRepo.upsert(it) }
        backup.exams.forEach { examRepo.upsert(it) }
        backup.captures.forEach { captureRepo.upsert(it) }
        engine.rescheduleAll()
        true
    }.getOrDefault(false)
}
