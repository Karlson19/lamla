package app.lamla.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.lamla.R
import app.lamla.data.prefs.AppPreferences
import app.lamla.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by AlarmManager when a reminder comes due.
 *
 * Responsibilities:
 *   1. Build a NotificationCompat with the right channel + content.
 *   2. Optionally speak the title via TTS if "voice announcements" is on.
 *
 * The sound itself is owned by the notification channel, so it is not set here.
 *
 * Why all this lives in onReceive (a 10-second budget):
 *   - DataStore reads are sub-ms.
 *   - TTS init is async; we fire-and-forget via [VoiceAnnouncer] which manages
 *     its own lifecycle on the application context.
 *   - We don't enqueue WorkManager here - would add latency before the user
 *     hears the sound. Direct posting is the right tool for an exact-time alert.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var voice: VoiceAnnouncer

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val kindName = intent.getStringExtra(EXTRA_KIND) ?: return
        val kind = runCatching { Reminder.Kind.valueOf(kindName) }.getOrNull() ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID).orEmpty()
        val stableId = intent.getIntExtra(EXTRA_STABLE_ID, 0)

        val officeQuestions = intent.getStringExtra(EXTRA_OFFICE_QUESTIONS)

        val openAppPi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification. Sound and importance come from the channel.
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildBigText(body, officeQuestions)))
            .setContentIntent(openAppPi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(
                when (kind) {
                    Reminder.Kind.Class, Reminder.Kind.Exam -> NotificationCompat.CATEGORY_ALARM
                    Reminder.Kind.DeadlineImminent -> NotificationCompat.CATEGORY_REMINDER
                    else -> NotificationCompat.CATEGORY_REMINDER
                }
            )

        // Group by kind so Android collapses sensibly.
        builder.setGroup("lamla-$kind")

        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

        val pending = goAsync()
        scope.launch {
            // Everything async lives inside this try so pending.finish() always runs,
            // even if a DataStore read or notify() throws. Leaking the goAsync() token
            // risks an ANR, so the finally is the one thing we never skip.
            try {
                // Voice announcement (TTS) if enabled - additive to channel sound, per spec.
                if (prefs.voiceAnnouncements.first() && (kind == Reminder.Kind.Class || kind == Reminder.Kind.Exam)) {
                    voice.announce(title)
                }

                nm.notify(stableId, builder.build())

                // Weekly recurrence: for recurring kinds (Class, OfficeHours), enqueue
                // a one-shot reschedule worker so next week's occurrence gets queued
                // up immediately after this one fires. AlarmManager's setExactAndAllowWhileIdle
                // is single-shot, so we re-arm it ourselves rather than rely on Android.
                //
                // Deadlines/exams/study sessions are one-time events - no reschedule needed.
                if (kind == Reminder.Kind.Class || kind == Reminder.Kind.OfficeHours) {
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        WORK_RESCHEDULE_AFTER_FIRE,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<RescheduleAllWorker>().build()
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildBigText(body: String, officeQuestions: String?): CharSequence {
        if (officeQuestions.isNullOrBlank()) return body
        return "$body\n\n${officeQuestions.lines().joinToString("\n") { "• $it" }}"
    }

    companion object {
        const val ACTION_FIRE = "app.lamla.action.FIRE_REMINDER"
        const val WORK_RESCHEDULE_AFTER_FIRE = "lamla-reschedule-after-fire"

        const val EXTRA_KIND = "kind"
        const val EXTRA_SOURCE_ID = "sourceId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_STABLE_ID = "stableId"
        const val EXTRA_COURSE_ID = "courseId"
        const val EXTRA_OFFICE_QUESTIONS = "officeQuestions"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
