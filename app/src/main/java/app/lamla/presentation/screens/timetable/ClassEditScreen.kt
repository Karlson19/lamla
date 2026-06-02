package app.lamla.presentation.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Course
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Add/edit class form.
 *
 * Fields:
 *   - Course (picker - required; if none yet, deep-link to add course)
 *   - Day of week (segmented chips)
 *   - Start / End time (time pickers)
 *   - Venue
 *   - Reminder offsets (multi-select chips: 0, 5, 10, 15, 30, 60 min)
 *
 * On save: upserts, then ReminderEngine re-schedules; ViewModel calls
 * cancelForClassSession on the previous version automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditScreen(
    classId: Long?,
    onBack: () -> Unit,
    viewModel: ClassEditViewModel = hiltViewModel()
) {
    LaunchedEffect(classId) { viewModel.load(classId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (classId == null) "New class" else "Edit class", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (viewModel.save()) onBack()
                            }
                        },
                        enabled = state.canSave
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.lamla.spacing.gutter)
                .padding(top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Course picker
            LamlaReveal(delayMillis = 0) {
                FieldGroup(label = "Course") {
                    CoursePicker(
                        selected = state.selectedCourse,
                        courses = state.allCourses,
                        onSelect = viewModel::selectCourse
                    )
                }
            }

            LamlaReveal(delayMillis = 40) {
                FieldGroup(label = "Day") {
                    DayPicker(
                        selected = state.dayOfWeek,
                        onSelect = viewModel::setDay
                    )
                }
            }

            LamlaReveal(delayMillis = 80) {
                FieldGroup(label = "Time") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LamlaSecondaryButton(
                            label = formatTime(state.startMinutes),
                            onClick = { showStart = true },
                            leadingIcon = Icons.Outlined.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                        LamlaSecondaryButton(
                            label = formatTime(state.endMinutes),
                            onClick = { showEnd = true },
                            leadingIcon = Icons.Outlined.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LamlaReveal(delayMillis = 120) {
                FieldGroup(label = "Venue") {
                    TextField(
                        value = state.venue,
                        onValueChange = viewModel::setVenue,
                        placeholder = "Building, room, e.g. PB 105",
                        leadingIcon = Icons.Outlined.Place
                    )
                }
            }

            LamlaReveal(delayMillis = 160) {
                FieldGroup(label = "Reminders") {
                    ReminderOffsetPicker(
                        selectedOffsets = state.reminderOffsetsMinutes,
                        options = listOf(0, 5, 10, 15, 30, 60),
                        onToggle = viewModel::toggleReminderOffset
                    )
                }
            }

            if (classId != null) {
                LamlaReveal(delayMillis = 200) {
                    LamlaDestructiveButton(
                        label = "Delete class",
                        onClick = {
                            scope.launch {
                                viewModel.delete()
                                onBack()
                            }
                        },
                        leadingIcon = Icons.Outlined.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showStart) {
        TimePickerSheet(
            initialMinutes = state.startMinutes,
            onPick = { viewModel.setStartMinutes(it); showStart = false },
            onDismiss = { showStart = false }
        )
    }
    if (showEnd) {
        TimePickerSheet(
            initialMinutes = state.endMinutes,
            onPick = { viewModel.setEndMinutes(it); showEnd = false },
            onDismiss = { showEnd = false }
        )
    }
}

@Composable
private fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun CoursePicker(
    selected: Course?,
    courses: List<Course>,
    onSelect: (Course) -> Unit
) {
    if (courses.isEmpty()) {
        Text(
            text = "No courses yet. Add one from Courses tab first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        courses.forEach { c ->
            LamlaChip(
                label = "${c.code} • ${c.name}",
                color = Color(c.colorArgb),
                selected = selected?.id == c.id,
                onClick = { onSelect(c) }
            )
        }
    }
}

@Composable
private fun DayPicker(
    selected: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.entries.forEach { d ->
            LamlaChip(
                label = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(),
                selected = d == selected,
                onClick = { onSelect(d) }
            )
        }
    }
}

@Composable
private fun ReminderOffsetPicker(
    selectedOffsets: List<Int>,
    options: List<Int>,
    onToggle: (Int) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { mins ->
            LamlaChip(
                label = if (mins == 0) "On time" else "$mins min before",
                selected = mins in selectedOffsets,
                onClick = { onToggle(mins) }
            )
        }
    }
}

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerMd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainerLow, shape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(cs.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(
    initialMinutes: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}

private fun formatTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
