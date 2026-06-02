package app.lamla.presentation.screens.timetable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.presentation.screens.scaffold.tabBottomInset
import app.lamla.presentation.screens.scaffold.tabTopInset
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Timetable.
 *
 * Two coordinated views:
 *   - Top: horizontal day-of-week tab strip (Mon, Tue, ..., Sun). Tappable + swipeable.
 *   - Body: HorizontalPager - one full-day list per page. Each row is a class card.
 *
 * Why a pager (not just a "selected day" filter): swipe gesture between days is
 * a high-value affordance students expect on calendar surfaces. Compose's
 * HorizontalPager makes it cheap, and keeps the rendering layer-cake shallow.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onAddClass: () -> Unit,
    onClassClick: (Long) -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val days = DayOfWeek.entries.toList()  // MON..SUN
    val todayDay = LocalDate.now().dayOfWeek
    val pagerState = rememberPagerState(
        initialPage = days.indexOf(todayDay).coerceAtLeast(0),
        pageCount = { days.size }
    )
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .auroraBackdrop()
        ) {
            // Header
            ScreenHeader(
                title = "Timetable",
                subtitle = "Your week in motion",
                modifier = Modifier.padding(
                    start = MaterialTheme.lamla.spacing.gutter,
                    end = MaterialTheme.lamla.spacing.gutter,
                    top = tabTopInset(16.dp),
                    bottom = 8.dp
                )
            )
            DayTabs(
                days = days,
                selectedIndex = pagerState.currentPage,
                today = todayDay,
                onSelect = { idx ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { pagerState.animateScrollToPage(idx) }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp
            ) { page ->
                val day = days[page]
                DayClassList(
                    day = day,
                    sessions = state.sessionsByDay[day] ?: emptyList(),
                    coursesById = state.coursesById,
                    onClassClick = onClassClick
                )
            }
        }

        // Add button: ember fill + warm halo, matching Home's capture action.
        val gradients = MaterialTheme.lamla.gradients
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = MaterialTheme.lamla.spacing.gutter, bottom = tabBottomInset())
                .size(48.dp)
                .glow(gradients.emberGlow, androidx.compose.foundation.shape.CircleShape, radius = 15.dp, alpha = 0.5f)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(gradients.emberLinear, androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onAddClass),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add class",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DayTabs(
    days: List<DayOfWeek>,
    selectedIndex: Int,
    today: DayOfWeek,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MaterialTheme.lamla.spacing.gutter, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days.size) { idx ->
            val day = days[idx]
            val isSelected = idx == selectedIndex
            val isToday = day == today
            DayTab(
                shortLabel = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                isSelected = isSelected,
                isToday = isToday,
                onClick = { onSelect(idx) }
            )
        }
    }
}

@Composable
private fun DayTab(
    shortLabel: String,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (isSelected) cs.onSurface else Color.Transparent
    val fg = if (isSelected) cs.surface else cs.onSurfaceVariant
    val border = if (!isSelected) MaterialTheme.lamla.colors.hairline else Color.Transparent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = shortLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium
            )
            if (isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.lamla.colors.timelineNow)
                )
            }
        }
    }
}

@Composable
private fun DayClassList(
    day: DayOfWeek,
    sessions: List<ClassSession>,
    coursesById: Map<Long, Course>,
    onClassClick: (Long) -> Unit
) {
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Nothing scheduled.",
                body = "${day.getDisplayName(TextStyle.FULL, Locale.getDefault())}s are clear for now."
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.lamla.spacing.gutter,
            end = MaterialTheme.lamla.spacing.gutter,
            top = 12.dp,
            bottom = tabBottomInset()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            val course = coursesById[session.courseId]
            TimetableClassRow(
                session = session,
                course = course,
                onClick = { onClassClick(session.id) }
            )
        }
    }
}

@Composable
private fun TimetableClassRow(
    session: ClassSession,
    course: Course?,
    onClick: () -> Unit
) {
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Time column
            Column(modifier = Modifier.width(58.dp)) {
                Text(
                    text = formatTime(session.startMinutes),
                    style = LamlaTextStyles.Metric,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(session.endMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Accent rail
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.7f))
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (course != null) LamlaChip(label = course.code, color = accent)
                }
                Text(
                    text = course?.name ?: "Class",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = session.venue.ifBlank { "Venue TBA" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
