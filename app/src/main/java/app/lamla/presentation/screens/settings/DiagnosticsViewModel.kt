package app.lamla.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.*
import app.lamla.domain.usecase.UpcomingAlarms
import app.lamla.notifications.AlarmScheduler
import app.lamla.notifications.RescheduleAllWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class DiagnosticsUiState(
    val upcoming: List<UpcomingAlarms.Entry> = emptyList(),
    val lastRescheduleAt: Long = 0L,
    val lastBootAt: Long = 0L,
    val canScheduleExact: Boolean = true,
    val totalUpcoming: Int = 0
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
    courseRepo: CourseRepository,
    classRepo: ClassSessionRepository,
    deadlineRepo: DeadlineRepository,
    examRepo: ExamRepository,
    lecturerRepo: LecturerRepository,
    questionRepo: QuestionRepository,
    studyRepo: StudySessionRepository,
    semesterRepo: SemesterRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /**
     * Combine has a 5-flow type-level limit. We fold the noisy repos into
     * pre-aggregated data classes to stay under it while still keeping the
     * UI live-reactive (no manual refresh needed when a class is added).
     */
    private data class CoreData(
        val courses: Map<Long, app.lamla.domain.model.Course>,
        val classes: List<app.lamla.domain.model.ClassSession>,
        val deadlines: List<app.lamla.domain.model.Deadline>
    )
    private data class ExtData(
        val exams: List<app.lamla.domain.model.Exam>,
        val lecturers: List<app.lamla.domain.model.Lecturer>,
        val studies: List<app.lamla.domain.model.StudySession>,
        val activeSem: app.lamla.domain.model.Semester?
    )

    val state: StateFlow<DiagnosticsUiState> = combine(
        combine(
            courseRepo.observeAll(),
            classRepo.observeAll(),
            deadlineRepo.observePending()
        ) { c, cl, d -> CoreData(c.associateBy { it.id }, cl, d) },
        combine(
            examRepo.observeAll(),
            lecturerRepo.observeAll(),
            studyRepo.observeAll(),
            semesterRepo.observeActive()
        ) { e, l, s, sm -> ExtData(e, l, s, sm) },
        prefs.lastRescheduleAt,
        prefs.lastBootAt
    ) { core, ext, lastReschedule, lastBoot ->
        // Build the question-count map for office-hours subtitle
        val questionCounts = ext.lecturers.associate { lec ->
            lec.id to runCatching { questionRepo.pendingForLecturer(lec.id).size }.getOrDefault(0)
        }

        val upcoming = UpcomingAlarms.compute(
            now = Instant.now(),
            zone = zone,
            classSessions = core.classes,
            deadlines = core.deadlines,
            exams = ext.exams,
            lecturers = ext.lecturers,
            studySessions = ext.studies,
            coursesById = core.courses,
            activeSemester = ext.activeSem,
            questionsByLecturerId = questionCounts
        )

        DiagnosticsUiState(
            upcoming = upcoming.take(30),
            totalUpcoming = upcoming.size,
            lastRescheduleAt = lastReschedule,
            lastBootAt = lastBoot,
            canScheduleExact = alarmScheduler.canScheduleExact()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DiagnosticsUiState())

    /**
     * "Run reschedule now" button. Enqueues the worker with REPLACE so
     * back-to-back taps coalesce. The user sees the lastRescheduleAt field
     * tick forward within ~1 second.
     */
    fun runRescheduleNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleAllWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(RescheduleAllWorker.KEY_TRIGGER, RescheduleAllWorker.TRIGGER_MANUAL)
                        .build()
                )
                .build()
        )
    }

    companion object {
        private const val WORK_NAME = "lamla-diagnostics-manual-refresh"
    }
}
