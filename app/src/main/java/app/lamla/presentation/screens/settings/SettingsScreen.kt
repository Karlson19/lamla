package app.lamla.presentation.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaChip
import app.lamla.ui.components.LamlaGhostButton
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.components.LamlaTextField
import app.lamla.ui.components.ScreenHeader
import app.lamla.ui.components.SectionLabel
import app.lamla.ui.theme.AppTheme
import app.lamla.ui.theme.swatch
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.presentation.screens.scaffold.tabBottomInset
import app.lamla.presentation.screens.scaffold.tabTopInset
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

@Composable
fun SettingsScreen(
    onNotificationSettings: () -> Unit,
    onBatteryGuide: () -> Unit,
    onDataExportImport: () -> Unit,
    onDiagnostics: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingName by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        contentPadding = PaddingValues(start = MaterialTheme.lamla.spacing.gutter, end = MaterialTheme.lamla.spacing.gutter, top = tabTopInset(16.dp), bottom = tabBottomInset()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ScreenHeader(title = "Settings") }

        item { SectionLabel("You") }
        item {
            NameRow(name = state.userName, onClick = { editingName = true })
        }

        item { SectionLabel("Appearance") }
        item {
            ThemePicker(selected = state.theme, onSelect = viewModel::setTheme)
        }

        item { SectionLabel("Notifications") }
        item {
            ToggleRow(
                icon = Icons.Outlined.RecordVoiceOver,
                title = "Speak class names",
                subtitle = "Use text-to-speech for class reminders.",
                checked = state.voiceAnnouncements,
                onToggle = viewModel::setVoiceAnnouncements
            )
        }
        item {
            NavRow(
                icon = Icons.Outlined.NotificationsActive,
                title = "Sounds per channel",
                subtitle = "Pick a sound for each reminder type.",
                onClick = onNotificationSettings
            )
        }
        item {
            NavRow(
                icon = Icons.Outlined.BatteryAlert,
                title = "Battery optimization",
                subtitle = "Make sure reminders fire on time.",
                onClick = onBatteryGuide
            )
        }

        item { SectionLabel("Data") }
        item {
            NavRow(
                icon = Icons.Outlined.ImportExport,
                title = "Export / Import",
                subtitle = "Back up to a JSON file on your phone.",
                onClick = onDataExportImport
            )
        }
        item {
            NavRow(
                icon = Icons.Outlined.MonitorHeart,
                title = "Diagnostics",
                subtitle = "See queued reminders and verify the background system.",
                onClick = onDiagnostics
            )
        }

        item { SectionLabel("About") }
        item {
            LamlaSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Lamla", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Local-only. No cloud, no accounts, no telemetry.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (editingName) {
        NameEditDialog(
            currentName = state.userName,
            onSave = { newName ->
                viewModel.setUserName(newName)
                editingName = false
            },
            onDismiss = { editingName = false }
        )
    }
}

@Composable
private fun NameRow(name: String, onClick: () -> Unit) {
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text("Your name", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = name.ifBlank { "Not set. Tap to add" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Small modal for editing the display name.
 *
 * Single-field, Save / Cancel. Auto-focuses the input, Done IME action saves.
 * Leaving the field blank + tapping Save is allowed (clears the name → falls
 * back to a name-less greeting on Home).
 */
@Composable
private fun NameEditDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LamlaTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "First name's fine"
                )
                Text(
                    text = "Used only to greet you on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThemePicker(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    LamlaSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { t ->
                    LamlaChip(
                        label = themeLabel(t),
                        color = t.swatch(),
                        selected = t == selected,
                        onClick = { onSelect(t) }
                    )
                }
            }
        }
    }
}

private fun themeLabel(t: AppTheme): String = when (t) {
    AppTheme.System -> "Match system"
    AppTheme.Light -> "Light"
    AppTheme.Dark -> "Dark"
    AppTheme.Gold -> "KNUST gold"
    AppTheme.Monochrome -> "Monochrome"
    AppTheme.Indigo -> "Indigo"
    AppTheme.Emerald -> "Emerald"
    AppTheme.Teal -> "Teal"
    AppTheme.Ocean -> "Ocean"
    AppTheme.Sunset -> "Sunset"
    AppTheme.Crimson -> "Crimson"
    AppTheme.Rose -> "Rose"
    AppTheme.Lavender -> "Lavender"
    AppTheme.Plum -> "Plum"
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    LamlaSurface(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(!checked) },
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    LamlaSurface(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
