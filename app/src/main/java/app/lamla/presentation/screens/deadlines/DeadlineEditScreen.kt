package app.lamla.presentation.screens.deadlines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Course
import app.lamla.ui.components.*
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineEditScreen(
    deadlineId: Long?,
    onBack: () -> Unit,
    viewModel: DeadlineEditViewModel = hiltViewModel()
) {
    LaunchedEffect(deadlineId) { viewModel.load(deadlineId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (deadlineId == null) "New deadline" else "Edit deadline", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, contentDescription = null) } },
                actions = {
                    TextButton(onClick = { scope.launch { if (viewModel.save()) onBack() } }, enabled = state.canSave) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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
            LamlaReveal(delayMillis = 0) {
                LamlaField("Title") {
                    LamlaTextField(value = state.title, onValueChange = viewModel::setTitle, placeholder = "e.g. Network Lab Assignment 3")
                }
            }
            LamlaReveal(delayMillis = 40) {
                LamlaField("Course") {
                    CoursePickerInline(state.allCourses, state.selectedCourse, viewModel::selectCourse)
                }
            }
            LamlaReveal(delayMillis = 80) {
                LamlaField("Due") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LamlaSecondaryButton(
                            label = state.dueDate?.toString() ?: "Pick date",
                            onClick = { showDate = true },
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f)
                        )
                        LamlaSecondaryButton(
                            label = state.dueTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Pick time",
                            onClick = { showTime = true },
                            leadingIcon = Icons.Outlined.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            LamlaReveal(delayMillis = 120) {
                LamlaField("Weight (% of grade)") {
                    LamlaTextField(
                        value = if (state.weightPercent == 0f) "" else state.weightPercent.toString(),
                        onValueChange = { viewModel.setWeight(it.toFloatOrNull() ?: 0f) },
                        placeholder = "10",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
            LamlaReveal(delayMillis = 160) {
                LamlaField("Mark obtained (optional)") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LamlaTextField(
                                value = state.scoreText,
                                onValueChange = viewModel::setScore,
                                placeholder = "17",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            Text("out of", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LamlaTextField(
                                value = state.scoreMaxText,
                                onValueChange = viewModel::setScoreMax,
                                placeholder = "20",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Leave the mark blank until it's graded. Filled marks feed your CWA projection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            LamlaReveal(delayMillis = 200) {
                LamlaField("Description (optional)") {
                    LamlaTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        placeholder = "Brief notes: what to submit, where",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6
                    )
                }
            }

            if (deadlineId != null) {
                LamlaReveal(delayMillis = 240) {
                    LamlaDestructiveButton(
                        label = "Delete deadline",
                        onClick = { scope.launch { viewModel.delete(); onBack() } },
                        leadingIcon = Icons.Outlined.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDate) {
        DatePickerDialogBasic(
            initial = state.dueDate ?: LocalDate.now(),
            onPick = { viewModel.setDate(it); showDate = false },
            onDismiss = { showDate = false }
        )
    }
    if (showTime) {
        TimePickerDialogBasic(
            initial = state.dueTime ?: LocalTime.of(23, 59),
            onPick = { viewModel.setTime(it); showTime = false },
            onDismiss = { showTime = false }
        )
    }
}

@Composable
private fun CoursePickerInline(courses: List<Course>, selected: Course?, onSelect: (Course) -> Unit) {
    if (courses.isEmpty()) {
        Text("Add a course first to attach this deadline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        courses.forEach { c ->
            LamlaChip(
                label = "${c.code} • ${c.name}",
                color = androidx.compose.ui.graphics.Color(c.colorArgb),
                selected = c.id == selected?.id,
                onClick = { onSelect(c) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogBasic(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    onPick(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogBasic(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}
