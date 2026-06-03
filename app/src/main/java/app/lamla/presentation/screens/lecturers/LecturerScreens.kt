package app.lamla.presentation.screens.lecturers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import app.lamla.domain.model.Lecturer
import app.lamla.domain.model.Question
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ---------------------------------- List ----------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturersScreen(
    onBack: () -> Unit,
    onLecturer: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: LecturersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lecturers", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add lecturer") }
        }
    ) { padding ->
        if (state.lecturers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No lecturers yet.",
                    body = "Add a lecturer to attach their office hours and queue questions for them.",
                    icon = Icons.Outlined.PersonOutline,
                    action = { LamlaButton(label = "Add lecturer", leadingIcon = Icons.Outlined.Add, onClick = onAdd) }
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(state.lecturers, key = { _, l -> l.id }) { index, l ->
                LamlaReveal(delayMillis = (index * 45).coerceAtMost(270)) {
                    LamlaSurface(modifier = Modifier.fillMaxWidth(), onClick = { onLecturer(l.id) }, contentPadding = MaterialTheme.lamla.spacing.md) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialAvatar(name = l.name)
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                if (l.officeLocation.isNotBlank()) {
                                    Text(l.officeLocation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialAvatar(name: String) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials.ifEmpty { "?" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---------------------------------- Detail ----------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerDetailScreen(
    lecturerId: Long,
    onBack: () -> Unit,
    viewModel: LecturerDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(lecturerId) { viewModel.load(lecturerId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lecturer = state.lecturer
    val scope = rememberCoroutineScope()
    var newQuestion by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(lecturer?.name ?: "Lecturer", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (lecturer == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                LamlaReveal {
                    LamlaSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (lecturer.email.isNotBlank()) DetailRow(Icons.Outlined.Email, lecturer.email)
                            if (lecturer.phone.isNotBlank()) DetailRow(Icons.Outlined.Phone, lecturer.phone)
                            if (lecturer.officeLocation.isNotBlank()) DetailRow(Icons.Outlined.MeetingRoom, lecturer.officeLocation)
                            if (lecturer.notes.isNotBlank()) {
                                Spacer(Modifier.size(6.dp))
                                Text(lecturer.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Office hours", trailing = "${lecturer.officeHours.size}") }
            if (lecturer.officeHours.isEmpty()) {
                item { Text("No office hours set.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                itemsIndexed(lecturer.officeHours) { index, slot ->
                    LamlaReveal(delayMillis = (index * 45).coerceAtMost(180)) {
                        LamlaSurface(modifier = Modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    slot.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(),
                                    style = LamlaTextStyles.SectionLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "%02d:%02d–%02d:%02d".format(slot.startMinutes / 60, slot.startMinutes % 60, slot.endMinutes / 60, slot.endMinutes % 60),
                                    style = LamlaTextStyles.Metric,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Questions for ${lecturer.name.split(" ").first()}", trailing = "${state.questions.count { !it.isAnswered }} open") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        LamlaTextField(value = newQuestion, onValueChange = { newQuestion = it }, placeholder = "Add a question…")
                    }
                    LamlaButton(label = "Add", onClick = {
                        if (newQuestion.isNotBlank()) {
                            scope.launch { viewModel.addQuestion(newQuestion.trim()); newQuestion = "" }
                        }
                    }, enabled = newQuestion.isNotBlank())
                }
            }
            items(state.questions, key = { it.id }) { q ->
                QuestionRow(question = q, onToggle = { scope.launch { viewModel.toggleQuestion(q) } })
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun QuestionRow(question: Question, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerMd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (question.isAnswered) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.5.dp, if (question.isAnswered) MaterialTheme.colorScheme.onSurface else MaterialTheme.lamla.colors.hairlineStrong, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (question.isAnswered) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(12.dp))
        }
        Text(
            text = question.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (question.isAnswered) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------- Edit ----------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerEditScreen(
    lecturerId: Long?,
    onBack: () -> Unit,
    viewModel: LecturerEditViewModel = hiltViewModel()
) {
    LaunchedEffect(lecturerId) { viewModel.load(lecturerId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Which office-hour slot's time is being edited: index to isStart (true = start, false = end).
    var editingSlot by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (lecturerId == null) "New lecturer" else "Edit lecturer", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, contentDescription = null) } },
                actions = {
                    TextButton(onClick = { scope.launch { if (viewModel.save()) onBack() } }, enabled = state.canSave) { Text("Save") }
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
            LamlaField("Name") { LamlaTextField(state.name, viewModel::setName, "e.g. Dr. Ama Owusu") }
            LamlaField("Email") { LamlaTextField(state.email, viewModel::setEmail, "name@knust.edu.gh", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email) }
            LamlaField("Phone") { LamlaTextField(state.phone, viewModel::setPhone, "+233…", leadingIcon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone) }
            LamlaField("Office location") { LamlaTextField(state.officeLocation, viewModel::setOfficeLocation, "Block A, Room 305", leadingIcon = Icons.Outlined.MeetingRoom) }
            LamlaField("Notes") { LamlaTextField(state.notes, viewModel::setNotes, "Anything worth remembering", singleLine = false, minLines = 2, maxLines = 4) }

            LamlaField("Office hours") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.officeHours.isEmpty()) {
                        Text(
                            "No office hours yet. Add one, then pick the day and time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.officeHours.forEachIndexed { idx, slot ->
                        OfficeHourEditor(
                            slot = slot,
                            onDayChange = { viewModel.setOfficeHourDay(idx, it) },
                            onStartClick = { editingSlot = idx to true },
                            onEndClick = { editingSlot = idx to false },
                            onRemove = { viewModel.removeOfficeHour(idx) }
                        )
                    }
                    LamlaSecondaryButton(label = "Add office hour", leadingIcon = Icons.Outlined.Add, onClick = { viewModel.addOfficeHourSlot() })
                }
            }

            if (lecturerId != null) {
                LamlaDestructiveButton(
                    label = "Delete lecturer",
                    leadingIcon = Icons.Outlined.Delete,
                    onClick = { scope.launch { viewModel.delete(); onBack() } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Time picker for whichever office-hour slot the user tapped.
    editingSlot?.let { (idx, isStart) ->
        val slot = state.officeHours.getOrNull(idx)
        if (slot == null) {
            editingSlot = null
        } else {
            OfficeHourTimePicker(
                initialMinutes = if (isStart) slot.startMinutes else slot.endMinutes,
                onPick = { mins ->
                    if (isStart) viewModel.setOfficeHourStart(idx, mins) else viewModel.setOfficeHourEnd(idx, mins)
                    editingSlot = null
                },
                onDismiss = { editingSlot = null }
            )
        }
    }
}

/** One editable office-hour slot: day chips + start/end time buttons + remove. */
@Composable
private fun OfficeHourEditor(
    slot: app.lamla.domain.model.OfficeHourSlot,
    onDayChange: (DayOfWeek) -> Unit,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerMd)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DayOfWeek.entries.forEach { d ->
                LamlaChip(
                    label = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(),
                    selected = d == slot.dayOfWeek,
                    onClick = { onDayChange(d) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LamlaSecondaryButton(
                label = "%02d:%02d".format(slot.startMinutes / 60, slot.startMinutes % 60),
                leadingIcon = Icons.Outlined.Schedule,
                onClick = onStartClick,
                modifier = Modifier.weight(1f)
            )
            Text("to", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LamlaSecondaryButton(
                label = "%02d:%02d".format(slot.endMinutes / 60, slot.endMinutes % 60),
                leadingIcon = Icons.Outlined.Schedule,
                onClick = onEndClick,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove office hour", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficeHourTimePicker(
    initialMinutes: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(timeState.hour * 60 + timeState.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = timeState) }
    )
}
