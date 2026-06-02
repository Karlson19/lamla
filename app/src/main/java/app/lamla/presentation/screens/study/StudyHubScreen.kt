package app.lamla.presentation.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaNavRow
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.components.ScreenHeader
import app.lamla.ui.components.SectionLabel
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.presentation.screens.scaffold.tabBottomInset
import app.lamla.presentation.screens.scaffold.tabTopInset
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

/**
 * Study Hub - landing screen for everything study-related.
 *
 * Sections:
 *   - "Start a focused session" - big CTA → Pomodoro
 *   - "This week" - per-course bar chart (mini)
 *   - "Recent sessions" - last few completed
 */
@Composable
fun StudyHubScreen(
    onStartPomodoro: () -> Unit,
    onOpenExamMode: () -> Unit = {},
    viewModel: StudyHubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        contentPadding = PaddingValues(start = MaterialTheme.lamla.spacing.gutter, end = MaterialTheme.lamla.spacing.gutter, top = tabTopInset(16.dp), bottom = tabBottomInset()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ScreenHeader(title = "Study", subtitle = "Deep work, gently scheduled.") }
        item {
            LamlaSurface(
                modifier = Modifier.fillMaxWidth(),
                glowColor = MaterialTheme.lamla.gradients.emberGlow,
                glowAlpha = 0.22f
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Start a focused session", style = MaterialTheme.typography.titleLarge)
                    Text("25-minute Pomodoro by default. Long break every 4 cycles.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LamlaButton(label = "Start Pomodoro", leadingIcon = Icons.Outlined.PlayArrow, onClick = onStartPomodoro)
                }
            }
        }
        item {
            val exams = state.upcomingExamCount
            LamlaNavRow(
                icon = Icons.Outlined.School,
                title = "Exam Mode",
                subtitle = when (exams) {
                    0 -> "Plan focused revision around your exams"
                    1 -> "1 exam coming up. Build a revision plan"
                    else -> "$exams exams coming up. Build a revision plan"
                },
                accent = MaterialTheme.lamla.gradients.emberGlow,
                badge = if (exams > 0) "$exams" else null,
                onClick = onOpenExamMode
            )
        }
        item { SectionLabel("This week", trailing = "${state.totalMinutesThisWeek / 60}h ${state.totalMinutesThisWeek % 60}m") }
        item { WeeklyBarChart(state) }
    }
}

@Composable
private fun WeeklyBarChart(state: StudyHubUiState) {
    if (state.perCourse.isEmpty()) {
        Text("No focused study yet this week.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxMin = state.perCourse.maxOf { it.second }.coerceAtLeast(1)
    LamlaSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.perCourse.forEach { (course, mins) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(course?.code ?: "Untagged", modifier = Modifier.width(64.dp), style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurface)
                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.lamla.colors.timelineRail)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(mins.toFloat() / maxMin)
                                .background(course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                        )
                    }
                    Text("${mins / 60}h ${mins % 60}m", style = LamlaTextStyles.Metric, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
