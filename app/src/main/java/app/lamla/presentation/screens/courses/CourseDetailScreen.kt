package app.lamla.presentation.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Deadline
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Long,
    onBack: () -> Unit,
    onOpenCaptures: (Long) -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val course = state.course

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(course?.code ?: "Course", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (course == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val accent = Color(course.colorArgb)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                LamlaReveal {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                Text(course.code.take(2).uppercase(), style = MaterialTheme.typography.labelLarge, color = accent)
                            }
                            Column {
                                Text(course.code, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(course.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (state.lecturerName != null) {
                            Text(state.lecturerName!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { LamlaReveal(delayMillis = 40) { SectionLabel(text = "Study minutes this week", trailing = "${state.minutesThisWeek} min") } }
            item {
                LamlaReveal(delayMillis = 70) {
                    LamlaSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (state.minutesThisWeek == 0) "No focused study logged yet." else "${state.minutesThisWeek / 60}h ${state.minutesThisWeek % 60}m logged in the past 7 days.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            item {
                LamlaReveal(delayMillis = 100) {
                    LamlaNavRow(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = "Captures",
                        subtitle = if (state.captureCount == 0) "Snap notes, photos, and voice memos"
                        else "${state.captureCount} saved for this course",
                        accent = accent,
                        badge = if (state.captureCount > 0) "${state.captureCount}" else null,
                        onClick = { onOpenCaptures(course.id) }
                    )
                }
            }
            item { LamlaReveal(delayMillis = 130) { SectionLabel(text = "Deadlines", trailing = "${state.deadlines.size}") } }
            if (state.deadlines.isEmpty()) {
                item {
                    LamlaReveal(delayMillis = 160) {
                        Text("No deadlines for this course.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(state.deadlines, key = { _, d -> d.id }) { index, d ->
                    LamlaReveal(delayMillis = (160 + index * 40).coerceAtMost(300)) { DeadlineMiniRow(d) }
                }
            }
        }
    }
}

@Composable
private fun DeadlineMiniRow(d: Deadline) {
    val zone = ZoneId.systemDefault()
    val due = remember(d.dueAtEpochMs) {
        Instant.ofEpochMilli(d.dueAtEpochMs).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
    }
    LamlaSurface(modifier = Modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("$due · ${d.weightPercent.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
