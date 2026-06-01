package app.lamla.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import app.lamla.R

/**
 * Notification channels.
 *
 * One channel per category (spec table). Each channel:
 *   - Has its own importance and (where applicable) vibration pattern.
 *   - Resolves its sound in this order: a bundled `.ogg` under `res/raw` if one is
 *     present, otherwise the system default tone for its [Channel.fallbackType].
 *     We do not ship custom audio today, so every channel currently lands on a
 *     system tone - but the urgent channels fall back to the louder ALARM tone and
 *     the rest to the NOTIFICATION tone, so a "due now" alert does not sound the
 *     same as a routine ping. Drop a file into `res/raw` and it is picked up with
 *     no code change.
 *   - The user can pick any sound (including audio files on their device) per
 *     category from Settings -> Notification sounds, which opens the system channel
 *     screen. Android owns channel sound after creation, so this is the route that
 *     reliably changes it without disturbing queued reminders.
 *
 * Channel creation is idempotent - Android replaces channels of the same id but
 * preserves user-modified attributes (mute state, importance downgrade, a custom
 * sound the user chose) per its spec. The only attribute we can change after
 * creation is the *name/description*; sound and importance changes require a new
 * channel id, so we version ids: `class_reminder_v1`, etc., and bump the suffix
 * only if we genuinely need to force a reset.
 */
object NotificationChannels {

    data class Channel(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val importance: Int,
        val rawSound: String?,            // res/raw/<name>.ogg without extension; null = system default
        val vibrate: LongArray? = null,
        val showBadge: Boolean = true,
        // System tone used when no bundled rawSound file is present. Urgent, act-now
        // channels use TYPE_ALARM so they are audibly distinct from routine pings.
        val fallbackType: Int = RingtoneManager.TYPE_NOTIFICATION
    )

    // Sounds bundled in res/raw/. If you add a file, register the name here.
    object Sound {
        const val Chime = "chime"
        const val Urgent = "urgent"
        const val Alarm = "alarm"
        const val Ping = "ping"
        const val Bell = "bell"
    }

    val ClassReminder = Channel(
        id = "class_reminder_v1",
        nameRes = R.string.channel_class_reminder,
        descRes = R.string.channel_class_reminder_desc,
        importance = NotificationManager.IMPORTANCE_HIGH,
        rawSound = Sound.Chime,
        vibrate = longArrayOf(0, 100, 80, 100)
    )

    val DeadlineWarning = Channel(
        id = "deadline_warning_v1",
        nameRes = R.string.channel_deadline_warning,
        descRes = R.string.channel_deadline_warning_desc,
        importance = NotificationManager.IMPORTANCE_HIGH,
        rawSound = Sound.Urgent,
        vibrate = longArrayOf(0, 150, 100, 150)
    )

    val DeadlineImminent = Channel(
        id = "deadline_imminent_v1",
        nameRes = R.string.channel_deadline_imminent,
        descRes = R.string.channel_deadline_imminent_desc,
        importance = NotificationManager.IMPORTANCE_MAX,
        rawSound = Sound.Alarm,
        vibrate = longArrayOf(0, 250, 100, 250, 100, 250),
        fallbackType = RingtoneManager.TYPE_ALARM
    )

    val StudySession = Channel(
        id = "study_session_v1",
        nameRes = R.string.channel_study_session,
        descRes = R.string.channel_study_session_desc,
        importance = NotificationManager.IMPORTANCE_DEFAULT,
        rawSound = Sound.Ping,
        vibrate = longArrayOf(0, 60)
    )

    val ExamAlert = Channel(
        id = "exam_alert_v1",
        nameRes = R.string.channel_exam_alert,
        descRes = R.string.channel_exam_alert_desc,
        importance = NotificationManager.IMPORTANCE_MAX,
        rawSound = Sound.Bell,
        vibrate = longArrayOf(0, 300, 100, 300),
        fallbackType = RingtoneManager.TYPE_ALARM
    )

    val OfficeHours = Channel(
        id = "office_hours_v1",
        nameRes = R.string.channel_office_hours,
        descRes = R.string.channel_office_hours_desc,
        importance = NotificationManager.IMPORTANCE_DEFAULT,
        rawSound = Sound.Ping,
        vibrate = longArrayOf(0, 80)
    )

    val General = Channel(
        id = "general_v1",
        nameRes = R.string.channel_general,
        descRes = R.string.channel_general_desc,
        importance = NotificationManager.IMPORTANCE_LOW,
        rawSound = null
    )

    val all = listOf(ClassReminder, DeadlineWarning, DeadlineImminent, StudySession, ExamAlert, OfficeHours, General)

    /** Create-or-update channels. Idempotent. Safe to call on every cold start. */
    fun ensure(context: Context) {
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        all.forEach { channel -> register(context, nm, channel) }
    }

    private fun register(context: Context, nm: NotificationManager, channel: Channel) {
        val nc = NotificationChannel(channel.id, context.getString(channel.nameRes), channel.importance).apply {
            description = context.getString(channel.descRes)
            setShowBadge(channel.showBadge)
            channel.vibrate?.let {
                enableVibration(true)
                vibrationPattern = it
            }
            val soundUri: Uri = channel.rawSound
                ?.let { rawName ->
                    val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
                    if (resId == 0) null else Uri.parse("android.resource://${context.packageName}/$resId")
                }
                ?: RingtoneManager.getDefaultUri(channel.fallbackType)

            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(nc)
    }
}
