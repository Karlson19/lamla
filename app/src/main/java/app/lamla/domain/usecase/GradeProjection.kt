package app.lamla.domain.usecase

import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import kotlin.math.abs

/**
 * CWA projection (KNUST).
 *
 * KNUST grades on a Cumulative Weighted Average: every course produces a mark out
 * of 100, courses are averaged by credit hours, and that average accumulates across
 * semesters. There are no grade points — the raw weighted mark *is* the metric, and
 * it maps directly to a class of degree.
 *
 *   course mark   = Σ over graded assessments of  (score% × weight) / 100
 *   SWA (semester) = Σ(mark × credits) / Σ(credits)   over that semester
 *   CWA (cumulative) = same, over every semester taken
 *
 * This object is the pure, side-effect-free math. It answers three questions:
 *   1. Where do I stand in a course right now? ([projectCourse])
 *   2. What do I need on what's left to hit a target? ([requiredOnRemaining])
 *   3. Where will my CWA land, and what semester average gets me to a goal?
 *      ([projectCwa], [requiredSwaForTargetCwa])
 *
 * "Projection" is the point: a student already knows their CWA. The value is the
 * forward look — given the marks banked so far, what's still reachable.
 */
object GradeProjection {

    /** KNUST class-of-degree boundaries on a 0..100 CWA. */
    enum class DegreeClass(val label: String, val minCwa: Float) {
        First("First Class", 70f),
        SecondUpper("Second Class (Upper)", 60f),
        SecondLower("Second Class (Lower)", 50f),
        Third("Third Class", 40f),
        Fail("Fail", 0f)
    }

    fun classOf(cwa: Float): DegreeClass = when {
        cwa >= DegreeClass.First.minCwa -> DegreeClass.First
        cwa >= DegreeClass.SecondUpper.minCwa -> DegreeClass.SecondUpper
        cwa >= DegreeClass.SecondLower.minCwa -> DegreeClass.SecondLower
        cwa >= DegreeClass.Third.minCwa -> DegreeClass.Third
        else -> DegreeClass.Fail
    }

    /** The CWA you'd need to reach the next class up, or null if already First. */
    fun nextClassTarget(cwa: Float): Float? = when (classOf(cwa)) {
        DegreeClass.First -> null
        DegreeClass.SecondUpper -> DegreeClass.First.minCwa
        DegreeClass.SecondLower -> DegreeClass.SecondUpper.minCwa
        DegreeClass.Third -> DegreeClass.SecondLower.minCwa
        DegreeClass.Fail -> DegreeClass.Third.minCwa
    }

    // --- Per-course standing ---------------------------------------------------

    /**
     * Where a course stands given its assessments (deadlines carrying a weight, some
     * graded). All weights are "percentage points of the final course mark."
     */
    data class CourseStanding(
        /** Sum of weights of assessments that have a score. */
        val gradedWeight: Float,
        /** Sum of weights of every assessment entered (graded + not). Ideally ~100. */
        val definedWeight: Float,
        /** Defined-but-ungraded weight: what you can still influence. */
        val remainingWeight: Float,
        /** Points banked so far, out of 100. */
        val pointsEarned: Float,
        /** Your average mark on what's been graded so far (0..100), or null. */
        val markOnGraded: Float?,
        /** True once every entered assessment has a score. */
        val isFullyGraded: Boolean,
        /** True when entered weights sum to ~100 — otherwise projections are partial. */
        val coverageComplete: Boolean
    ) {
        /** Final course mark if you score [percentOnRemaining] on every remaining assessment. */
        fun projectedMark(percentOnRemaining: Float): Float =
            (pointsEarned + remainingWeight * (percentOnRemaining / 100f)).coerceIn(0f, 100f)

        /** The mark locked in even if you scored zero on everything left. */
        val floorMark: Float get() = pointsEarned

        /** The best still achievable (100% on everything remaining). */
        val ceilingMark: Float get() = (pointsEarned + remainingWeight).coerceAtMost(100f)
    }

    fun projectCourse(assessments: List<Deadline>): CourseStanding {
        val definedWeight = assessments.sumOf { it.weightPercent.toDouble() }.toFloat()
        val graded = assessments.filter { it.isGraded }
        val gradedWeight = graded.sumOf { it.weightPercent.toDouble() }.toFloat()
        val pointsEarned = graded.sumOf { d ->
            val pct = d.scorePercent ?: 0f
            (pct / 100.0) * d.weightPercent
        }.toFloat()
        val remainingWeight = (definedWeight - gradedWeight).coerceAtLeast(0f)
        return CourseStanding(
            gradedWeight = gradedWeight,
            definedWeight = definedWeight,
            remainingWeight = remainingWeight,
            pointsEarned = pointsEarned,
            markOnGraded = if (gradedWeight > 0f) (pointsEarned / gradedWeight) * 100f else null,
            isFullyGraded = definedWeight > 0f && remainingWeight <= 0.01f,
            coverageComplete = abs(definedWeight - 100f) <= 0.5f
        )
    }

    /** Result of "what do I need on what's left to hit [target]". */
    sealed interface Required {
        /** Need at least this average (0..100) on the remaining assessments. */
        data class Score(val percent: Float) : Required
        /** Target already banked — you can't lose it. */
        data object Secured : Required
        /** Unreachable even scoring 100% on everything left. */
        data object Impossible : Required
        /** Nothing left to grade; the mark is final. */
        data object NoneRemaining : Required
    }

    fun requiredOnRemaining(standing: CourseStanding, targetMark: Float): Required {
        if (standing.remainingWeight <= 0.01f) return Required.NoneRemaining
        val need = (targetMark - standing.pointsEarned) / standing.remainingWeight * 100f
        return when {
            need <= 0f -> Required.Secured
            need > 100f -> Required.Impossible
            else -> Required.Score(need)
        }
    }

    // --- Across courses: SWA & CWA --------------------------------------------

    /** A (mark, creditHours) pair for one course feeding an average. */
    data class CreditedMark(val mark: Float, val credits: Int)

    fun creditedMark(course: Course, mark: Float) = CreditedMark(mark, course.creditHours)

    /** Credit-weighted average of course marks, or null if no credits. */
    fun weightedAverage(marks: List<CreditedMark>): Float? {
        val totalCredits = marks.sumOf { it.credits }
        if (totalCredits == 0) return null
        val sum = marks.sumOf { it.mark.toDouble() * it.credits }
        return (sum / totalCredits).toFloat()
    }

    /**
     * Project the new CWA by layering this semester's (projected) marks on top of a
     * known prior standing. [priorCwa]/[priorCredits] are what the student has already
     * accumulated (they know these); pass priorCwa = null for a first-semester student.
     */
    fun projectCwa(priorCwa: Float?, priorCredits: Int, semester: List<CreditedMark>): Float? {
        val semCredits = semester.sumOf { it.credits }
        val semPoints = semester.sumOf { it.mark.toDouble() * it.credits }
        val baseCredits = if (priorCwa != null) priorCredits else 0
        val basePoints = if (priorCwa != null) priorCwa.toDouble() * priorCredits else 0.0
        val total = baseCredits + semCredits
        if (total == 0) return null
        return ((basePoints + semPoints) / total).toFloat()
    }

    /**
     * The semester average (SWA) needed across [semesterCredits] credits this semester
     * to move the cumulative average to [targetCwa], given the prior standing. May
     * return a value > 100 (target unreachable this semester) or ≤ 0 (already there) —
     * callers decide how to message that.
     */
    fun requiredSwaForTargetCwa(
        targetCwa: Float,
        priorCwa: Float?,
        priorCredits: Int,
        semesterCredits: Int
    ): Float? {
        if (semesterCredits == 0) return null
        val baseCredits = if (priorCwa != null) priorCredits else 0
        val basePoints = if (priorCwa != null) priorCwa.toDouble() * priorCredits else 0.0
        val total = baseCredits + semesterCredits
        return ((targetCwa.toDouble() * total - basePoints) / semesterCredits).toFloat()
    }
}
