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
 *   - Has a distinct default sound, importance, and (where applicable) vibration pattern.
 *   - Sound URIs prefer bundled `.ogg` files under `res/raw`; if a sound is missing we fall back
 *     gracefully to the system default rather than crashing the channel setup.
 *   - User can override sounds via Settings (see [AppPreferences.channelSound]).
 *
 * Channel creation is idempotent — Android replaces channels of the same id but
 * preserves user-modified attributes (mute state, importance downgrade) per its
 * spec. The only attribute we can change after creation is the *name/description*.
 * Sound and importance changes require a new channel id, so we version ids:
 * `class_reminder_v1`, etc., and bump the suffix only if we genuinely need to
 * force a reset.
 */
object NotificationChannels {

    data class Channel(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val importance: Int,
        val rawSound: String?,            // res/raw/<name>.ogg without extension; null = system default
        val vibrate: LongArray? = null,
        val showBadge: Boolean = true
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
        vibrate = longArrayOf(0, 250, 100, 250, 100, 250)
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
        vibrate = longArrayOf(0, 300, 100, 300)
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
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

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
