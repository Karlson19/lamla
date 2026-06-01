package app.lamla.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.*
import app.lamla.domain.model.*
import app.lamla.domain.usecase.StressScore
import app.lamla.domain.usecase.TodayFlow
import app.lamla.ui.components.StressBand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    /** "Good morning" / "Good afternoon" / "Good evening" / "Burning the midnight oil". */
    val greeting: String = "",
    /** User's display name, or empty string when not set. Empty = name-less greeting. */
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val stressScore: Int = 0,
    val stressBand: StressBand = StressBand.Chill,
    val stressContributions: List<StressScore.Contribution> = emptyList(),
    val flow: List<TodayFlow.Item> = emptyList(),
    val nextClass: TodayFlow.Item.ClassItem? = null,
    val courses: Map<Long, Course> = emptyMap(),
    val activeCourseAtNow: Course? = null,
    /** Count of pending deadlines due within the next 7 days. Drives the Home shortcut. */
    val deadlinesDueThisWeek: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    courseRepo: CourseRepository,
    classRepo: ClassSessionRepository,
    deadlineRepo: DeadlineRepository,
    studyRepo: StudySessionRepository,
    personalRepo: PersonalEventRepository,
    prefs: AppPreferences
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /**
     * Combine has a 5-flow limit at the type level. We pre-fold three of the
     * five primary data flows into one to stay under that cap when we add the
     * preferences (userName) channel. Tradeoff: a single allocation per emission
     * vs. a more sprawling combine that would also work but be harder to read.
     */
    private data class CoreData(
        val courses: List<Course>,
        val classes: List<ClassSession>,
        val deadlines: List<Deadline>
    )

    val state: StateFlow<HomeUiState> = combine(
        combine(
            courseRepo.observeAll(),
            classRepo.observeAll(),
            deadlineRepo.observePending()
        ) { c, cl, d -> CoreData(c, cl, d) },
        studyRepo.observeAll(),
        personalRepo.observeAll(),
        prefs.userName
    ) { core, studies, personals, userName ->
        val today = LocalDate.now(zone)
        val coursesById = core.courses.associateBy { it.id }

        val flow = TodayFlow.build(today, zone, core.classes, core.deadlines, studies, personals, coursesById)

        val nowMins = java.time.LocalTime.now(zone).toSecondOfDay() / 60
        val nowDay = today.dayOfWeek
        val activeClass = core.classes
            .firstOrNull { it.dayOfWeek == nowDay && nowMins in it.startMinutes..it.endMinutes }
        val activeCourse = activeClass?.let { coursesById[it.courseId] }

        val nextClass = flow.filterIsInstance<TodayFlow.Item.ClassItem>()
            .firstOrNull { it.startMinutes >= nowMins }

        val stress = StressScore.compute(core.deadlines, coursesById)

        val nowMs = System.currentTimeMillis()
        val weekAheadMs = nowMs + 7L * 24 * 60 * 60_000
        val dueThisWeek = core.deadlines.count { it.dueAtEpochMs in nowMs..weekAheadMs }

        HomeUiState(
            greeting = greetingFor(nowMins),
            userName = userName,
            today = today,
            stressScore = stress.score,
            stressBand = StressBand.fromScore(stress.score),
            stressContributions = stress.contributions,
            flow = flow,
            nextClass = nextClass,
            courses = coursesById,
            activeCourseAtNow = activeCourse,
            deadlinesDueThisWeek = dueThisWeek,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    private fun greetingFor(nowMins: Int): String = when (nowMins / 60) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Burning the midnight oil"
    }
}
