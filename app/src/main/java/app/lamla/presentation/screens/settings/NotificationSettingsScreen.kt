package app.lamla.presentation.screens.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.lamla.notifications.NotificationChannels
import app.lamla.ui.components.LamlaReveal
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

/**
 * Per-channel sound configuration.
 *
 * Android owns a channel's sound after the channel is created (it cannot be changed
 * from the app without recreating the channel, which would disturb queued reminders).
 * So each row deep-links to that channel's own system settings screen, where the user
 * picks any sound they like - including audio files on their device via the picker's
 * "Add" option. We show the sound that is currently active and refresh it on resume,
 * so the moment they come back from system settings the new choice is reflected here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notification sounds", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LamlaReveal {
                    Text(
                        text = "Tap a category to choose its sound. You can pick any ringtone or an " +
                            "audio file from your device. Urgent alerts use the louder alarm tone by default.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(NotificationChannels.all, key = { _, c -> c.id }) { index, channel ->
                LamlaReveal(delayMillis = (40 + index * 45).coerceAtMost(270)) {
                    ChannelRow(channel)
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: NotificationChannels.Channel) {
    val context = LocalContext.current
    var soundTitle by remember { mutableStateOf(currentSoundTitle(context, channel.id)) }
    LifecycleResumeEffect(channel.id) {
        soundTitle = currentSoundTitle(context, channel.id)
        onPauseOrDispose { }
    }
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channel.id)
            }
            runCatching { context.startActivity(intent) }
        },
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(context.getString(channel.nameRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(context.getString(channel.descRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Sound: $soundTitle",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.lamla.gradients.emberGlow
                )
            }
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Resolve the human-readable title of the sound a channel is currently using. */
private fun currentSoundTitle(context: Context, channelId: String): String {
    val nm = context.getSystemService(NotificationManager::class.java) ?: return "Default"
    val channel = nm.getNotificationChannel(channelId) ?: return "Default"
    val uri = channel.sound ?: return "Silent"
    return runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
        .getOrNull() ?: "Custom sound"
}
