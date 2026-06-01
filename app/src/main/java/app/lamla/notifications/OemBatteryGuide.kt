package app.lamla.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * OEM-specific battery optimization guides.
 *
 * These manufacturers aggressively kill background tasks even with battery
 * optimization disabled - they have proprietary "Auto-start manager" /
 * "Protected apps" settings buried elsewhere. Knowing the manufacturer lets us
 * deep-link the user to the right screen rather than handwaving "go find it
 * in settings somewhere".
 *
 * Reference: dontkillmyapp.com lists the worst offenders. We cover the top six.
 *
 * Each entry maps Build.MANUFACTURER → (display name, intent component path or
 * URI to open the relevant settings screen). If the deep link fails (manufacturer
 * changed the activity path between versions), we fall back to the generic
 * battery optimization settings.
 */
object OemBatteryGuide {

    enum class Manufacturer(val displayName: String) {
        Xiaomi("Xiaomi / Redmi / POCO"),
        Huawei("Huawei / Honor"),
        Oppo("OPPO"),
        Vivo("vivo / iQOO"),
        Samsung("Samsung"),
        Tecno("Tecno / Infinix / itel"),
        Generic("Your phone")
    }

    fun detect(): Manufacturer {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            "xiaomi" in m || "redmi" in b || "poco" in b -> Manufacturer.Xiaomi
            "huawei" in m || "honor" in b -> Manufacturer.Huawei
            "oppo" in m || "realme" in b -> Manufacturer.Oppo
            "vivo" in m || "iqoo" in b -> Manufacturer.Vivo
            "samsung" in m -> Manufacturer.Samsung
            "tecno" in m || "infinix" in b || "itel" in b -> Manufacturer.Tecno
            else -> Manufacturer.Generic
        }
    }

    /** Whether Android battery optimization is disabled for our package. */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = ContextCompat.getSystemService(context, PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Step-by-step instructions for the given manufacturer. Kept in English
     * because OEM settings menus are not consistently localized anyway.
     */
    fun steps(manufacturer: Manufacturer): List<String> = when (manufacturer) {
        Manufacturer.Xiaomi -> listOf(
            "Open Settings → Apps → Manage apps → Lamla",
            "Battery saver → No restrictions",
            "Autostart → Enable",
            "Other permissions → Display pop-up windows while running in background"
        )
        Manufacturer.Huawei -> listOf(
            "Settings → Apps → Lamla → Battery",
            "App launch → Manage manually → enable all three toggles",
            "Settings → Battery → App launch → Manage manually for Lamla"
        )
        Manufacturer.Oppo -> listOf(
            "Settings → Battery → Lamla → Allow background activity",
            "Settings → Apps → App management → Lamla → Allow Auto Startup"
        )
        Manufacturer.Vivo -> listOf(
            "Settings → Battery → Background power consumption manager → Lamla → Allow",
            "Settings → More settings → Permission manager → Autostart → Enable Lamla"
        )
        Manufacturer.Samsung -> listOf(
            "Settings → Apps → Lamla → Battery → Unrestricted",
            "Settings → Battery and device care → Background usage limits → Make sure Lamla is not in 'Sleeping apps'"
        )
        Manufacturer.Tecno -> listOf(
            "Settings → Power management → App power saving → Lamla → No restrictions",
            "Settings → Apps → Lamla → Autostart → Enable",
            "Settings → Battery → Lamla → Allow background activity"
        )
        Manufacturer.Generic -> listOf(
            "Open the battery optimization settings",
            "Find Lamla in the list",
            "Set it to 'Not optimized' (or 'Unrestricted', wording varies)"
        )
    }

    /**
     * Attempt to deep-link to the most useful settings screen. Falls back to
     * the generic Android battery-optimization request if the OEM-specific
     * intent isn't resolvable.
     */
    fun openSettings(context: Context, manufacturer: Manufacturer = detect()): Boolean {
        val intents = buildList {
            when (manufacturer) {
                Manufacturer.Xiaomi -> {
                    add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                    add(Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")))
                }
                Manufacturer.Huawei -> {
                    add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                    add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
                }
                Manufacturer.Oppo -> {
                    add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                    add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
                }
                Manufacturer.Vivo -> {
                    add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                    add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                }
                Manufacturer.Samsung -> {
                    add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                }
                Manufacturer.Tecno -> {
                    add(Intent().setComponent(ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.applicationmanager.activity.AppManagerActivity")))
                }
                Manufacturer.Generic -> {}
            }
            // Generic fallback: Android battery optimization screen scoped to our app
            add(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            add(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                runCatching { context.startActivity(intent) }
                    .onSuccess { return true }
            }
        }
        return false
    }
}
