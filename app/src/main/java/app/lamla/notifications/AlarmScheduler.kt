package app.lamla.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager front-end.
 *
 * Spec: class start times and exam alerts use [setExactAndAllowWhileIdle];
 * deadlines and study reminders use [WorkManager] (less critical, can drift).
 *
 * Our convention:
 *   - This scheduler handles **all** Reminder kinds via AlarmManager, exact-or-allowed.
 *   - WorkManager is used elsewhere for *periodic* tasks (daily stress recompute,
 *     widget refresh), not single-fire reminders.
 *   - Rationale: exact alarms have been available on all min-SDK targets (26+);
 *     using AlarmManager uniformly keeps the cancel-on-edit story simple
 *     (one cancel API, not two).
 *
 * Permission: from Android 12+, `SCHEDULE_EXACT_ALARM` is a runtime grant for
 * "alarm clock"-style use. We declared it in the manifest; we check at call time
 * and fall back to an inexact alarm if the user denied it, with a one-time toast
 * surfaced from the caller (we don't show UI from this class).
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        ContextCompat.getSystemService(context, AlarmManager::class.java)
            ?: error("AlarmManager unavailable")

    /** True if we can schedule exact alarms (Android 12+). On <12, always true. */
    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    fun schedule(reminder: Reminder) {
        val now = System.currentTimeMillis()
        if (reminder.triggerAtEpochMs <= now) return  // don't schedule in the past

        val pi = pendingIntentFor(reminder, mutable = false)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtEpochMs,
                    pi
                )
            } else {
                // Best-effort fallback - user revoked exact-alarm permission.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtEpochMs,
                    pi
                )
            }
        } catch (se: SecurityException) {
            // Race: permission revoked between canScheduleExact() and setExact*().
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtEpochMs, pi)
        }
    }

    fun scheduleAll(reminders: Collection<Reminder>) = reminders.forEach { schedule(it) }

    fun cancel(reminder: Reminder) {
        val pi = pendingIntentFor(reminder, mutable = false, flagsExtra = PendingIntent.FLAG_NO_CREATE)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    /** Cancel by stableId - used when the underlying source row is mutated; we don't have full Reminder. */
    fun cancelByStableId(stableId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE)
        val pi = PendingIntent.getBroadcast(
            context, stableId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun pendingIntentFor(
        reminder: Reminder,
        mutable: Boolean,
        flagsExtra: Int = 0
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_KIND, reminder.kind.name)
            putExtra(ReminderReceiver.EXTRA_SOURCE_ID, reminder.sourceId)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_BODY, reminder.body)
            putExtra(ReminderReceiver.EXTRA_CHANNEL_ID, reminder.channelId)
            putExtra(ReminderReceiver.EXTRA_STABLE_ID, reminder.stableId)
            when (reminder) {
                is Reminder.Class -> {
                    putExtra(ReminderReceiver.EXTRA_COURSE_ID, reminder.courseId)
                }
                is Reminder.Deadline -> {
                    putExtra(ReminderReceiver.EXTRA_COURSE_ID, reminder.courseId)
                }
                is Reminder.StudySession -> reminder.courseId?.let {
                    putExtra(ReminderReceiver.EXTRA_COURSE_ID, it)
                }
                is Reminder.OfficeHours -> {
                    putExtra(ReminderReceiver.EXTRA_OFFICE_QUESTIONS, reminder.pendingQuestionsPreview)
                }
                is Reminder.Exam -> {
                    putExtra(ReminderReceiver.EXTRA_COURSE_ID, reminder.courseId)
                }
            }
        }
        val flags = (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or
            PendingIntent.FLAG_UPDATE_CURRENT or flagsExtra
        return PendingIntent.getBroadcast(context, reminder.stableId, intent, flags)
    }
}
