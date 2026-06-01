package app.lamla.data.repo

import app.lamla.data.local.dao.*
import app.lamla.data.local.toDomain
import app.lamla.data.local.toEntity
import app.lamla.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositories.
 *
 * One per aggregate root. Each one:
 *   - Owns DAO ↔ domain mapping (no Room types leak past this layer).
 *   - Returns [Flow] for observations, suspending fun for one-shot reads/writes.
 *   - For writes that touch the alarm scheduler (class sessions, deadlines, exams),
 *     the *use-case* layer (see [app.lamla.domain.usecase]) wraps repo writes with
 *     alarm cancel/reschedule. Keeps the data layer pure.
 */

@Singleton
class CourseRepository @Inject constructor(private val dao: CourseDao) {
    fun observeAll(): Flow<List<Course>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeForSemester(semesterId: Long): Flow<List<Course>> =
        dao.observeForSemester(semesterId).map { it.map { e -> e.toDomain() } }
    fun observe(id: Long): Flow<Course?> = dao.observe(id).map { it?.toDomain() }
    fun observeForLecturer(lecturerId: Long): Flow<List<Course>> =
        dao.observeForLecturer(lecturerId).map { it.map { e -> e.toDomain() } }
    suspend fun get(id: Long): Course? = dao.get(id)?.toDomain()
    suspend fun upsert(course: Course): Long = dao.upsert(course.toEntity())
    suspend fun delete(course: Course) = dao.delete(course.toEntity())
}

@Singleton
class ClassSessionRepository @Inject constructor(private val dao: ClassSessionDao) {
    fun observeAll(): Flow<List<ClassSession>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeForDay(day: java.time.DayOfWeek): Flow<List<ClassSession>> =
        dao.observeForDay(day).map { it.map { e -> e.toDomain() } }
    fun observeForCourse(courseId: Long): Flow<List<ClassSession>> =
        dao.observeForCourse(courseId).map { it.map { e -> e.toDomain() } }
    suspend fun get(id: Long): ClassSession? = dao.get(id)?.toDomain()
    suspend fun all(): List<ClassSession> = dao.all().map { it.toDomain() }
    suspend fun upsert(session: ClassSession): Long = dao.upsert(session.toEntity())
    suspend fun delete(session: ClassSession) = dao.delete(session.toEntity())
}

@Singleton
class DeadlineRepository @Inject constructor(private val dao: DeadlineDao) {
    fun observeAll(): Flow<List<Deadline>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeUpcoming(now: Long = System.currentTimeMillis()): Flow<List<Deadline>> =
        dao.observeUpcoming(DeadlineStatus.Pending, now).map { it.map { e -> e.toDomain() } }
    fun observePending(): Flow<List<Deadline>> =
        dao.observeByStatus(DeadlineStatus.Pending).map { it.map { e -> e.toDomain() } }
    fun observeForCourse(courseId: Long): Flow<List<Deadline>> =
        dao.observeForCourse(courseId).map { it.map { e -> e.toDomain() } }
    suspend fun get(id: Long): Deadline? = dao.get(id)?.toDomain()
    suspend fun allPending(): List<Deadline> = dao.allPending().map { it.toDomain() }
    suspend fun upsert(deadline: Deadline): Long = dao.upsert(deadline.toEntity())
    suspend fun delete(deadline: Deadline) = dao.delete(deadline.toEntity())
    suspend fun setStatus(id: Long, status: DeadlineStatus) = dao.setStatus(id, status)
}

@Singleton
class LecturerRepository @Inject constructor(private val dao: LecturerDao) {
    fun observeAll(): Flow<List<Lecturer>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observe(id: Long): Flow<Lecturer?> = dao.observe(id).map { it?.toDomain() }
    suspend fun get(id: Long): Lecturer? = dao.get(id)?.toDomain()
    suspend fun upsert(lecturer: Lecturer): Long = dao.upsert(lecturer.toEntity())
    suspend fun delete(lecturer: Lecturer) = dao.delete(lecturer.toEntity())
}

@Singleton
class QuestionRepository @Inject constructor(private val dao: QuestionDao) {
    fun observeForLecturer(lecturerId: Long): Flow<List<Question>> =
        dao.observeForLecturer(lecturerId).map { it.map { e -> e.toDomain() } }
    suspend fun pendingForLecturer(lecturerId: Long): List<Question> =
        dao.pendingForLecturer(lecturerId).map { it.toDomain() }
    suspend fun all(): List<Question> = dao.all().map { it.toDomain() }
    suspend fun upsert(question: Question): Long = dao.upsert(question.toEntity())
    suspend fun markAnswered(id: Long, answeredAt: Long = System.currentTimeMillis()) =
        dao.markAnswered(id, answeredAt)
    suspend fun delete(question: Question) = dao.delete(question.toEntity())
}

@Singleton
class PersonalEventRepository @Inject constructor(private val dao: PersonalEventDao) {
    fun observeAll(): Flow<List<PersonalEvent>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeBetween(from: Long, to: Long): Flow<List<PersonalEvent>> =
        dao.observeBetween(from, to).map { it.map { e -> e.toDomain() } }
    suspend fun all(): List<PersonalEvent> = dao.all().map { it.toDomain() }
    suspend fun upsert(event: PersonalEvent): Long = dao.upsert(event.toEntity())
    suspend fun delete(event: PersonalEvent) = dao.delete(event.toEntity())
}

@Singleton
class StudySessionRepository @Inject constructor(private val dao: StudySessionDao) {
    fun observeAll(): Flow<List<StudySession>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeBetween(from: Long, to: Long): Flow<List<StudySession>> =
        dao.observeBetween(from, to).map { it.map { e -> e.toDomain() } }
    fun observeMinutesPerCourse(from: Long, to: Long): Flow<Map<Long?, Int>> =
        dao.observeMinutesPerCourse(from, to).map { it.associate { row -> row.courseId to row.minutes } }
    suspend fun get(id: Long): StudySession? = dao.get(id)?.toDomain()
    suspend fun upsert(session: StudySession): Long = dao.upsert(session.toEntity())
    suspend fun delete(session: StudySession) = dao.delete(session.toEntity())
}

@Singleton
class CaptureRepository @Inject constructor(private val dao: CaptureDao) {
    fun observeAll(): Flow<List<Capture>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeForCourse(courseId: Long): Flow<List<Capture>> =
        dao.observeForCourse(courseId).map { it.map { e -> e.toDomain() } }
    suspend fun upsert(capture: Capture): Long = dao.upsert(capture.toEntity())
    suspend fun delete(capture: Capture) = dao.delete(capture.toEntity())
}

@Singleton
class ExamRepository @Inject constructor(private val dao: ExamDao) {
    fun observeAll(): Flow<List<Exam>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeUpcoming(now: Long = System.currentTimeMillis()): Flow<List<Exam>> =
        dao.observeUpcoming(now).map { it.map { e -> e.toDomain() } }
    suspend fun get(id: Long): Exam? = dao.get(id)?.toDomain()
    suspend fun upsert(exam: Exam): Long = dao.upsert(exam.toEntity())
    suspend fun delete(exam: Exam) = dao.delete(exam.toEntity())
}

@Singleton
class SemesterRepository @Inject constructor(private val dao: SemesterDao) {
    fun observeAll(): Flow<List<Semester>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeActive(): Flow<Semester?> = dao.observeActive().map { it?.toDomain() }
    suspend fun active(): Semester? = dao.active()?.toDomain()
    suspend fun upsert(semester: Semester): Long = dao.upsert(semester.toEntity())
    suspend fun setActive(id: Long) = dao.setActive(id)
}
