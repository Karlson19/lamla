package app.lamla.domain.usecase

import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import app.lamla.domain.model.PersonalEvent
import app.lamla.domain.model.StudySession
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Today's flow.
 *
 * Things 3-style chronological timeline of everything happening today:
 *   - All classes in their time slots
 *   - All deadlines (slotted at due time)
 *   - All study sessions
 *   - All personal events
 *
 * Why interleave? Because separate "Classes today" + "Deadlines today" cards
 * are how every other student app looks. A unified timeline shows the *actual
 * shape* of your day - when you're free, when you're back-to-back.
 *
 * Renders as a left-rail timeline (see HomeScreen). Sort key is start time.
 */
object TodayFlow {

    sealed interface Item {
        val startMinutes: Int   // minutes since midnight
        val course: Course?

        data class ClassItem(
            val session: ClassSession,
            override val course: Course?,
            override val startMinutes: Int = session.startMinutes,
            val endMinutes: Int = session.endMinutes
        ) : Item

        data class DeadlineItem(
            val deadline: Deadline,
            override val course: Course?,
            override val startMinutes: Int  // local minute-of-day of due time
        ) : Item

        data class StudyItem(
            val session: StudySession,
            override val course: Course?,
            override val startMinutes: Int
        ) : Item

        data class PersonalItem(
            val event: PersonalEvent,
            override val course: Course? = null,
            override val startMinutes: Int
        ) : Item
    }

    fun build(
        date: LocalDate,
        zone: ZoneId,
        classSessions: List<ClassSession>,
        deadlines: List<Deadline>,
        studySessions: List<StudySession>,
        personalEvents: List<PersonalEvent>,
        coursesById: Map<Long, Course>
    ): List<Item> {
        val day = date.dayOfWeek
        val startOfDay = LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(zone).toInstant().toEpochMilli()
        val endOfDay = startOfDay + 24L * 60 * 60_000

        val classes = classSessions
            .filter { it.dayOfWeek == day }
            .map { Item.ClassItem(it, coursesById[it.courseId]) }

        val deadlinesToday = deadlines
            .filter { it.dueAtEpochMs in startOfDay until endOfDay }
            .map {
                val mins = ((it.dueAtEpochMs - startOfDay) / 60_000).toInt()
                Item.DeadlineItem(it, coursesById[it.courseId], mins)
            }

        val studyToday = studySessions
            .filter { it.scheduledStartEpochMs in startOfDay until endOfDay }
            .map {
                val mins = ((it.scheduledStartEpochMs - startOfDay) / 60_000).toInt()
                Item.StudyItem(it, it.courseId?.let(coursesById::get), mins)
            }

        val personalToday = personalEvents
            .filter { it.startEpochMs in startOfDay until endOfDay }
            .map {
                val mins = ((it.startEpochMs - startOfDay) / 60_000).toInt()
                Item.PersonalItem(it, startMinutes = mins)
            }

        return (classes + deadlinesToday + studyToday + personalToday).sortedBy { it.startMinutes }
    }
}
