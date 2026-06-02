package app.lamla.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import app.lamla.ui.theme.ThemeAccent
import app.lamla.ui.theme.ThemeMode
import app.lamla.ui.theme.label
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
            ThemePicker(
                mode = state.themeMode,
                accent = state.themeAccent,
                onModeChange = viewModel::setThemeMode,
                onAccentChange = viewModel::setThemeAccent
            )
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
            Icon(Icons.Outlined.PersonOutline, contentDescription = "User profile", tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text("Your name", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = name.ifBlank { "Not set. Tap to add" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Navigate to name settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

/**
 * Theme picker, split into two clear, independent controls:
 *   1. Appearance - a segmented Light/Dark/System control (the brightness axis).
 *   2. Accent     - a grid of color swatches (the hue axis).
 *
 * Splitting them means picking "Dark" no longer drops your color, and picking a
 * vibrant accent no longer forces system light/dark. Any combination is valid.
 */
@Composable
private fun ThemePicker(
    mode: ThemeMode,
    accent: ThemeAccent,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (ThemeAccent) -> Unit
) {
    LamlaSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                ModeSegmented(selected = mode, onSelect = onModeChange)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Accent color", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(accent.label(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AccentGrid(selected = accent, onSelect = onAccentChange)
            }
        }
    }
}

/** Three-way segmented control for the light/dark mode. Cross-fades the active segment. */
@Composable
private fun ModeSegmented(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.System, "System", Icons.Outlined.BrightnessAuto),
        Triple(ThemeMode.Light, "Light", Icons.Outlined.LightMode),
        Triple(ThemeMode.Dark, "Dark", Icons.Outlined.DarkMode)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (m, label, icon) ->
            val isSel = m == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.medium2),
                label = "segBg"
            )
            val fg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.medium2),
                label = "segFg"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** Grid of accent swatches; the selected one rings up and shows a check. */
@Composable
private fun AccentGrid(selected: ThemeAccent, onSelect: (ThemeAccent) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThemeAccent.entries.forEach { accent ->
            val isSel = accent == selected
            val color = accent.swatch()
            val ring by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.lamla.colors.hairline,
                MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.short4),
                label = "accentRing"
            )
            val scale by animateFloatAsState(if (isSel) 1f else 0.9f, MaterialTheme.lamla.motion.springBouncy, label = "accentScale")
            val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (isSel) 2.5.dp else 1.dp, ring, CircleShape)
                    .clickable { onSelect(accent) },
                contentAlignment = Alignment.Center
            ) {
                if (isSel) {
                    Icon(Icons.Outlined.Check, contentDescription = accent.label(), tint = onColor, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
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
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
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
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Navigate to $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
