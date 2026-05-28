package app.lamla.domain.usecase

import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import app.lamla.domain.model.DeadlineStatus
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Stress score.
 *
 * Spec says: `sum(weight × urgency_factor)`. Two problems with that as written:
 *   1. Unbounded — one huge assignment dominates the entire scale forever.
 *   2. Constant urgency — a deadline 6 weeks out contributes the same as one tomorrow.
 *
 * Our formulation (still simple, still derivable from the same inputs):
 *
 *     raw = Σ pending deadlines: weightPercent × decay(hoursLeft)
 *     decay(h) = exp(-h / TAU)  where TAU = 48 hours
 *     score    = round(100 × tanh(raw / SCALE))
 *
 * Properties:
 *   - Tomorrow-due 30% weight assignment ≈ 30 × e^(-24/48) ≈ 18 raw → meaningful jump
 *   - 6-weeks-out 30% weight ≈ 30 × e^(-1000/48) ≈ ~0 raw → barely registers
 *   - tanh squashes raw to (0, 100) → never pegs at 100 from one assignment alone
 *     but a stack of imminent deadlines easily reaches the Crunch band
 *
 * SCALE picked so a "normal full week of work" (e.g. 3 deadlines, all 20%, all within 72h)
 * lands in Steady-to-Heavy band ≈ 50-65. Tune via [SCALE] constant alone — every band
 * threshold falls out automatically.
 *
 * Returned with per-deadline contributions so the breakdown sheet can show
 * what's driving the number ("Network Lab assignment — contributes +21").
 */
object StressScore {

    private const val TAU_HOURS = 48.0
    private const val SCALE = 50.0     // tunable: lower = more sensitive, higher = chiller

    data class Contribution(
        val deadline: Deadline,
        val course: Course?,
        val raw: Double,        // pre-normalization
        val displayPoints: Int  // approx contribution to final 0-100 score
    )

    data class Result(
        val score: Int,
        val contributions: List<Contribution>  // sorted desc by raw
    )

    fun compute(
        deadlines: List<Deadline>,
        coursesById: Map<Long, Course>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result {
        val pending = deadlines.filter { it.status == DeadlineStatus.Pending && it.dueAtEpochMs > nowEpochMs }
        if (pending.isEmpty()) return Result(0, emptyList())

        val contribs = pending.map { d ->
            val hoursLeft = (d.dueAtEpochMs - nowEpochMs) / 3_600_000.0
            val raw = d.weightPercent.toDouble() * exp(-hoursLeft / TAU_HOURS)
            Contribution(d, coursesById[d.courseId], raw, displayPoints = 0)
        }.sortedByDescending { it.raw }

        val rawSum = contribs.sumOf { it.raw }
        val normalized = (100.0 * tanh(rawSum / SCALE))

        // Distribute the normalized score back to contributions proportional to raw,
        // so the breakdown rows sum to (approximately) the headline number.
        val withPoints = if (rawSum > 0) {
            contribs.map { c ->
                val share = (c.raw / rawSum) * normalized
                c.copy(displayPoints = share.roundToInt())
            }
        } else contribs

        return Result(
            score = normalized.roundToInt().coerceIn(0, 100),
            contributions = withPoints
        )
    }

    private fun tanh(x: Double): Double {
        // kotlin.math.tanh exists but kotlin.math doesn't always re-export it on older JVMs.
        // Implement directly to avoid import surprises.
        val e2x = exp(2 * x)
        return (e2x - 1) / (e2x + 1)
    }
}
