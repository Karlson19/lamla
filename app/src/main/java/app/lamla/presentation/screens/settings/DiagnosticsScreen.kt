package app.lamla.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.lamla.domain.usecase.UpcomingAlarms
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.components.SectionLabel
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.lamla
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Self-test screen.
 *
 * The audience here is a slightly anxious user (or a developer debugging on a
 * real phone). It answers three questions:
 *
 *   1. "Did the background system actually run recently?"  → lastRescheduleAt
 *   2. "Did the boot receiver fire after my last restart?" → lastBootAt
 *   3. "What reminders are actually queued up?"            → upcoming list
 *
 * Plus a "Run reschedule now" button so they can force-trigger the worker and
 * watch the timestamp update — proves the wiring works in <2 seconds, no
 * waiting around for the daily tick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Diagnostics", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = MaterialTheme.lamla.spacing.gutter,
                end = MaterialTheme.lamla.spacing.gutter,
                top = 12.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "What the reminder system is doing right now. " +
                        "If anything looks wrong, tap \"Run reschedule now\" — the timestamp should tick within a second.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Health card
            item {
                LamlaSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthRow(
                            label = "Exact alarms",
                            value = if (state.canScheduleExact) "Allowed" else "Denied — grant in system settings",
                            ok = state.canScheduleExact
                        )
                        HealthRow(
                            label = "Last reschedule run",
                            value = humanizeTimestamp(state.lastRescheduleAt),
                            ok = state.lastRescheduleAt > 0
                        )
                        HealthRow(
                            label = "Last boot received",
                            value = if (state.lastBootAt == 0L) "Never (since install)"
                            else humanizeTimestamp(state.lastBootAt),
                            ok = true  // null is fine if user hasn't rebooted since installing
                        )
                        HealthRow(
                            label = "Reminders queued",
                            value = "${state.totalUpcoming}",
                            ok = true
                        )
                    }
                }
            }

            item {
                LamlaButton(
                    label = "Run reschedule now",
                    leadingIcon = Icons.Outlined.Refresh,
                    onClick = { viewModel.runRescheduleNow() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionLabel("Upcoming reminders", trailing = "next ${state.upcoming.size}") }

            if (state.upcoming.isEmpty()) {
                item {
                    Text(
                        text = "Nothing queued — add a class, deadline, or exam, then come back.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.upcoming, key = { "${it.triggerAtEpochMs}-${it.kind}-${it.title}" }) { entry ->
                    AlarmEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (ok) MaterialTheme.lamla.colors.stressChill
                        else MaterialTheme.lamla.colors.stressCrunch
                    )
            )
            Text(value, style = LamlaTextStyles.Metric, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AlarmEntryRow(entry: UpcomingAlarms.Entry) {
    val accent = entry.courseColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val zone = ZoneId.systemDefault()
    val triggerLabel = remember(entry.triggerAtEpochMs) {
        formatTrigger(entry.triggerAtEpochMs, zone)
    }
    val kindLabel = when (entry.kind) {
        UpcomingAlarms.Entry.Kind.Class -> "CLASS"
        UpcomingAlarms.Entry.Kind.OfficeHours -> "OFFICE HOURS"
        UpcomingAlarms.Entry.Kind.Deadline -> "DEADLINE"
        UpcomingAlarms.Entry.Kind.DeadlineImminent -> "DUE SOON"
        UpcomingAlarms.Entry.Kind.Exam -> "EXAM"
        UpcomingAlarms.Entry.Kind.StudySession -> "STUDY"
    }

    LamlaSurface(modifier = Modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Accent rail
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.7f))
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        kindLabel,
                        style = LamlaTextStyles.SectionLabel,
                        color = accent
                    )
                    Text(
                        triggerLabel,
                        style = LamlaTextStyles.Metric,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun humanizeTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return "Never yet"
    val now = System.currentTimeMillis()
    val deltaMs = now - epochMs
    val minutes = deltaMs / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        deltaMs < 60_000 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
            ldt.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}

private fun formatTrigger(epochMs: Long, zone: ZoneId): String {
    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone)
    val today = LocalDate.now(zone)
    val date = ldt.toLocalDate()
    val timeStr = "%02d:%02d".format(ldt.hour, ldt.minute)
    return when {
        date == today -> "Today $timeStr"
        date == today.plusDays(1) -> "Tomorrow $timeStr"
        date.isBefore(today.plusDays(7)) -> "${ldt.dayOfWeek.toString().take(3)} $timeStr"
        else -> ldt.format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
    }
}
