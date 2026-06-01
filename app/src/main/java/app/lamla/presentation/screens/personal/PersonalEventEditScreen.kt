package app.lamla.presentation.screens.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.ui.components.*
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Add/edit a personal event (study block, appointment, anything non-class).
 *
 * Shows up on the Home timeline interleaved with classes & deadlines. The form
 * is deliberately stripped: title, start time, duration, notes. Recurrence is
 * deferred to a follow-up (RRULE editing is a UI engineering project on its own).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalEventEditScreen(
    eventId: Long?,
    onBack: () -> Unit,
    viewModel: PersonalEventEditViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) { viewModel.load(eventId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (eventId == null) "New event" else "Edit event",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { scope.launch { if (viewModel.save()) onBack() } },
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
            LamlaField("Title") {
                LamlaTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = "e.g. Project group meeting"
                )
            }
            LamlaField("Date") {
                LamlaSecondaryButton(
                    label = state.date?.toString() ?: "Pick date",
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    onClick = { showDate = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LamlaField("Time") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LamlaSecondaryButton(
                        label = state.startTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Start",
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = { showStart = true },
                        modifier = Modifier.weight(1f)
                    )
                    LamlaSecondaryButton(
                        label = state.endTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "End",
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = { showEnd = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            LamlaField("Notes (optional)") {
                LamlaTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    placeholder = "Anything worth remembering",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 6
                )
            }
            if (eventId != null) {
                LamlaDestructiveButton(
                    label = "Delete event",
                    leadingIcon = Icons.Outlined.Delete,
                    onClick = { scope.launch { viewModel.delete(); onBack() } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDate) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.date ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        viewModel.setDate(
                            Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showStart) {
        val s = rememberTimePickerState(
            initialHour = state.startTime?.hour ?: 9,
            initialMinute = state.startTime?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStart = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setStartTime(LocalTime.of(s.hour, s.minute))
                    showStart = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStart = false }) { Text("Cancel") } },
            text = { TimePicker(state = s) }
        )
    }
    if (showEnd) {
        val s = rememberTimePickerState(
            initialHour = state.endTime?.hour ?: 10,
            initialMinute = state.endTime?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setEndTime(LocalTime.of(s.hour, s.minute))
                    showEnd = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEnd = false }) { Text("Cancel") } },
            text = { TimePicker(state = s) }
        )
    }
}
