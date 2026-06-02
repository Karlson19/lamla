package app.lamla.presentation.screens.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.DeadlineRepository
import app.lamla.data.repo.SemesterRepository
import app.lamla.domain.model.Course
import app.lamla.domain.usecase.GradeProjection
import app.lamla.domain.usecase.GradeProjection.CourseStanding
import app.lamla.domain.usecase.GradeProjection.DegreeClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One course's standing plus the forward-looking marks the projection cares about.
 *
 * [projectedMark] is "if you keep your current pace" - it assumes you score your
 * current graded average on everything still outstanding. Null when nothing is
 * graded yet (there is no pace to extrapolate from).
 */
data class CourseGrade(
    val course: Course,
    val standing: CourseStanding,
    val projectedMark: Float?
)

data class GradesUiState(
    val loading: Boolean = true,
    val semesterName: String? = null,
    val courseGrades: List<CourseGrade> = emptyList(),
    val priorCwa: Float? = null,
    val priorCredits: Int = 0,
    /** Credit-weighted projected average across this semester's projectable courses. */
    val projectedSwa: Float? = null,
    /** Cumulative projection: prior standing layered with this semester. */
    val projectedCwa: Float? = null,
    val projectedClass: DegreeClass? = null,
    /** CWA needed for the next class up, or null if already First / unprojectable. */
    val nextClassTarget: Float? = null,
    /** SWA you'd need this semester to land the next class up cumulatively. */
    val requiredSwaForNextClass: Float? = null,
    /** Credits carried by courses with enough data to project. */
    val projectedCredits: Int = 0
) {
    val hasAnyGrades: Boolean get() = courseGrades.any { it.standing.gradedWeight > 0f }
}

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    private val deadlineRepo: DeadlineRepository,
    private val semesterRepo: SemesterRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val state: StateFlow<GradesUiState> = combine(
        semesterRepo.observeActive(),
        courseRepo.observeAll(),
        deadlineRepo.observeAll(),
        prefs.priorCwa,
        prefs.priorCredits
    ) { semester, allCourses, allDeadlines, priorCwa, priorCredits ->
        // Scope to the active semester so the SWA reflects "this term".
        val courses = if (semester != null) allCourses.filter { it.semesterId == semester.id } else allCourses
        val byCourse = allDeadlines.groupBy { it.courseId }

        val grades = courses.map { course ->
            val standing = GradeProjection.projectCourse(byCourse[course.id].orEmpty())
            // Current pace = average on graded work; project that onto what's left.
            val projected = standing.markOnGraded?.let { standing.projectedMark(it) }
            CourseGrade(course = course, standing = standing, projectedMark = projected)
        }.sortedBy { it.course.code }

        val projectable = grades.filter { it.projectedMark != null }
        val semesterMarks = projectable.map {
            GradeProjection.creditedMark(it.course, it.projectedMark!!)
        }
        val projectedSwa = GradeProjection.weightedAverage(semesterMarks)
        val projectedCredits = projectable.sumOf { it.course.creditHours }
        val projectedCwa = GradeProjection.projectCwa(priorCwa, priorCredits, semesterMarks)
        val projectedClass = projectedCwa?.let { GradeProjection.classOf(it) }
        val nextTarget = projectedCwa?.let { GradeProjection.nextClassTarget(it) }
        val requiredSwa = nextTarget?.let {
            GradeProjection.requiredSwaForTargetCwa(it, priorCwa, priorCredits, projectedCredits)
        }

        GradesUiState(
            loading = false,
            semesterName = semester?.name,
            courseGrades = grades,
            priorCwa = priorCwa,
            priorCredits = priorCredits,
            projectedSwa = projectedSwa,
            projectedCwa = projectedCwa,
            projectedClass = projectedClass,
            nextClassTarget = nextTarget,
            requiredSwaForNextClass = requiredSwa,
            projectedCredits = projectedCredits
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GradesUiState())

    fun setPriorStanding(cwa: Float?, credits: Int) {
        viewModelScope.launch { prefs.setPriorStanding(cwa, credits) }
    }
}
