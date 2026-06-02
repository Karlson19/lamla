package app.lamla.presentation.screens.deadlines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import app.lamla.domain.model.DeadlineStatus
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(
    onBack: () -> Unit,
    onDeadline: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: DeadlinesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Deadlines", style = MaterialTheme.typography.titleMedium) },
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
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add deadline") }
        }
    ) { padding ->
        if (state.deadlines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No deadlines yet.",
                    body = "When you add one, it'll be ranked by urgency × weight.",
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    action = { LamlaButton(label = "Add deadline", leadingIcon = Icons.Outlined.Add, onClick = onAdd) }
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.deadlines, key = { it.id }) { d ->
                DeadlineRow(
                    deadline = d,
                    course = state.coursesById[d.courseId],
                    onClick = { onDeadline(d.id) },
                    onToggleDone = { scope.launch { viewModel.toggleDone(d) } },
                    // Reorder/insert/remove animates - toggling "done" slides the row
                    // to its new rank instead of teleporting.
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun DeadlineRow(
    deadline: Deadline,
    course: Course?,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val zone = ZoneId.systemDefault()
    val due = remember(deadline.dueAtEpochMs) {
        Instant.ofEpochMilli(deadline.dueAtEpochMs).atZone(zone).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
    }
    val isDone = deadline.status == DeadlineStatus.Done
    val urgencyTint = remember(deadline) {
        val deltaH = (deadline.dueAtEpochMs - System.currentTimeMillis()) / 3_600_000.0
        when {
            isDone -> null
            deltaH <= 1 -> "DUE"
            deltaH <= 24 -> "TODAY"
            deltaH <= 24 * 3 -> "SOON"
            else -> null
        }
    }
    LamlaSurface(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Checkbox
            CheckButton(checked = isDone, onClick = onToggleDone, color = accent)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (course != null) LamlaChip(label = course.code, color = accent)
                    if (urgencyTint != null) {
                        Text(urgencyTint, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.lamla.colors.stressCrunch)
                    }
                }
                Text(
                    text = deadline.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$due · ${deadline.weightPercent.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckButton(checked: Boolean, onClick: () -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (checked) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.5.dp, if (checked) color else MaterialTheme.lamla.colors.hairlineStrong, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Outlined.Check, contentDescription = "Mark pending", tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
