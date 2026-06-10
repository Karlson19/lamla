package app.lamla.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.usecase.StressScore
import app.lamla.domain.usecase.TodayFlow
import app.lamla.presentation.enqueueExactAlarmReschedule
import app.lamla.presentation.openAppNotificationSettings
import app.lamla.presentation.openBatteryOptimizationSettings
import app.lamla.presentation.openExactAlarmSettings
import app.lamla.presentation.rememberBatteryOptimizationIgnored
import app.lamla.presentation.rememberExactAlarmAllowed
import app.lamla.presentation.rememberNotificationsAllowed
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.presentation.screens.scaffold.tabBottomInset
import app.lamla.presentation.screens.scaffold.tabTopInset
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home - "Today".
 *
 * Layout, top-to-bottom:
 *   1. Editorial header: greeting + dateline
 *   2. Stress score card (tappable → bottom-sheet breakdown)
 *   3. Today's flow timeline (chronological - classes, deadlines, study blocks interleaved)
 *   4. Floating "+" capture FAB (lives in scaffold; sheet handled here)
 *
 * Why no separate "Next class" giant card: the next class is naturally the first
 * unstarted item in the timeline. Designating it visually (full color tag + bigger
 * type) preserves the spec's intent without duplicating the data on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDeadlines: () -> Unit,
    onAddCapture: () -> Unit,
    onClassClick: (Long) -> Unit,
    onDeadlineClick: (Long) -> Unit,
    onStartPomodoro: () -> Unit,
    onScheduleEvent: () -> Unit = {},
    onPersonalEventClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBreakdown by rememberSaveable { mutableStateOf(false) }
    var showCaptureSheet by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    // If the OS is blocking notifications, reminders silently never arrive, so we
    // surface a one-tap fix at the very top of the page. Re-checked on resume.
    val notificationsAllowed = rememberNotificationsAllowed()
    // On Android 12+, without exact-alarm access reminders downgrade to inexact and
    // can fire minutes late. Same one-tap fix, and the moment access flips on we
    // re-arm every queued alarm so they upgrade to exact without waiting for a restart.
    val exactAlarmsAllowed = rememberExactAlarmAllowed()
    var exactAlarmsWereAllowed by rememberSaveable { mutableStateOf(exactAlarmsAllowed) }

    // On Android, the OS can defer our alarms when the app isn't exempt from battery
    // optimization, so reminders may arrive late. Re-checked on resume so it clears the
    // moment the user grants the exemption.
    val batteryOptimized = !rememberBatteryOptimizationIgnored()

    LaunchedEffect(exactAlarmsAllowed) {
        if (exactAlarmsAllowed && !exactAlarmsWereAllowed) context.enqueueExactAlarmReschedule()
        exactAlarmsWereAllowed = exactAlarmsAllowed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Warm aura blooming from the top, behind the greeting. Fixed to the
            // screen so it stays put as the timeline scrolls beneath it.
            .auroraBackdrop()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.lamla.spacing.gutter,
                end = MaterialTheme.lamla.spacing.gutter,
                top = tabTopInset(16.dp),
                bottom = tabBottomInset()   // clears the system nav + floating bar
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!notificationsAllowed) {
                item {
                    NotificationsOffBanner(onEnable = { context.openAppNotificationSettings() })
                }
            }

            // Only nudge about exact alarms when notifications themselves are on -
            // otherwise the notification banner is the more important fix to show first.
            if (notificationsAllowed && !exactAlarmsAllowed) {
                item {
                    ExactAlarmsOffBanner(onEnable = { context.openExactAlarmSettings() })
                }
            }

            // Only surface the battery nudge once notifications are on - otherwise the
            // notifications banner is the more urgent fix and we avoid stacking three.
            if (notificationsAllowed && batteryOptimized) {
                item {
                    BatteryOptimizationBanner(onEnable = { context.openBatteryOptimizationSettings() })
                }
            }

            item {
                HomeHeader(
                    greeting = state.greeting,
                    userName = state.userName,
                    today = state.today
                )
            }

            item {
                StressIndicator(
                    score = state.stressScore,
                    band = state.stressBand,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showBreakdown = true
                    }
                )
            }

            item {
                val due = state.deadlinesDueThisWeek
                LamlaNavRow(
                    icon = Icons.AutoMirrored.Outlined.EventNote,
                    title = "Deadlines",
                    subtitle = when (due) {
                        0 -> "Nothing due in the next 7 days"
                        1 -> "1 due in the next 7 days"
                        else -> "$due due in the next 7 days"
                    },
                    // Light up in the "crunch" hue only when something is actually due,
                    // so the row stays calm on a clear week and pulls focus when it matters.
                    accent = if (due > 0) MaterialTheme.lamla.colors.stressCrunch
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    badge = if (due > 0) "$due" else null,
                    onClick = onOpenDeadlines
                )
            }

            item {
                SectionLabel(
                    text = "Today's flow",
                    trailing = "${state.flow.size} item${if (state.flow.size == 1) "" else "s"}"
                )
            }

            if (state.flow.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(
                        title = "Nothing on for today.",
                        body = "A quiet day. Use it to get ahead, or rest. Both count.",
                        icon = Icons.Outlined.WbSunny
                    )
                }
            } else {
                items(state.flow, key = { itemKey(it) }) { flowItem ->
                    FlowRow(
                        item = flowItem,
                        nowMinutes = currentNowMinutes(),
                        onClassClick = onClassClick,
                        onDeadlineClick = onDeadlineClick,
                        onStudyClick = { onStartPomodoro() },
                        onPersonalClick = onPersonalEventClick,
                        // The timeline reorders as the day progresses and items appear /
                        // disappear; animate so rows slide rather than jump.
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Floating quick-capture button - bottom-right, floats above the nav.
        QuickCaptureFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = MaterialTheme.lamla.spacing.gutter, bottom = tabBottomInset()),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showCaptureSheet = true
            }
        )
    }

    if (showBreakdown) {
        StressBreakdownSheet(
            score = state.stressScore,
            contributions = state.stressContributions,
            classLoad = state.stressClassLoad,
            onDismiss = { showBreakdown = false },
            onDeadlineClick = { id ->
                showBreakdown = false
                onDeadlineClick(id)
            }
        )
    }

    if (showCaptureSheet) {
        app.lamla.presentation.screens.capture.QuickCaptureSheet(
            activeCourse = state.activeCourseAtNow,
            allCourses = state.courses.values.toList(),
            onDismiss = { showCaptureSheet = false },
            onScheduleEvent = onScheduleEvent
        )
    }
}

/** Stable key for LazyColumn item recomposition. */
private fun itemKey(item: TodayFlow.Item): String = when (item) {
    is TodayFlow.Item.ClassItem -> "c-${item.session.id}"
    is TodayFlow.Item.DeadlineItem -> "d-${item.deadline.id}"
    is TodayFlow.Item.StudyItem -> "s-${item.session.id}"
    is TodayFlow.Item.PersonalItem -> "p-${item.event.id}"
}

private fun currentNowMinutes(): Int =
    java.time.LocalTime.now().toSecondOfDay() / 60

@Composable
private fun HomeHeader(
    greeting: String,
    userName: String,
    today: java.time.LocalDate
) {
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val date = today.format(DateTimeFormatter.ofPattern("d MMM"))
    // The one true "fire layer" hero on Home: the greeting reads in calm onSurface,
    // but the student's own name is set in the ember gradient so it glows like the
    // signature warmth. Falls back to a clean, name-less greeting when unset.
    val ember = MaterialTheme.lamla.gradients.emberLinear
    val onSurface = MaterialTheme.colorScheme.onSurface
    val headline = remember(greeting, userName, ember, onSurface) {
        buildAnnotatedString {
            if (userName.isNotBlank()) {
                withStyle(SpanStyle(color = onSurface)) { append("$greeting, ") }
                withStyle(SpanStyle(brush = ember)) { append(userName) }
            } else {
                withStyle(SpanStyle(color = onSurface)) { append(greeting) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$dayName · $date".uppercase(),
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.displaySmall
        )
        // The masthead rule: a short ember stroke under the serif greeting -
        // the same signature gesture as a magazine's folio line.
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 44.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.lamla.gradients.emberHorizontal())
        )
    }
}

/**
 * Shown only when the OS is blocking notifications. A warm, ember-lit card so it
 * reads as "act on this" without feeling like a hard error; tapping it opens the
 * system notification settings for the app.
 */
@Composable
private fun NotificationsOffBanner(onEnable: () -> Unit) {
    val ember = MaterialTheme.lamla.gradients.emberGlow
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glow(ember, shape, radius = 14.dp, alpha = 0.32f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, ember.copy(alpha = 0.5f), shape)
            .clickable(onClick = onEnable)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsOff,
            contentDescription = "Notifications are disabled",
            tint = ember,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Notifications are off",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reminders can't reach you until you turn them on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Turn on",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ember
        )
    }
}

@Composable
private fun ExactAlarmsOffBanner(onEnable: () -> Unit) {
    val ember = MaterialTheme.lamla.gradients.emberGlow
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glow(ember, shape, radius = 14.dp, alpha = 0.32f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, ember.copy(alpha = 0.5f), shape)
            .clickable(onClick = onEnable)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Alarm,
            contentDescription = "Exact alarms are disabled",
            tint = ember,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Reminders may arrive late",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Allow exact alarms so class and exam alerts fire on the minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Allow",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ember
        )
    }
}

/**
 * A single row in the timeline.
 *
 * Visual structure (left to right):
 *   [time column] [vertical rail with type indicator] [content card]
 *
 * The rail visually links related rows (continuous line) and signals item type
 * via the indicator shape: ● class, ◇ deadline, ▢ study, ○ personal.
 */
@Composable
private fun FlowRow(
    item: TodayFlow.Item,
    nowMinutes: Int,
    onClassClick: (Long) -> Unit,
    onDeadlineClick: (Long) -> Unit,
    onStudyClick: (Long) -> Unit,
    onPersonalClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPast = item.startMinutes < nowMinutes && when (item) {
        is TodayFlow.Item.ClassItem -> item.endMinutes < nowMinutes
        else -> true
    }
    val isHappeningNow = when (item) {
        is TodayFlow.Item.ClassItem -> nowMinutes in item.startMinutes..item.endMinutes
        else -> false
    }
    val opacity = if (isPast && !isHappeningNow) 0.45f else 1f

    val timeStr = formatTime(item.startMinutes)
    val courseColor = item.course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacity),
        verticalAlignment = Alignment.Top
    ) {
        // Time column
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = timeStr,
                style = LamlaTextStyles.Metric,
                color = if (isHappeningNow) MaterialTheme.lamla.colors.timelineNow
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isHappeningNow) FontWeight.SemiBold else FontWeight.Medium
            )
            if (item is TodayFlow.Item.ClassItem) {
                Text(
                    text = formatTime(item.endMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        // Rail
        TimelineRail(
            isCurrent = isHappeningNow,
            itemColor = courseColor,
            kind = railKind(item)
        )
        Spacer(Modifier.size(12.dp))
        // Content
        when (item) {
            is TodayFlow.Item.ClassItem -> ClassCard(item, isHappeningNow, onClassClick)
            is TodayFlow.Item.DeadlineItem -> DeadlineCard(item, onDeadlineClick)
            is TodayFlow.Item.StudyItem -> StudyCard(item, onStudyClick)
            is TodayFlow.Item.PersonalItem -> PersonalCard(item, onClick = { onPersonalClick(item.event.id) })
        }
    }
}

private enum class RailKind { Class, Deadline, Study, Personal }
private fun railKind(item: TodayFlow.Item): RailKind = when (item) {
    is TodayFlow.Item.ClassItem -> RailKind.Class
    is TodayFlow.Item.DeadlineItem -> RailKind.Deadline
    is TodayFlow.Item.StudyItem -> RailKind.Study
    is TodayFlow.Item.PersonalItem -> RailKind.Personal
}

@Composable
private fun TimelineRail(
    isCurrent: Boolean,
    itemColor: Color,
    kind: RailKind
) {
    val railColor = MaterialTheme.lamla.colors.timelineRail
    Column(
        modifier = Modifier
            .width(14.dp)
            .height(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(8.dp)
                .background(railColor)
        )
        // Indicator
        val indicatorColor = if (isCurrent) MaterialTheme.lamla.colors.timelineNow else itemColor
        when (kind) {
            RailKind.Class -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            RailKind.Deadline -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Transparent)
                    .border(1.5.dp, indicatorColor, RoundedCornerShape(2.dp))
            )
            RailKind.Study -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor.copy(alpha = 0.25f))
                    .border(1.dp, indicatorColor, RoundedCornerShape(2.dp))
            )
            RailKind.Personal -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.5.dp, indicatorColor, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .weight(1f)
                .background(railColor)
        )
    }
}

@Composable
private fun ClassCard(
    item: TodayFlow.Item.ClassItem,
    isHappeningNow: Boolean,
    onClick: (Long) -> Unit
) {
    val course = item.course
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(item.session.id) },
        color = if (isHappeningNow) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface,
        borderColor = if (isHappeningNow) accent.copy(alpha = 0.4f) else MaterialTheme.lamla.colors.hairline,
        // The class happening right now glows in its course color, so it reads as
        // the one "live" surface on the page.
        glowColor = if (isHappeningNow) accent else null,
        glowAlpha = 0.32f,
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course != null) {
                    LamlaChip(
                        label = course.code,
                        color = accent
                    )
                }
                if (isHappeningNow) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LIVE", style = LamlaTextStyles.SectionLabel, color = accent)
                    }
                }
            }
            Text(
                text = course?.name ?: "Class",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = "Location",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.session.venue.ifBlank { "Venue TBA" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeadlineCard(
    item: TodayFlow.Item.DeadlineItem,
    onClick: (Long) -> Unit
) {
    val course = item.course
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(item.deadline.id) },
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course != null) LamlaChip(label = course.code, color = accent)
                Text(
                    text = "DUE",
                    style = LamlaTextStyles.SectionLabel,
                    color = MaterialTheme.lamla.colors.stressCrunch
                )
            }
            Text(
                text = item.deadline.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${item.deadline.weightPercent.toInt()}% of grade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyCard(
    item: TodayFlow.Item.StudyItem,
    onClick: (Long) -> Unit
) {
    val course = item.course
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(item.session.id) },
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Study session",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "STUDY",
                    style = LamlaTextStyles.SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = course?.let { "${it.code} • ${it.name}" } ?: "Focused study time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val durationMin = ((item.session.scheduledEndEpochMs - item.session.scheduledStartEpochMs) / 60_000).toInt()
            Text(
                text = "${durationMin} min block",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonalCard(item: TodayFlow.Item.PersonalItem, onClick: () -> Unit) {
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PERSONAL",
                style = LamlaTextStyles.SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickCaptureFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // The one warm, lit call to action floating over the page - the shared
    // ember gesture, at its largest here because capture is Home's primary verb.
    LamlaFab(
        onClick = onClick,
        modifier = modifier,
        contentDescription = "Quick capture",
        size = 56.dp
    )
}

@Composable
private fun BatteryOptimizationBanner(onEnable: () -> Unit) {
    val ember = MaterialTheme.lamla.gradients.emberGlow
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glow(ember, shape, radius = 14.dp, alpha = 0.32f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, ember.copy(alpha = 0.5f), shape)
            .clickable(onClick = onEnable)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.BatteryAlert,
            contentDescription = "Battery optimization is on",
            tint = ember,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Battery optimization active",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reminders might be delayed by the OS. Disable optimization for reliable alerts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Fix",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ember
        )
    }
}

private fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
