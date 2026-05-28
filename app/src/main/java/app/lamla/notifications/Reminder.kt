package app.lamla.notifications

/**
 * Reminders.
 *
 * A reminder is a single fire-once notification scheduled for a specific epoch ms.
 * Each one is identified by a [stableId] derived from its source so re-scheduling
 * a class's reminders idempotently replaces (not duplicates) existing ones.
 *
 * Why a sealed Kind: serialization into Intent extras (a flat string) is easier
 * than persisting a polymorphic class, and the receiver-side switch is exhaustive.
 */
sealed interface Reminder {
    val triggerAtEpochMs: Long
    val channelId: String
    val title: String
    val body: String

    /** Used as both the PendingIntent request code and the Notification id. */
    val stableId: Int

    /** Source row id, e.g. classSessionId — for boot rescheduling lookup. */
    val sourceId: Long

    /** Source kind discriminator, persisted as Intent extra. */
    val kind: Kind

    enum class Kind { Class, Deadline, DeadlineImminent, StudySession, OfficeHours, Exam }

    data class Class(
        override val sourceId: Long,
        val courseId: Long,
        val offsetMinutes: Int,
        override val triggerAtEpochMs: Long,
        override val title: String,
        override val body: String
    ) : Reminder {
        override val channelId = NotificationChannels.ClassReminder.id
        override val kind = Kind.Class
        override val stableId: Int = stable("class", sourceId, offsetMinutes)
    }

    data class Deadline(
        override val sourceId: Long,
        val courseId: Long,
        val offsetMinutes: Int,
        val imminent: Boolean,
        override val triggerAtEpochMs: Long,
        override val title: String,
        override val body: String
    ) : Reminder {
        override val channelId = if (imminent) NotificationChannels.DeadlineImminent.id else NotificationChannels.DeadlineWarning.id
        override val kind = if (imminent) Kind.DeadlineImminent else Kind.Deadline
        override val stableId: Int = stable("deadline", sourceId, offsetMinutes)
    }

    data class StudySession(
        override val sourceId: Long,
        val courseId: Long?,
        val offsetMinutes: Int,
        override val triggerAtEpochMs: Long,
        override val title: String,
        override val body: String
    ) : Reminder {
        override val channelId = NotificationChannels.StudySession.id
        override val kind = Kind.StudySession
        override val stableId: Int = stable("study", sourceId, offsetMinutes)
    }

    data class OfficeHours(
        override val sourceId: Long,             // lecturerId
        val pendingQuestionsPreview: String,     // surfaced in body when imminent
        val offsetMinutes: Int,
        override val triggerAtEpochMs: Long,
        override val title: String,
        override val body: String
    ) : Reminder {
        override val channelId = NotificationChannels.OfficeHours.id
        override val kind = Kind.OfficeHours
        override val stableId: Int = stable("office", sourceId, offsetMinutes)
    }

    data class Exam(
        override val sourceId: Long,
        val courseId: Long,
        val offsetMinutes: Int,
        override val triggerAtEpochMs: Long,
        override val title: String,
        override val body: String
    ) : Reminder {
        override val channelId = NotificationChannels.ExamAlert.id
        override val kind = Kind.Exam
        override val stableId: Int = stable("exam", sourceId, offsetMinutes)
    }
}

/**
 * Stable id construction.
 *
 * Hashing strategy: combine kind tag + sourceId + offset. Collisions across kinds
 * are astronomically unlikely but possible — we keep the kind tag namespace short
 * (one char) so the hash space is mostly source+offset.
 *
 * `Int` ceiling fits PendingIntent request code requirements.
 */
private fun stable(tag: String, sourceId: Long, offsetMinutes: Int): Int {
    val seed = (tag.hashCode().toLong() * 31L + sourceId) * 31L + offsetMinutes
    // Mix bits, then collapse to Int — avoids dominance by sourceId alone.
    var h = seed
    h = h xor (h ushr 33)
    h *= -49064778989728563L           // SplitMix64 mixer
    h = h xor (h ushr 33)
    h *= -4265267296991594537L
    h = h xor (h ushr 33)
    return (h and 0x7FFFFFFF).toInt()  // positive 31-bit
}
