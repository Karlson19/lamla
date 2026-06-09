package app.lamla.domain.usecase

import app.lamla.domain.model.AttendanceRecord
import app.lamla.domain.model.AttendanceStatus
import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Attendance maths — pure and side-effect free.
 *
 * Two jobs:
 *   1. Roll per-class records up into a course attendance rate.
 *   2. Run the "bunk calculator": given the whole semester's scheduled meetings and a
 *      target rate, how many classes can you still skip — or must you still attend —
 *      to finish on the right side of the line.
 *
 * Occurrence counting is schedule-derived: a weekly [ClassSession] on a given weekday
 * produces one meeting per matching calendar day inside the semester. No need to store
 * a row per meeting; we count them from the timetable.
 */
object AttendanceStats {

    /** The attendance floor most departments enforce. */
    const val DEFAULT_TARGET = 0.75f

    data class CourseAttendance(
        val course: Course,
        val present: Int,
        val late: Int,
        val absent: Int,
        val excused: Int,
        /** Scheduled meetings across the whole semester (0 when the schedule is unknown). */
        val totalPlanned: Int,
        /** Meetings whose date is on or before today (0 when unknown). */
        val heldSoFar: Int
    ) {
        /** Meetings with a non-excused verdict — the rate's denominator. */
        val marked: Int get() = present + late + absent
        /** Late still counts as turning up. */
        val attended: Int get() = present + late
        /** Attendance rate 0..1 over marked meetings, or null when nothing's logged. */
        val rate: Float? get() = if (marked > 0) attended.toFloat() / marked else null

        /**
         * How many of the *remaining* meetings you can still skip and finish the semester
         * at or above [target] (assuming you attend the rest). Null when the schedule
         * isn't known yet.
         */
        fun canStillSkip(target: Float = DEFAULT_TARGET): Int? {
            if (totalPlanned <= 0) return null
            val maxAbsencesAllowed = floor(totalPlanned * (1f - target)).toInt()
            return (maxAbsencesAllowed - absent).coerceAtLeast(0)
        }

        /**
         * The fewest of the remaining meetings you must attend to claw back to [target]
         * by semester end. Null when the schedule isn't known.
         */
        fun mustAttendOfRemaining(target: Float = DEFAULT_TARGET): Int? {
            if (totalPlanned <= 0) return null
            val remaining = (totalPlanned - heldSoFar).coerceAtLeast(0)
            val neededAttended = ceil(totalPlanned * target).toInt() - attended
            return neededAttended.coerceIn(0, remaining)
        }

        /** True once it's no longer mathematically possible to reach [target] this term. */
        fun targetUnreachable(target: Float = DEFAULT_TARGET): Boolean {
            if (totalPlanned <= 0) return false
            val maxAttainable = attended + (totalPlanned - heldSoFar).coerceAtLeast(0)
            return maxAttainable.toFloat() / totalPlanned < target
        }
    }

    /** Count calendar days in [startEpochDay, endEpochDay] (inclusive) that fall on [day]. */
    fun occurrencesBetween(day: DayOfWeek, startEpochDay: Long, endEpochDay: Long): Int {
        if (endEpochDay < startEpochDay) return 0
        val startWeekday = LocalDate.ofEpochDay(startEpochDay).dayOfWeek.value // 1..7
        val delta = ((day.value - startWeekday) + 7) % 7
        val firstMatch = startEpochDay + delta
        if (firstMatch > endEpochDay) return 0
        return ((endEpochDay - firstMatch) / 7 + 1).toInt()
    }

    fun forCourses(
        courses: List<Course>,
        sessionsByCourse: Map<Long, List<ClassSession>>,
        records: List<AttendanceRecord>,
        semesterStartEpochDay: Long?,
        semesterEndEpochDay: Long?,
        todayEpochDay: Long
    ): List<CourseAttendance> {
        val recordsByCourse = records.groupBy { it.courseId }
        return courses.map { course ->
            val recs = recordsByCourse[course.id].orEmpty()
            val sessions = sessionsByCourse[course.id].orEmpty()
            var totalPlanned = 0
            var heldSoFar = 0
            if (semesterStartEpochDay != null && semesterEndEpochDay != null) {
                val heldEnd = minOf(todayEpochDay, semesterEndEpochDay)
                sessions.forEach { s ->
                    totalPlanned += occurrencesBetween(s.dayOfWeek, semesterStartEpochDay, semesterEndEpochDay)
                    heldSoFar += occurrencesBetween(s.dayOfWeek, semesterStartEpochDay, heldEnd)
                }
            }
            CourseAttendance(
                course = course,
                present = recs.count { it.status == AttendanceStatus.Present },
                late = recs.count { it.status == AttendanceStatus.Late },
                absent = recs.count { it.status == AttendanceStatus.Absent },
                excused = recs.count { it.status == AttendanceStatus.Excused },
                totalPlanned = totalPlanned,
                heldSoFar = heldSoFar
            )
        }
    }
}
