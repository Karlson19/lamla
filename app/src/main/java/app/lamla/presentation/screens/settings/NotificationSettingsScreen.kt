package app.lamla.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lamla.notifications.NotificationChannels
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.theme.lamla

/**
 * Per-channel sound configuration.
 *
 * Each channel renders a row with: name, current sound (default or override),
 * and a chevron → into Android's system channel-settings screen, where the
 * user changes sound natively (Android requires this since O — channel sound
 * isn't mutable from the app once set).
 *
 * We deep-link to the channel settings screen via [Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS]
 * so it's a one-tap journey.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notification sounds", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(NotificationChannels.all, key = { it.id }) { channel ->
                ChannelRow(channel)
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: NotificationChannels.Channel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val intent = android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channel.id)
            }
            runCatching { context.startActivity(intent) }
        },
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Column {
            Text(context.getString(channel.nameRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(context.getString(channel.descRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

