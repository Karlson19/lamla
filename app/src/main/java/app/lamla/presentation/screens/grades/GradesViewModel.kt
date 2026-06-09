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
import app.lamla.domain.usecase.GradeProjection.ProjectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val projectedCredits: Int = 0,
    /**
     * Active "what-if" overrides: courseId → the hypothetical mark the user dragged to.
     * Drives the slider state and the live re-projection. Empty = showing real projection.
     */
    val simulatedMarks: Map<Long, Float> = emptyMap(),
    /** The goal-driven plan: required effort per course to hit [targetCwa]. */
    val targetPlan: GradeProjection.TargetPlan? = null,
    /** The CWA goal currently in effect (explicit or auto next-class). */
    val targetCwa: Float? = null,
    /** True when the user set the goal themselves; false when we auto-picked next class up. */
    val targetIsCustom: Boolean = false
) {
    val hasAnyGrades: Boolean get() = courseGrades.any { it.standing.gradedWeight > 0f }
    /** True while any course is being explored hypothetically. */
    val isSimulating: Boolean get() = simulatedMarks.isNotEmpty()
}

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    private val deadlineRepo: DeadlineRepository,
    private val semesterRepo: SemesterRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    /** Active "what-if" overrides (courseId → hypothetical mark). Empty = real projection. */
    private val simulatedMarks = MutableStateFlow<Map<Long, Float>>(emptyMap())

    /**
     * The real, data-driven half of the screen: courses, standings and the credited
     * marks that feed the average. Folded in one combine (the 5-flow cap) so the cheap,
     * frequently-changing simulation channel can layer on top without re-reading Room.
     */
    private data class BaseGrades(
        val semesterName: String?,
        val grades: List<CourseGrade>,
        val semesterMarks: List<GradeProjection.CreditedMark>,
        val priorCwa: Float?,
        val priorCredits: Int,
        val projectedCredits: Int
    )

    private val baseGrades = combine(
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
        BaseGrades(
            semesterName = semester?.name,
            grades = grades,
            semesterMarks = semesterMarks,
            priorCwa = priorCwa,
            priorCredits = priorCredits,
            projectedCredits = projectable.sumOf { it.course.creditHours }
        )
    }

    val state: StateFlow<GradesUiState> = combine(baseGrades, simulatedMarks, prefs.gradeTargetCwa) { base, sims, explicitTarget ->
        // Drop overrides for courses that are no longer projectable (e.g. a grade was
        // removed) so the simulation can't reference a course the user can't see.
        val liveSims = sims.filterKeys { id -> base.semesterMarks.any { it.courseId == id } }

        val projectedSwa = (GradeProjection.weightedAverage(base.semesterMarks, liveSims)
            as? GradeProjection.ProjectionResult.Success)?.value
        val projectedCwa = (GradeProjection.projectCwa(base.priorCwa, base.priorCredits, base.semesterMarks, liveSims)
            as? GradeProjection.ProjectionResult.Success)?.value
        val projectedClass = projectedCwa?.let { GradeProjection.classOf(it) }
        val nextTarget = projectedCwa?.let { GradeProjection.nextClassTarget(it) }
        val requiredSwa = nextTarget?.let {
            (GradeProjection.requiredSwaForTargetCwa(it, base.priorCwa, base.priorCredits, base.projectedCredits)
                as? GradeProjection.ProjectionResult.Success)?.value
        }

        // --- Target engine ---------------------------------------------------
        // The plan reasons over *real* banked marks (not what-if sims), so the goal
        // doesn't lurch around while the user drags a slider. The goal in effect is
        // the user's explicit pick, or—failing that—the next class of degree up from
        // where they actually stand.
        val realCwa = (GradeProjection.projectCwa(base.priorCwa, base.priorCredits, base.semesterMarks)
            as? GradeProjection.ProjectionResult.Success)?.value
        val resolvedTarget = explicitTarget
            ?: realCwa?.let { GradeProjection.nextClassTarget(it) ?: GradeProjection.DegreeClass.First.minCwa }
            ?: GradeProjection.DegreeClass.SecondUpper.minCwa
        val targetInputs = base.grades.map { cg ->
            GradeProjection.TargetCourseInput(
                courseId = cg.course.id,
                code = cg.course.code,
                name = cg.course.name,
                colorArgb = cg.course.colorArgb,
                credits = cg.course.creditHours,
                gradedWeight = cg.standing.gradedWeight,
                pointsEarned = cg.standing.pointsEarned
            )
        }
        val targetPlan = GradeProjection.planForTarget(resolvedTarget, base.priorCwa, base.priorCredits, targetInputs)

        GradesUiState(
            loading = false,
            semesterName = base.semesterName,
            courseGrades = base.grades,
            priorCwa = base.priorCwa,
            priorCredits = base.priorCredits,
            projectedSwa = projectedSwa,
            projectedCwa = projectedCwa,
            projectedClass = projectedClass,
            nextClassTarget = nextTarget,
            requiredSwaForNextClass = requiredSwa,
            projectedCredits = base.projectedCredits,
            simulatedMarks = liveSims,
            targetPlan = targetPlan,
            targetCwa = resolvedTarget,
            targetIsCustom = explicitTarget != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GradesUiState())

    fun setPriorStanding(cwa: Float?, credits: Int) {
        viewModelScope.launch { prefs.setPriorStanding(cwa, credits) }
    }

    /** Set the CWA goal the Target Engine plans toward. Null reverts to auto next-class. */
    fun setTargetCwa(cwa: Float?) {
        viewModelScope.launch { prefs.setGradeTargetCwa(cwa) }
    }

    /** Drag a course to a hypothetical mark and watch the CWA re-project live. */
    fun simulateMark(courseId: Long, mark: Float) {
        simulatedMarks.update { it + (courseId to mark.coerceIn(0f, 100f)) }
    }

    /** Drop one course's what-if override, snapping it back to its real projection. */
    fun clearSimulation(courseId: Long) {
        simulatedMarks.update { it - courseId }
    }

    /** Exit what-if mode entirely. */
    fun resetSimulation() {
        simulatedMarks.value = emptyMap()
    }
}
