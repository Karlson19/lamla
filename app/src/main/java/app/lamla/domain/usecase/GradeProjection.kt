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

    /**
     * A (mark, creditHours) pair for one course feeding an average. [courseId] is kept
     * so a "what-if" simulation can override this course's mark by id without disturbing
     * the rest of the semester.
     */
    data class CreditedMark(val courseId: Long, val mark: Float, val credits: Int)

    fun creditedMark(course: Course, mark: Float) = CreditedMark(course.id, mark, course.creditHours)

    /** Result of a projection that may fail due to missing data. */
    sealed interface ProjectionResult<out T> {
        data class Success<T>(val value: T) : ProjectionResult<T>
        data object MissingCredits : ProjectionResult<Nothing>
        data object Impossible : ProjectionResult<Nothing>
    }

    /**
     * Credit-weighted average of [marks]. [simulatedMarks] (courseId → mark) overrides a
     * course's mark for "what-if" exploration; absent entries use the real projected mark.
     */
    fun weightedAverage(
        marks: List<CreditedMark>,
        simulatedMarks: Map<Long, Float> = emptyMap()
    ): ProjectionResult<Float> {
        val totalCredits = marks.sumOf { it.credits }
        if (totalCredits == 0) return ProjectionResult.MissingCredits
        val sum = marks.sumOf { cm ->
            val mark = simulatedMarks[cm.courseId] ?: cm.mark
            mark.toDouble() * cm.credits
        }
        return ProjectionResult.Success((sum / totalCredits).toFloat())
    }

    /**
     * Project the new CWA by layering this semester's marks on top of a known prior
     * standing. [priorCwa]/[priorCredits] are what the student has already accumulated;
     * pass priorCwa = null for a first-semester student. [simulatedMarks] (courseId →
     * mark) overrides a course's projected mark with a "what-if" value.
     */
    fun projectCwa(
        priorCwa: Float?,
        priorCredits: Int,
        semester: List<CreditedMark>,
        simulatedMarks: Map<Long, Float> = emptyMap()
    ): ProjectionResult<Float> {
        val semCredits = semester.sumOf { it.credits }
        val semPoints = semester.sumOf { cm ->
            val mark = simulatedMarks[cm.courseId] ?: cm.mark
            mark.toDouble() * cm.credits
        }
        val baseCredits = if (priorCwa != null) priorCredits else 0
        val basePoints = if (priorCwa != null) priorCwa.toDouble() * priorCredits else 0.0
        val total = baseCredits + semCredits
        if (total == 0) return ProjectionResult.MissingCredits
        return ProjectionResult.Success(((basePoints + semPoints) / total).toFloat())
    }

    /**
     * The semester average (SWA) needed across [semesterCredits] credits this semester to
     * move the cumulative average to [targetCwa], given the prior standing. The raw value
     * is returned even when > 100 (target unreachable this semester) or ≤ 0 (already
     * there) — callers decide how to message that.
     */
    fun requiredSwaForTargetCwa(
        targetCwa: Float,
        priorCwa: Float?,
        priorCredits: Int,
        semesterCredits: Int
    ): ProjectionResult<Float> {
        if (semesterCredits == 0) return ProjectionResult.MissingCredits
        val baseCredits = if (priorCwa != null) priorCredits else 0
        val basePoints = if (priorCwa != null) priorCwa.toDouble() * priorCredits else 0.0
        val total = baseCredits + semesterCredits
        val needed = ((targetCwa.toDouble() * total - basePoints) / semesterCredits).toFloat()
        return ProjectionResult.Success(needed)
    }

    // --- Target engine: solve the effort a goal demands ------------------------

    /**
     * Per-course facts the target solver needs. [pointsEarned] and [gradedWeight] come
     * straight off a [CourseStanding]; the rest identify the course for the UI.
     */
    data class TargetCourseInput(
        val courseId: Long,
        val code: String,
        val name: String,
        val colorArgb: Int,
        val credits: Int,
        val gradedWeight: Float,
        val pointsEarned: Float
    )

    /**
     * One course inside a target plan: what's banked, what's still up for grabs, and
     * where the course finishes if you hit the plan's required effort.
     */
    data class TargetLine(
        val courseId: Long,
        val code: String,
        val name: String,
        val colorArgb: Int,
        val credits: Int,
        /** Weight (% of the 100-pt course) already graded. */
        val gradedWeight: Float,
        /** Points banked out of 100 from graded work so far. */
        val pointsEarned: Float,
        /** The rest of the course still earnable: 100 − gradedWeight. */
        val influenceableWeight: Float,
        /** Final course mark if you score the plan's required % on everything left. */
        val projectedFinishMark: Float,
        /** Nothing left to influence — this course's mark is locked. */
        val locked: Boolean
    )

    /** The verdict of a target plan. */
    sealed interface TargetOutcome {
        /** Score at least [percentOnRemaining] (0..100) on every ungraded portion left. */
        data class Reachable(val percentOnRemaining: Float) : TargetOutcome
        /** Already banked: even zero on everything left lands the goal. [floorCwa] is the worst case. */
        data class Secured(val floorCwa: Float) : TargetOutcome
        /** Unreachable this term; [bestCwa]/[bestClass] is the ceiling (100% on all that's left). */
        data class OutOfReach(val bestCwa: Float, val bestClass: DegreeClass) : TargetOutcome
        /** No credits / courses to plan over. */
        data object NoData : TargetOutcome
    }

    data class TargetPlan(
        val targetCwa: Float,
        val targetClass: DegreeClass,
        /** Semester average this plan demands. */
        val requiredSwa: Float,
        val outcome: TargetOutcome,
        val lines: List<TargetLine>
    )

    /**
     * Solve the uniform effort needed across every *ungraded* portion of this semester's
     * courses to land [targetCwa] cumulatively, given the prior standing.
     *
     * Model: each course finishes at  pointsEarned + (100 − gradedWeight) · X/100, where
     * X is a single shared "score on everything still outstanding". We solve the
     * credit-weighted CWA = [targetCwa] for X, then read each course's finish mark back.
     * Unlike [CourseStanding.remainingWeight] (only *entered* assessments), this treats the
     * whole rest of each course as still earnable — the right assumption for "what do I
     * need overall", since every course ultimately marks out of 100.
     */
    fun planForTarget(
        targetCwa: Float,
        priorCwa: Float?,
        priorCredits: Int,
        courses: List<TargetCourseInput>
    ): TargetPlan {
        val targetClass = classOf(targetCwa)
        val semCredits = courses.sumOf { it.credits }
        if (semCredits == 0) {
            return TargetPlan(targetCwa, targetClass, targetCwa, TargetOutcome.NoData, emptyList())
        }
        val baseCredits = if (priorCwa != null) priorCredits else 0
        val basePoints = if (priorCwa != null) priorCwa.toDouble() * priorCredits else 0.0
        val totalCredits = baseCredits + semCredits
        val requiredSwa = ((targetCwa.toDouble() * totalCredits - basePoints) / semCredits).toFloat()

        fun influenceable(c: TargetCourseInput) = (100f - c.gradedWeight).coerceIn(0f, 100f)

        // Σ banked points and Σ influenceable capacity, both credit-weighted.
        val bankedWeighted = courses.sumOf { it.pointsEarned.toDouble() * it.credits }
        val capacityWeighted = courses.sumOf { influenceable(it).toDouble() * it.credits }
        val neededSemPoints = requiredSwa.toDouble() * semCredits

        // X = % needed on all remaining work (one shared effort level). Null = nothing left.
        val rawX: Float? = if (capacityWeighted <= 0.0001) null
            else (100.0 * (neededSemPoints - bankedWeighted) / capacityWeighted).toFloat()

        fun cwaAt(xEff: Float): Float {
            val semPoints = courses.sumOf { c ->
                (c.pointsEarned + influenceable(c) * xEff / 100f).toDouble() * c.credits
            }
            return ((basePoints + semPoints) / totalCredits).toFloat()
        }
        fun linesAt(xEff: Float) = courses.map { c ->
            val inf = influenceable(c)
            TargetLine(
                courseId = c.courseId, code = c.code, name = c.name, colorArgb = c.colorArgb,
                credits = c.credits, gradedWeight = c.gradedWeight, pointsEarned = c.pointsEarned,
                influenceableWeight = inf,
                projectedFinishMark = (c.pointsEarned + inf * xEff / 100f).coerceIn(0f, 100f),
                locked = inf <= 0.01f
            )
        }

        val outcome: TargetOutcome = when {
            rawX == null -> {
                // Everything locked: compare the final (banked) CWA to the goal.
                val finalCwa = cwaAt(0f)
                if (finalCwa + 0.05f >= targetCwa) TargetOutcome.Secured(finalCwa)
                else TargetOutcome.OutOfReach(finalCwa, classOf(finalCwa))
            }
            rawX <= 0f -> TargetOutcome.Secured(cwaAt(0f))
            rawX > 100f -> cwaAt(100f).let { TargetOutcome.OutOfReach(it, classOf(it)) }
            else -> TargetOutcome.Reachable(rawX)
        }
        val xForLines = (rawX ?: 0f).coerceIn(0f, 100f)
        return TargetPlan(targetCwa, targetClass, requiredSwa, outcome, linesAt(xForLines))
    }
}
