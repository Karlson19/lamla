package app.lamla.data.local.dao

import androidx.room.*
import app.lamla.data.local.entities.*
import app.lamla.domain.model.DeadlineStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadlineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deadline: DeadlineEntity): Long

    @Update suspend fun update(deadline: DeadlineEntity)
    @Delete suspend fun delete(deadline: DeadlineEntity)

    @Query("SELECT * FROM deadlines WHERE id = :id")
    suspend fun get(id: Long): DeadlineEntity?

    @Query("SELECT * FROM deadlines ORDER BY dueAtEpochMs")
    fun observeAll(): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE status = :status ORDER BY dueAtEpochMs")
    fun observeByStatus(status: DeadlineStatus): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE courseId = :courseId ORDER BY dueAtEpochMs")
    fun observeForCourse(courseId: Long): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE status = :status AND dueAtEpochMs > :now ORDER BY dueAtEpochMs")
    fun observeUpcoming(status: DeadlineStatus, now: Long): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE status = 'Pending'")
    suspend fun allPending(): List<DeadlineEntity>

    @Query("UPDATE deadlines SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: DeadlineStatus)
}

@Dao
interface LecturerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lecturer: LecturerEntity): Long

    @Update suspend fun update(lecturer: LecturerEntity)
    @Delete suspend fun delete(lecturer: LecturerEntity)

    @Query("SELECT * FROM lecturers WHERE id = :id")
    suspend fun get(id: Long): LecturerEntity?

    @Query("SELECT * FROM lecturers WHERE id = :id")
    fun observe(id: Long): Flow<LecturerEntity?>

    @Query("SELECT * FROM lecturers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<LecturerEntity>>
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(question: QuestionEntity): Long

    @Update suspend fun update(question: QuestionEntity)
    @Delete suspend fun delete(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun get(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE lecturerId = :lecturerId ORDER BY answeredAtEpochMs IS NULL DESC, createdAtEpochMs DESC")
    fun observeForLecturer(lecturerId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE lecturerId = :lecturerId AND answeredAtEpochMs IS NULL")
    suspend fun pendingForLecturer(lecturerId: Long): List<QuestionEntity>

    @Query("UPDATE questions SET answeredAtEpochMs = :answeredAt WHERE id = :id")
    suspend fun markAnswered(id: Long, answeredAt: Long)
}

@Dao
interface PersonalEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: PersonalEventEntity): Long

    @Delete suspend fun delete(event: PersonalEventEntity)

    @Query("SELECT * FROM personal_events ORDER BY startEpochMs")
    fun observeAll(): Flow<List<PersonalEventEntity>>

    @Query("SELECT * FROM personal_events WHERE startEpochMs BETWEEN :from AND :to ORDER BY startEpochMs")
    fun observeBetween(from: Long, to: Long): Flow<List<PersonalEventEntity>>

    @Query("SELECT * FROM personal_events")
    suspend fun all(): List<PersonalEventEntity>
}

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: StudySessionEntity): Long

    @Update suspend fun update(session: StudySessionEntity)
    @Delete suspend fun delete(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun get(id: Long): StudySessionEntity?

    @Query("SELECT * FROM study_sessions ORDER BY scheduledStartEpochMs DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE scheduledStartEpochMs BETWEEN :from AND :to")
    fun observeBetween(from: Long, to: Long): Flow<List<StudySessionEntity>>

    /** Per-course minute totals between [from] and [to]. */
    @Query("""
        SELECT courseId AS courseId, SUM(actualMinutesStudied) AS minutes
        FROM study_sessions
        WHERE completedAtEpochMs IS NOT NULL
          AND completedAtEpochMs BETWEEN :from AND :to
        GROUP BY courseId
    """)
    fun observeMinutesPerCourse(from: Long, to: Long): Flow<List<CourseMinutes>>

    data class CourseMinutes(val courseId: Long?, val minutes: Int)
}

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(capture: CaptureEntity): Long

    @Delete suspend fun delete(capture: CaptureEntity)

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun get(id: Long): CaptureEntity?

    @Query("SELECT * FROM captures ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE courseId = :courseId ORDER BY createdAtEpochMs DESC")
    fun observeForCourse(courseId: Long): Flow<List<CaptureEntity>>
}

@Dao
interface ExamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exam: ExamEntity): Long

    @Update suspend fun update(exam: ExamEntity)
    @Delete suspend fun delete(exam: ExamEntity)

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun get(id: Long): ExamEntity?

    @Query("SELECT * FROM exams ORDER BY examDateEpochMs")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE examDateEpochMs >= :now ORDER BY examDateEpochMs")
    fun observeUpcoming(now: Long): Flow<List<ExamEntity>>
}

@Dao
interface SemesterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(semester: SemesterEntity): Long

    @Update suspend fun update(semester: SemesterEntity)
    @Delete suspend fun delete(semester: SemesterEntity)

    @Query("SELECT * FROM semesters ORDER BY startDateEpochMs DESC")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun active(): SemesterEntity?

    @Query("UPDATE semesters SET isActive = (id = :id)")
    suspend fun setActive(id: Long)
}
