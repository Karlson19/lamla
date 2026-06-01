package app.lamla.domain.usecase

import app.lamla.domain.model.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Materializes the set of reminders that the engine has (or would have)
 * scheduled, sorted by trigger time. Pure function - useful for the
 * Diagnostics screen and for unit tests of the reschedule math.
 *
 * This duplicates the per-row scheduling math from ReminderEngine because
 * AlarmManager doesn't expose its queue. Two ways to keep them in sync:
 *   - Same conventions in both (next-occurrence rule, offset semantics)
 *   - Run AlarmEngine smoke tests against this calculator
 *
 * If the two ever diverge, this is the canonical "what should be scheduled"
 * answer - the engine is what actually pokes AlarmManager but it derives
 * from the same data.
 */
object UpcomingAlarms {

    data class Entry(
        val triggerAtEpochMs: Long,
        val kind: Kind,
        val title: String,
        val subtitle: String,
        val courseColorArgb: Int? = null,
        val offsetMinutes: Int
    ) {
        enum class Kind { Class, OfficeHours, Deadline, DeadlineImminent, Exam, StudySession }
    }

    fun compute(
        now: Instant,
        zone: ZoneId,
        classSessions: List<ClassSession>,
        deadlines: List<Deadline>,
        exams: List<Exam>,
        lecturers: List<Lecturer>,
        studySessions: List<StudySession>,
        coursesById: Map<Long, Course>,
        activeSemester: Semester?,
        questionsByLecturerId: Map<Long, Int>
    ): List<Entry> {
        val results = mutableListOf<Entry>()

        // Helper - is now within the active semester window?
        val inActiveSemester = activeSemester == null ||
            run {
                val endOfLastDay = activeSemester.endDateEpochMs + 24L * 60 * 60_000
                now.toEpochMilli() in activeSemester.startDateEpochMs..endOfLastDay
            }

        // Classes - only if semester is current
        if (inActiveSemester) {
            classSessions.forEach { session ->
                val course = coursesById[session.courseId] ?: return@forEach
                val nextStart = nextWeeklyStart(now, zone, session.dayOfWeek, session.startMinutes) ?: return@forEach
                session.reminderOffsetsMinutes.forEach { offset ->
                    val trigger = nextStart.minusSeconds(offset * 60L)
                    if (trigger.isAfter(now)) {
                        results += Entry(
                            triggerAtEpochMs = trigger.toEpochMilli(),
                            kind = Entry.Kind.Class,
                            title = "${course.code} • ${course.name}",
                            subtitle = "${session.venue.ifBlank { "Venue TBA" }} · ${
                                if (offset == 0) "starting now" else "$offset min before"
                            }",
                            courseColorArgb = course.colorArgb,
                            offsetMinutes = offset
                        )
                    }
                }
            }
        }

        // Deadlines - only pending
        deadlines.filter { it.status == DeadlineStatus.Pending }.forEach { deadline ->
            val course = coursesById[deadline.courseId]
            deadline.reminderOffsetsMinutes.forEach { offset ->
                val trigger = Instant.ofEpochMilli(deadline.dueAtEpochMs - offset * 60_000L)
                if (trigger.isAfter(now)) {
                    val imminent = offset <= 60
                    results += Entry(
                        triggerAtEpochMs = trigger.toEpochMilli(),
                        kind = if (imminent) Entry.Kind.DeadlineImminent else Entry.Kind.Deadline,
                        title = (course?.let { "${it.code}: " } ?: "") + deadline.title,
                        subtitle = "${deadline.weightPercent.toInt()}% · ${humanLeadTime(offset)} before",
                        courseColorArgb = course?.colorArgb,
                        offsetMinutes = offset
                    )
                }
            }
        }

        // Exams
        exams.forEach { exam ->
            val course = coursesById[exam.courseId] ?: return@forEach
            listOf(7 * 24 * 60, 24 * 60, 60, 0).forEach { offset ->
                val trigger = Instant.ofEpochMilli(exam.examDateEpochMs - offset * 60_000L)
                if (trigger.isAfter(now)) {
                    results += Entry(
                        triggerAtEpochMs = trigger.toEpochMilli(),
                        kind = Entry.Kind.Exam,
                        title = "${course.code} exam",
                        subtitle = "${exam.venue.ifBlank { "Venue TBA" }} · ${humanLeadTime(offset)} before",
                        courseColorArgb = course.colorArgb,
                        offsetMinutes = offset
                    )
                }
            }
        }

        // Office hours - only if semester current
        if (inActiveSemester) {
            lecturers.forEach { lec ->
                lec.officeHours.forEach { slot ->
                    val nextStart = nextWeeklyStart(now, zone, slot.dayOfWeek, slot.startMinutes) ?: return@forEach
                    val trigger = nextStart.minusSeconds(30 * 60L)
                    if (trigger.isAfter(now)) {
                        val openQ = questionsByLecturerId[lec.id] ?: 0
                        results += Entry(
                            triggerAtEpochMs = trigger.toEpochMilli(),
                            kind = Entry.Kind.OfficeHours,
                            title = "${lec.name} • office hours",
                            subtitle = if (openQ == 0) "30 min before"
                            else "30 min before · $openQ pending question${if (openQ == 1) "" else "s"}",
                            offsetMinutes = 30
                        )
                    }
                }
            }
        }

        // Study sessions (one-time)
        studySessions.filter { it.completedAtEpochMs == null }.forEach { session ->
            val course = session.courseId?.let { coursesById[it] }
            listOf(5, 0).forEach { offset ->
                val trigger = Instant.ofEpochMilli(session.scheduledStartEpochMs - offset * 60_000L)
                if (trigger.isAfter(now)) {
                    results += Entry(
                        triggerAtEpochMs = trigger.toEpochMilli(),
                        kind = Entry.Kind.StudySession,
                        title = course?.let { "${it.code} • ${it.name}" } ?: "Study session",
                        subtitle = if (offset == 0) "starting now" else "5 min before",
                        courseColorArgb = course?.colorArgb,
                        offsetMinutes = offset
                    )
                }
            }
        }

        return results.sortedBy { it.triggerAtEpochMs }
    }

    private fun nextWeeklyStart(
        now: Instant,
        zone: ZoneId,
        day: DayOfWeek,
        startMinutes: Int
    ): Instant {
        val today = LocalDate.ofInstant(now, zone)
        val start = LocalTime.of(startMinutes / 60, startMinutes % 60)
        val targetDate = if (today.dayOfWeek == day) {
            val nowLt = LocalTime.ofInstant(now, zone)
            if (nowLt < start) today else today.with(TemporalAdjusters.next(day))
        } else {
            today.with(TemporalAdjusters.next(day))
        }
        return LocalDateTime.of(targetDate, start).atZone(zone).toInstant()
    }

    private fun humanLeadTime(minutes: Int): String = when {
        minutes <= 0 -> "right at start"
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> "${minutes / 60}h"
        else -> "${minutes / (24 * 60)}d"
    }
}
