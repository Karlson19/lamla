package app.lamla.presentation.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Lecturer
import app.lamla.ui.components.*
import app.lamla.ui.theme.Palette
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    courseId: Long?,
    onBack: () -> Unit,
    viewModel: CourseEditViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (courseId == null) "New course" else "Edit course", style = MaterialTheme.typography.titleMedium) },
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
            LamlaField("Course code") {
                LamlaTextField(value = state.code, onValueChange = viewModel::setCode, placeholder = "e.g. COE271")
            }
            LamlaField("Course name") {
                LamlaTextField(value = state.name, onValueChange = viewModel::setName, placeholder = "e.g. Computer Networking")
            }
            LamlaField("Credit hours") {
                LamlaTextField(
                    value = state.creditHoursText,
                    onValueChange = viewModel::setCreditHours,
                    placeholder = "3",
                    keyboardType = KeyboardType.Number
                )
            }
            LamlaField("Lecturer") {
                LecturerPicker(
                    selected = state.selectedLecturer,
                    options = state.allLecturers,
                    onSelect = viewModel::setLecturer
                )
            }
            LamlaField("Color") {
                ColorPicker(
                    selected = state.colorArgb,
                    onSelect = viewModel::setColor
                )
            }

            if (courseId != null) {
                LamlaDestructiveButton(
                    label = "Delete course",
                    onClick = { scope.launch { viewModel.delete(); onBack() } },
                    leadingIcon = Icons.Outlined.Delete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LecturerPicker(
    selected: Lecturer?,
    options: List<Lecturer>,
    onSelect: (Lecturer?) -> Unit
) {
    if (options.isEmpty()) {
        Text("Add lecturers from the Lecturers screen to link them here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LamlaChip(label = "Unassigned", selected = selected == null, onClick = { onSelect(null) })
        options.forEach { lec ->
            LamlaChip(label = lec.name, selected = lec.id == selected?.id, onClick = { onSelect(lec) })
        }
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Palette.CoursePalette.forEach { c ->
            val argbInt = c.toArgb()
            val isSelected = argbInt == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.lamla.colors.hairline, CircleShape)
                    .clickable { onSelect(argbInt) }
            )
        }
    }
}
