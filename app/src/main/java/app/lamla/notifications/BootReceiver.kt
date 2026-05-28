package app.lamla.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Boot / time-change receiver.
 *
 * AlarmManager loses its schedule across:
 *   - Device reboot (incl. quick reboot)
 *   - App update (MY_PACKAGE_REPLACED)
 *   - System time/timezone change
 *
 * Each of these emits a broadcast; we enqueue a WorkManager job to recompute
 * all reminders from the database. WorkManager survives Doze and is the
 * Android-blessed way to do background work past a 10-second receiver budget.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                // Re-ensure channels (they survive but the channel name strings may have moved
                // resources across an app update).
                NotificationChannels.ensure(context)

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleAllWorker>()
                        .setInputData(
                            Data.Builder()
                                .putString(RescheduleAllWorker.KEY_TRIGGER, RescheduleAllWorker.TRIGGER_BOOT)
                                .build()
                        )
                        .build()
                )
            }
        }
    }
}
