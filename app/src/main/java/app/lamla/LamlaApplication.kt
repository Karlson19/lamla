package app.lamla

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.lamla.notifications.NotificationChannels
import app.lamla.notifications.RescheduleAllWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Responsibilities at cold start:
 *   1. Ensure notification channels exist (idempotent, safe to call every launch).
 *   2. Schedule the daily reminder-refresh worker - recovers any reminder
 *      that drifted off (OEM kill, crashed receiver, etc.) and keeps weekly
 *      recurrence going on its own.
 *
 * Hilt provides the WorkerFactory so @HiltWorker classes can be instantiated
 * by WorkManager with full dependency injection.
 */
@HiltAndroidApp
class LamlaApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        scheduleDailyReminderRefresh()
    }

    /**
     * Daily safety-net reschedule.
     *
     * Why we need this even though ReminderReceiver self-reschedules on fire:
     *   - If an alarm gets killed by the OEM before firing, nothing self-heals.
     *   - If the user adds a class while the app is closed (impossible with our
     *     UI today, but trivially possible via JSON import or a future widget action),
     *     the new reminder wouldn't get scheduled until app-open.
     *   - 24h cadence is cheap (~50 alarm IPCs per run for a full student schedule)
     *     and means worst-case drift is one day.
     *
     * KEEP policy: if it's already enqueued (e.g. from a previous cold start),
     * don't replace it - the existing cadence is fine.
     */
    private fun scheduleDailyReminderRefresh() {
        val request = PeriodicWorkRequestBuilder<RescheduleAllWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 6, flexTimeIntervalUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_REFRESH,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME_DAILY_REFRESH = "lamla-daily-reminder-refresh"
    }
}
