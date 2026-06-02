package app.lamla.domain.usecase

import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import app.lamla.domain.model.DeadlineStatus
import java.time.LocalTime
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Stress score.
 *
 * Spec says: `sum(weight × urgency_factor)`. Two problems with that as written:
 *   1. Unbounded - one huge assignment dominates the entire scale forever.
 *   2. Constant urgency - a deadline 6 weeks out contributes the same as one tomorrow.
 *
 * Our formulation (still simple, still derivable from the same inputs):
 *
 *     raw = Σ pending deadlines: weightPercent × decay(hoursLeft)
 *         + classLoad: contactHoursLeftToday × CLASS_HOUR_WEIGHT
 *     decay(h) = exp(-h / TAU)  where TAU = 48 hours
 *     score    = round(100 × tanh(raw / SCALE))
 *
 * Properties:
 *   - Tomorrow-due 30% weight assignment ≈ 30 × e^(-24/48) ≈ 18 raw → meaningful jump
 *   - 6-weeks-out 30% weight ≈ 30 × e^(-1000/48) ≈ ~0 raw → barely registers
 *   - A packed teaching day matters too: a deadline-free morning with 6 contact hours
 *     ahead reads Heavy, not Chill. Each remaining lecture hour adds CLASS_HOUR_WEIGHT
 *     to raw, and that load *drains* as the day's classes finish.
 *   - tanh squashes raw to (0, 100) → never pegs at 100 from one assignment alone
 *     but a stack of imminent deadlines (or a brutal class day) easily reaches Crunch
 *
 * SCALE picked so a "normal full week of work" (e.g. 3 deadlines, all 20%, all within 72h)
 * lands in Steady-to-Heavy band ≈ 50-65. Tune via [SCALE] constant alone - every band
 * threshold falls out automatically.
 *
 * Returned with per-deadline contributions plus a class-load summary so the breakdown
 * sheet can show what's driving the number ("Network Lab assignment +21", "4 classes +18").
 */
object StressScore {

    private const val TAU_HOURS = 48.0
    private const val SCALE = 50.0          // tunable: lower = more sensitive, higher = chiller
    private const val CLASS_HOUR_WEIGHT = 5.0 // each contact hour still ahead today adds this to raw

    data class Contribution(
        val deadline: Deadline,
        val course: Course?,
        val raw: Double,        // pre-normalization
        val displayPoints: Int  // approx contribution to final 0-100 score
    )

    /** The day's teaching burden, summarised. Null when no classes remain today. */
    data class ClassLoad(
        val classCount: Int,            // classes not yet finished today
        val contactHoursRemaining: Float,
        val displayPoints: Int          // approx contribution to final 0-100 score
    )

    data class Result(
        val score: Int,
        val contributions: List<Contribution>,  // sorted desc by raw
        val classLoad: ClassLoad? = null
    )

    /**
     * @param todaysClasses the [ClassSession]s scheduled for today (any day-filtering is
     *        the caller's job); only those not yet finished add load.
     * @param nowMinutes minutes since local midnight, used to drain finished classes.
     */
    fun compute(
        deadlines: List<Deadline>,
        coursesById: Map<Long, Course>,
        todaysClasses: List<ClassSession> = emptyList(),
        nowMinutes: Int = LocalTime.now().toSecondOfDay() / 60,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result {
        val pending = deadlines.filter { it.status == DeadlineStatus.Pending && it.dueAtEpochMs > nowEpochMs }

        val contribs = pending.map { d ->
            val hoursLeft = (d.dueAtEpochMs - nowEpochMs) / 3_600_000.0
            val raw = d.weightPercent.toDouble() * exp(-hoursLeft / TAU_HOURS)
            Contribution(d, coursesById[d.courseId], raw, displayPoints = 0)
        }.sortedByDescending { it.raw }

        // Contact hours still ahead today. An in-progress class counts only its remainder,
        // so the load eases through the day rather than dropping off a cliff at one time.
        val upcoming = todaysClasses.filter { it.endMinutes > nowMinutes }
        val contactHoursRemaining = upcoming.sumOf { c ->
            val from = maxOf(c.startMinutes, nowMinutes)
            (c.endMinutes - from).coerceAtLeast(0) / 60.0
        }
        val classRaw = contactHoursRemaining * CLASS_HOUR_WEIGHT

        val deadlineRaw = contribs.sumOf { it.raw }
        val rawSum = deadlineRaw + classRaw
        if (rawSum <= 0.0) return Result(0, emptyList(), null)

        val normalized = 100.0 * tanh(rawSum / SCALE)

        // Distribute the normalized score back to each driver proportional to its raw,
        // so the breakdown rows sum to (approximately) the headline number.
        val withPoints = contribs.map { c ->
            c.copy(displayPoints = ((c.raw / rawSum) * normalized).roundToInt())
        }
        val classLoad = if (classRaw > 0.0) {
            ClassLoad(
                classCount = upcoming.size,
                contactHoursRemaining = contactHoursRemaining.toFloat(),
                displayPoints = ((classRaw / rawSum) * normalized).roundToInt()
            )
        } else null

        return Result(
            score = normalized.roundToInt().coerceIn(0, 100),
            contributions = withPoints,
            classLoad = classLoad
        )
    }

    private fun tanh(x: Double): Double {
        // kotlin.math.tanh exists but kotlin.math doesn't always re-export it on older JVMs.
        // Implement directly to avoid import surprises.
        val e2x = exp(2 * x)
        return (e2x - 1) / (e2x + 1)
    }
}
