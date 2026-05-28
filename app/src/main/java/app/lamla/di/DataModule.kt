package app.lamla.di

import android.content.Context
import androidx.room.Room
import app.lamla.data.local.AppDatabase
import app.lamla.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "lamla.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSemesterDao(db: AppDatabase): SemesterDao = db.semesterDao()
    @Provides fun provideLecturerDao(db: AppDatabase): LecturerDao = db.lecturerDao()
    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun provideClassSessionDao(db: AppDatabase): ClassSessionDao = db.classSessionDao()
    @Provides fun provideDeadlineDao(db: AppDatabase): DeadlineDao = db.deadlineDao()
    @Provides fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()
    @Provides fun providePersonalEventDao(db: AppDatabase): PersonalEventDao = db.personalEventDao()
    @Provides fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()
    @Provides fun provideCaptureDao(db: AppDatabase): CaptureDao = db.captureDao()
    @Provides fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
}
