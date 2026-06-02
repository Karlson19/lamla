package app.lamla.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.lamla.data.local.dao.*
import app.lamla.data.local.entities.*

@Database(
    entities = [
        SemesterEntity::class,
        LecturerEntity::class,
        CourseEntity::class,
        ClassSessionEntity::class,
        DeadlineEntity::class,
        QuestionEntity::class,
        PersonalEventEntity::class,
        StudySessionEntity::class,
        CaptureEntity::class,
        ExamEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun lecturerDao(): LecturerDao
    abstract fun courseDao(): CourseDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun questionDao(): QuestionDao
    abstract fun personalEventDao(): PersonalEventDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun captureDao(): CaptureDao
    abstract fun examDao(): ExamDao
}
