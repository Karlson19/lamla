package app.lamla.notifications

import app.lamla.data.repo.*
import app.lamla.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The brain of the reminder system.
 *
 * Every domain row (class, deadline, exam, study session, office hour) has a
 * deterministic set of `Reminder`s derived from its data. This engine computes
 * those, then asks [AlarmScheduler] to schedule them.
 *
 * Cancel-on-edit story:
 *   - Each Reminder has a stableId derived from (kind, sourceId, offsetMinutes).
 *   - To re-schedule a row, we just call [scheduleForClassSession] again — it
 *     computes the *current* set of reminders (which may be different — user
 *     changed venue, day, offsets), and AlarmScheduler.schedule replaces any
 *     existing PendingIntent with the same stableId (FLAG_UPDATE_CURRENT).
 *   - For *deletions* or for offset-list shrinks (user removed "30 min before"),
 *     stale alarms would remain. So callers MUST call [cancelForClassSession]
 *     before deleting a row, and we re-cancel-then-reschedule on edit.
 *
 * Spec compliance:
 *   - Class: offsets default to [30, 10, 0] minutes before start (configurable per class).
 *   - Deadline: offsets default to [1440, 360, 60, 15] (24h, 6h, 1h, 15min before).
 *     The 1h-and-below offsets fire on the DeadlineImminent channel (MAX importance).
 *   - Study session: at start + 5 min before.
 *   - Office hours: 30 min before, body includes pending questions.
 *   - Exam: at start time (MAX importance via Exam channel).
 *
 * Weekly recurrence: classes recur weekly. We schedule only the **next** occurrence
 * for each offset. When the alarm fires, [ReminderReceiver] (in a future revision)
 * could chain-schedule the next week — but Android's alarm budget makes this fine
 * to do on app open / via the periodic refresh worker too. We re-schedule on each
 * boot, each app open, and via a weekly WorkManager job (see WeeklyReminderRefreshWorker).
 */
@Singleton
class ReminderEngine @Inject constructor(
    private val scheduler: AlarmScheduler,
    private val classSessionRepo: ClassSessionRepository,
    private val courseRepo: CourseRepository,
    private val deadlineRepo: DeadlineRepository,
    private val lecturerRepo: LecturerRepository,
    private val questionRepo: QuestionRepository,
    private val examRepo: ExamRepository,
    private val studySessionRepo: StudySessionRepository,
    private val semesterRepo: app.lamla.data.repo.SemesterRepository
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    // -- Per-row scheduling -----------------------------------------------------

    suspend fun scheduleForClassSession(session: ClassSession) {
        val course = courseRepo.get(session.courseId) ?: return
        cancelForClassSession(session)

        // Don't keep firing reminders for a finished semester. A class that
        // belongs to a semester whose end date has passed is dormant — the
        // user can resurrect it by extending the semester end date or creating
        // a new semester and importing the course. Until then: silence.
        if (!isSemesterCurrentlyActive(course.semesterId)) return

        nextClassOccurrence(session)?.let { startInstant ->
            session.reminderOffsetsMinutes.forEach { offset ->
                val trigger = startInstant.minusSeconds(offset * 60L)
                if (trigger.isAfter(Instant.now())) {
                    val body = buildString {
                        append(session.venue.ifBlank { "venue TBA" })
                        if (offset > 0) append(" • starts in $offset min")
                        else append(" • starting now")
                    }
                    scheduler.schedule(
                        Reminder.Class(
                            sourceId = session.id,
                            courseId = course.id,
                            offsetMinutes = offset,
                            triggerAtEpochMs = trigger.toEpochMilli(),
                            title = "${course.code} • ${course.name}",
                            body = body
                        )
                    )
                }
            }
        }
    }

    /**
     * A semester is "currently active" for reminder purposes if right now sits
     * between its start date and its end-date-plus-one-day (so reminders fire
     * through the final day of the semester, not just up to midnight before it).
     *
     * Note: this is NOT the same as `Semester.isActive` — that flag is "the
     * user's currently focused semester for UI display". A user could be
     * mid-semester and toggle isActive between semesters; the time check
     * remains the source of truth for "should reminders fire?".
     */
    private suspend fun isSemesterCurrentlyActive(semesterId: Long): Boolean {
        val semester = semesterRepo.observeAll().firstOrNullSafe()
            ?.firstOrNull { it.id == semesterId } ?: return true  // unknown → don't break
        val now = System.currentTimeMillis()
        val endOfLastDay = semester.endDateEpochMs + 24L * 60 * 60_000  // include the end date itself
        return now in semester.startDateEpochMs..endOfLastDay
    }

    fun cancelForClassSession(session: ClassSession) {
        // Cancel possible offsets the user might have removed (over-cancel is harmless).
        val possible = (session.reminderOffsetsMinutes + listOf(0, 5, 10, 15, 30, 60)).toSet()
        possible.forEach { offset ->
            scheduler.cancelByStableId(stableIdFor("class", session.id, offset))
        }
    }

    suspend fun scheduleForDeadline(deadline: Deadline) {
        if (deadline.status == DeadlineStatus.Done) {
            cancelForDeadline(deadline)
            return
        }
        cancelForDeadline(deadline)
        val course = courseRepo.get(deadline.courseId) ?: return
        deadline.reminderOffsetsMinutes.forEach { offset ->
            val trigger = deadline.dueAtEpochMs - offset * 60_000L
            if (trigger > System.currentTimeMillis()) {
                val imminent = offset <= 60
                val timeLabel = humanizeLeadTime(offset)
                scheduler.schedule(
                    Reminder.Deadline(
                        sourceId = deadline.id,
                        courseId = deadline.courseId,
                        offsetMinutes = offset,
                        imminent = imminent,
                        triggerAtEpochMs = trigger,
                        title = "${course.code} — ${deadline.title}",
                        body = "Due in $timeLabel · ${deadline.weightPercent.toInt()}% weight"
                    )
                )
            }
        }
    }

    fun cancelForDeadline(deadline: Deadline) {
        val possible = (deadline.reminderOffsetsMinutes + listOf(15, 60, 360, 1440)).toSet()
        possible.forEach { offset ->
            scheduler.cancelByStableId(stableIdFor("deadline", deadline.id, offset))
        }
    }

    suspend fun scheduleForExam(exam: Exam) {
        cancelForExam(exam)
        val course = courseRepo.get(exam.courseId) ?: return
        val offsets = listOf(7 * 24 * 60, 24 * 60, 60, 0)  // 1w, 1d, 1h, on-time
        offsets.forEach { offset ->
            val trigger = exam.examDateEpochMs - offset * 60_000L
            if (trigger > System.currentTimeMillis()) {
                scheduler.schedule(
                    Reminder.Exam(
                        sourceId = exam.id,
                        courseId = exam.courseId,
                        offsetMinutes = offset,
                        triggerAtEpochMs = trigger,
                        title = "${course.code} exam · ${if (offset == 0) "now" else "in ${humanizeLeadTime(offset)}"}",
                        body = "${exam.venue.ifBlank { "venue TBA" }}${if (exam.topics.isNotEmpty()) " · ${exam.topics.size} topics" else ""}"
                    )
                )
            }
        }
    }

    fun cancelForExam(exam: Exam) {
        listOf(7 * 24 * 60, 24 * 60, 60, 0).forEach { offset ->
            scheduler.cancelByStableId(stableIdFor("exam", exam.id, offset))
        }
    }

    suspend fun scheduleForStudySession(session: StudySession) {
        cancelForStudySession(session)
        val course = session.courseId?.let { courseRepo.get(it) }
        val offsets = listOf(5, 0)
        offsets.forEach { offset ->
            val trigger = session.scheduledStartEpochMs - offset * 60_000L
            if (trigger > System.currentTimeMillis()) {
                scheduler.schedule(
                    Reminder.StudySession(
                        sourceId = session.id,
                        courseId = session.courseId,
                        offsetMinutes = offset,
                        triggerAtEpochMs = trigger,
                        title = if (offset == 0) "Study time" else "Study session starts in 5 min",
                        body = course?.let { "${it.code} • ${it.name}" }
                            ?: "Tap to start Pomodoro"
                    )
                )
            }
        }
    }

    fun cancelForStudySession(session: StudySession) {
        listOf(0, 5).forEach { offset ->
            scheduler.cancelByStableId(stableIdFor("study", session.id, offset))
        }
    }

    suspend fun scheduleForLecturerOfficeHours(lecturer: Lecturer) {
        cancelForLecturerOfficeHours(lecturer)

        // Office hours are weekly recurring like classes. Only fire if the
        // user's active semester is current — a lecturer's office hours from
        // last semester shouldn't keep nagging through break.
        val activeSem = semesterRepo.active()
        if (activeSem != null) {
            val now = System.currentTimeMillis()
            val endOfLastDay = activeSem.endDateEpochMs + 24L * 60 * 60_000
            if (now !in activeSem.startDateEpochMs..endOfLastDay) return
        }

        val pendingQuestions = questionRepo.pendingForLecturer(lecturer.id)
        val preview = pendingQuestions.take(3).joinToString("\n") { it.text }

        lecturer.officeHours.forEach { slot ->
            val nextStart = nextWeeklyOccurrence(slot.dayOfWeek, slot.startMinutes)
            val trigger = nextStart.minusSeconds(30 * 60L)
            if (trigger.isAfter(Instant.now())) {
                scheduler.schedule(
                    Reminder.OfficeHours(
                        sourceId = lecturer.id,
                        pendingQuestionsPreview = preview,
                        offsetMinutes = 30,
                        triggerAtEpochMs = trigger.toEpochMilli(),
                        title = "${lecturer.name} • office hours soon",
                        body = if (pendingQuestions.isEmpty())
                            "Office hours start in 30 min"
                        else
                            "You have ${pendingQuestions.size} pending question${if (pendingQuestions.size == 1) "" else "s"}:"
                    )
                )
            }
        }
    }

    fun cancelForLecturerOfficeHours(lecturer: Lecturer) {
        scheduler.cancelByStableId(stableIdFor("office", lecturer.id, 30))
    }

    // -- Bulk reschedule (boot, app open, weekly refresh) ----------------------

    suspend fun rescheduleAll() {
        classSessionRepo.all().forEach { scheduleForClassSession(it) }
        deadlineRepo.allPending().forEach { scheduleForDeadline(it) }
        examRepo.observeUpcoming().firstOrNullSafe()?.forEach { scheduleForExam(it) }
        lecturerRepo.observeAll().firstOrNullSafe()?.forEach { scheduleForLecturerOfficeHours(it) }
        // Upcoming study sessions in next 30 days
        val now = System.currentTimeMillis()
        studySessionRepo.observeBetween(now, now + 30L * 24 * 60 * 60_000)
            .firstOrNullSafe()?.forEach { scheduleForStudySession(it) }
    }

    // -- Helpers ---------------------------------------------------------------

    private fun nextClassOccurrence(session: ClassSession): Instant? {
        val today = LocalDate.now(zone)
        val targetDate = if (today.dayOfWeek == session.dayOfWeek) {
            // If today and start time is in the future, today. Else next week.
            val nowLt = LocalTime.now(zone)
            if (nowLt < session.start) today else today.with(TemporalAdjusters.next(session.dayOfWeek))
        } else {
            today.with(TemporalAdjusters.next(session.dayOfWeek))
        }
        return LocalDateTime.of(targetDate, session.start).atZone(zone).toInstant()
    }

    private fun nextWeeklyOccurrence(day: DayOfWeek, startMinutes: Int): Instant {
        val today = LocalDate.now(zone)
        val targetDate = if (today.dayOfWeek == day) {
            val nowLt = LocalTime.now(zone)
            val start = LocalTime.of(startMinutes / 60, startMinutes % 60)
            if (nowLt < start) today else today.with(TemporalAdjusters.next(day))
        } else {
            today.with(TemporalAdjusters.next(day))
        }
        val time = LocalTime.of(startMinutes / 60, startMinutes % 60)
        return LocalDateTime.of(targetDate, time).atZone(zone).toInstant()
    }

    private fun humanizeLeadTime(minutes: Int): String = when {
        minutes <= 0 -> "now"
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> "${minutes / 60}h"
        else -> "${minutes / (24 * 60)}d"
    }
}

private fun stableIdFor(tag: String, sourceId: Long, offset: Int): Int {
    val seed = (tag.hashCode().toLong() * 31L + sourceId) * 31L + offset
    var h = seed
    h = h xor (h ushr 33); h *= -49064778989728563L
    h = h xor (h ushr 33); h *= -4265267296991594537L
    h = h xor (h ushr 33)
    return (h and 0x7FFFFFFF).toInt()
}

/** Convenience: collect a Flow's current emission once, or null on failure. */
private suspend fun <T> Flow<T>.firstOrNullSafe(): T? =
    runCatching { first() }.getOrNull()
