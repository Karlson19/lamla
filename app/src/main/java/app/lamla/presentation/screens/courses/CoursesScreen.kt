package app.lamla.presentation.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Course
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.presentation.screens.scaffold.tabBottomInset
import app.lamla.presentation.screens.scaffold.tabTopInset
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

@Composable
fun CoursesScreen(
    onCourseClick: (Long) -> Unit,
    onAddCourse: () -> Unit,
    onOpenLecturers: () -> Unit = {},
    onOpenGrades: () -> Unit = {},
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().auroraBackdrop()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.lamla.spacing.gutter,
                end = MaterialTheme.lamla.spacing.gutter,
                top = tabTopInset(16.dp),
                bottom = tabBottomInset()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Courses",
                    subtitle = state.semesterName?.let { "$it · ${state.courses.size} course${if (state.courses.size == 1) "" else "s"}" }
                )
            }
            item {
                val lecturerCount = state.lecturerNameById.size
                LamlaNavRow(
                    icon = Icons.Outlined.Groups,
                    title = "Lecturers",
                    subtitle = when (lecturerCount) {
                        0 -> "Office hours, contacts, and questions to ask"
                        1 -> "1 lecturer · office hours and questions"
                        else -> "$lecturerCount lecturers · office hours and questions"
                    },
                    badge = if (lecturerCount > 0) "$lecturerCount" else null,
                    onClick = onOpenLecturers
                )
            }
            item {
                LamlaNavRow(
                    icon = Icons.Outlined.Insights,
                    title = "Grades & CWA",
                    subtitle = "Project your CWA from the marks you've banked",
                    onClick = onOpenGrades
                )
            }
            if (state.courses.isEmpty()) {
                item {
                    EmptyState(
                        title = "No courses yet.",
                        body = "Add your courses to build out a timetable and tag deadlines.",
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        action = {
                            LamlaButton(label = "Add a course", leadingIcon = Icons.Outlined.Add, onClick = onAddCourse)
                        }
                    )
                }
            } else {
                items(state.courses, key = { it.id }) { course ->
                    CourseRow(
                        course = course,
                        lecturerName = state.lecturerNameById[course.lecturerId],
                        onClick = { onCourseClick(course.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        LamlaFab(
            onClick = onAddCourse,
            contentDescription = "Add course",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = MaterialTheme.lamla.spacing.gutter, bottom = tabBottomInset())
        )
    }
}

@Composable
private fun CourseRow(course: Course, lecturerName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Color(course.colorArgb)
    LamlaSurface(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(
                    text = course.code.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.code, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(course.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (lecturerName != null) {
                    Text(lecturerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${course.creditHours}cr", style = LamlaTextStyles.Metric, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
