package app.lamla.presentation.screens.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Course
import app.lamla.domain.model.Exam
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamModeScreen(
    onBack: () -> Unit,
    onAddExam: () -> Unit,
    viewModel: ExamModeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            LamlaTopBar(
                title = "Exam mode",
                onBack = onBack,
                actions = {
                    Switch(checked = state.examModeOn, onCheckedChange = { viewModel.toggleExamMode(it) })
                }
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddExam,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add exam") }
        }
    ) { padding ->
        if (state.exams.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No exams scheduled.",
                    body = "Add exams to get countdowns and a revision plan.",
                    icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    action = { LamlaButton(label = "Add exam", leadingIcon = Icons.Outlined.Add, onClick = onAddExam) }
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(state.exams, key = { _, e -> e.id }) { index, exam ->
                LamlaReveal(delayMillis = (index * 45).coerceAtMost(270)) {
                    ExamCard(exam, state.courses[exam.courseId])
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: Exam, course: Course?) {
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val now = System.currentTimeMillis()
    val daysLeft = max(0, ((exam.examDateEpochMs - now) / (24 * 60 * 60_000)).toInt())
    val examDate = remember(exam.examDateEpochMs) {
        Instant.ofEpochMilli(exam.examDateEpochMs).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
    }
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = MaterialTheme.lamla.spacing.lg,
        glowColor = if (daysLeft <= 3) accent else null,
        glowAlpha = 0.3f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course != null) LamlaChip(label = course.code, color = accent)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (daysLeft == 0) "TODAY" else "$daysLeft days",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
            Text(course?.name ?: "Exam", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$examDate · ${exam.venue.ifBlank { "Venue TBA" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (exam.topics.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TOPICS", style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        exam.topics.forEach { LamlaChip(label = it) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditScreen(
    examId: Long?,
    onBack: () -> Unit,
    viewModel: ExamEditViewModel = hiltViewModel()
) {
    LaunchedEffect(examId) { viewModel.load(examId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var newTopic by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            LamlaTopBar(
                title = if (examId == null) "New exam" else "Edit exam",
                onBack = onBack,
                navIcon = Icons.Outlined.Close,
                actions = {
                    TextButton(onClick = { scope.launch { if (viewModel.save()) onBack() } }, enabled = state.canSave) { Text("Save") }
                }
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
                LamlaField("Course") {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.allCourses.forEach { c ->
                            LamlaChip(label = "${c.code} • ${c.name}", color = Color(c.colorArgb), selected = c.id == state.selectedCourse?.id, onClick = { viewModel.selectCourse(c) })
                        }
                    }
                }
            }
            LamlaReveal(delayMillis = 40) {
                LamlaField("Date & time") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LamlaSecondaryButton(label = state.date?.toString() ?: "Pick date", onClick = { showDate = true }, leadingIcon = Icons.Outlined.CalendarMonth, modifier = Modifier.weight(1f))
                        LamlaSecondaryButton(label = state.time?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Pick time", onClick = { showTime = true }, leadingIcon = Icons.Outlined.Schedule, modifier = Modifier.weight(1f))
                    }
                }
            }
            LamlaReveal(delayMillis = 80) {
                LamlaField("Venue") {
                    LamlaTextField(state.venue, viewModel::setVenue, "e.g. Great Hall", leadingIcon = Icons.Outlined.Place)
                }
            }
            LamlaReveal(delayMillis = 120) {
                LamlaField("Topics") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.topics.forEach { t ->
                                LamlaChip(label = t, leadingIcon = Icons.Outlined.Close, onClick = { viewModel.removeTopic(t) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                LamlaTextField(newTopic, { newTopic = it }, "Add a topic")
                            }
                            LamlaButton(label = "Add", onClick = {
                                if (newTopic.isNotBlank()) { viewModel.addTopic(newTopic.trim()); newTopic = "" }
                            }, enabled = newTopic.isNotBlank())
                        }
                    }
                }
            }

            if (examId != null) {
                LamlaReveal(delayMillis = 160) {
                    LamlaDestructiveButton(label = "Delete exam", leadingIcon = Icons.Outlined.Delete, onClick = { scope.launch { viewModel.delete(); onBack() } }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showDate) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = (state.date ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        viewModel.setDate(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showTime) {
        val timeState = rememberTimePickerState(initialHour = state.time?.hour ?: 9, initialMinute = state.time?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { viewModel.setTime(LocalTime.of(timeState.hour, timeState.minute)); showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }
}
