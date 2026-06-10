package app.lamla.presentation.screens.attendance

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.AttendanceStatus
import app.lamla.domain.usecase.AttendanceStats.CourseAttendance
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import kotlin.math.roundToInt

/**
 * Theme-aware verdict colors, riding the stress tokens: sage = showed up,
 * amber = late, rust = missed. They flip to their dark-mode variants with the
 * theme and collapse correctly under the Monochrome accent — hard-coded hex
 * here would glow wrong on an obsidian surface.
 */
private data class VerdictColors(val present: Color, val late: Color, val absent: Color)

@Composable
private fun verdictColors(): VerdictColors {
    val c = MaterialTheme.lamla.colors
    return VerdictColors(present = c.stressChill, late = c.stressSteady, absent = c.stressCrunch)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pinning by viewModel.pinningVenue.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onPermissionsChanged() }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionsChanged() }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LamlaTopBar(title = "Attendance", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = MaterialTheme.lamla.spacing.gutter,
                end = MaterialTheme.lamla.spacing.gutter,
                top = 4.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                state.semesterName?.let {
                    Text(it, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { OverallHero(state) }

            if (state.today.isNotEmpty()) {
                item { SectionLabel("Today", trailing = "${state.today.size}") }
                items(state.today, key = { it.session.id }) { tc ->
                    TodayClassCard(
                        todayClass = tc,
                        onMark = { status -> viewModel.markToday(tc.session, status) },
                        onClear = { viewModel.clearToday(tc.session) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (state.courses.isEmpty()) {
                item {
                    EmptyState(
                        title = "No courses yet.",
                        body = "Add courses and classes to your timetable, then track who showed up here.",
                        icon = Icons.Outlined.CheckCircle
                    )
                }
            } else {
                item { SectionLabel("This semester", trailing = "Target ${(state.target * 100).roundToInt()}%") }
                item { TargetChips(target = state.target, onSet = viewModel::setTarget) }
                items(state.courses, key = { it.course.id }) { ca ->
                    CourseAttendanceCard(ca = ca, target = state.target, modifier = Modifier.animateItem())
                }
            }

            // Auto check-in
            item { SectionLabel("Auto check-in") }
            item {
                AutoMarkCard(
                    state = state,
                    onToggleAuto = { enabled ->
                        if (enabled && !state.hasLocationPermission) {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        viewModel.setAutoEnabled(enabled)
                    },
                    onGrantLocation = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onGrantBackground = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                )
            }

            if (state.hasVenues) {
                items(state.venues, key = { it.key }) { row ->
                    VenueCard(
                        row = row,
                        pinning = pinning == row.key,
                        enabled = state.hasLocationPermission,
                        onPin = { viewModel.pinVenue(row) },
                        onUnpin = { viewModel.unpinVenue(row) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

// --- Hero --------------------------------------------------------------------

@Composable
private fun OverallHero(state: AttendanceUiState) {
    val cs = MaterialTheme.colorScheme
    val verdict = verdictColors()
    val rate = state.overallRate
    val good = rate == null || rate >= state.target
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        glowColor = if (good) MaterialTheme.lamla.gradients.emberGlow else verdict.absent.copy(alpha = 0.5f),
        contentPadding = MaterialTheme.lamla.spacing.lg
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("OVERALL ATTENDANCE", style = LamlaTextStyles.SectionLabel, color = cs.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = pct(rate),
                    style = MaterialTheme.typography.displayMedium.copy(
                        brush = MaterialTheme.lamla.gradients.emberLinear,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = if (rate == null) "nothing logged yet" else if (good) "above your line" else "below your line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            RateBar(fraction = rate ?: 0f, color = if (good) verdict.present else verdict.absent, target = state.target)
            if (state.pinnedVenueCount > 0) {
                Text(
                    "${state.pinnedVenueCount} venue${if (state.pinnedVenueCount == 1) "" else "s"} pinned for auto check-in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}

// --- Today -------------------------------------------------------------------

@Composable
private fun TodayClassCard(
    todayClass: TodayClass,
    onMark: (AttendanceStatus) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val accent = Color(todayClass.course.colorArgb)
    val s = todayClass.session
    LamlaSurface(modifier = modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(todayClass.course.code, style = LamlaTextStyles.SectionLabel, color = accent)
                    Text(todayClass.course.name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text(
                        "${hhmm(s.startMinutes)}–${hhmm(s.endMinutes)}" + if (s.venue.isNotBlank()) " · ${s.venue}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            val verdict = verdictColors()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Present", verdict.present, todayClass.status == AttendanceStatus.Present) {
                    if (todayClass.status == AttendanceStatus.Present) onClear() else onMark(AttendanceStatus.Present)
                }
                StatusPill("Late", verdict.late, todayClass.status == AttendanceStatus.Late) {
                    if (todayClass.status == AttendanceStatus.Late) onClear() else onMark(AttendanceStatus.Late)
                }
                StatusPill("Absent", verdict.absent, todayClass.status == AttendanceStatus.Absent) {
                    if (todayClass.status == AttendanceStatus.Absent) onClear() else onMark(AttendanceStatus.Absent)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull)
    val bg = if (selected) color else color.copy(alpha = 0.10f)
    val fg = if (selected) Color.White else color
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

// --- Per-course --------------------------------------------------------------

@Composable
private fun CourseAttendanceCard(ca: CourseAttendance, target: Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val verdict = verdictColors()
    val accent = Color(ca.course.colorArgb)
    val rate = ca.rate
    val good = rate == null || rate >= target
    LamlaSurface(modifier = modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ca.course.code, style = LamlaTextStyles.SectionLabel, color = accent)
                    Text(ca.course.name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                }
                Text(
                    pct(rate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (good) cs.onSurface else verdict.absent
                )
            }
            RateBar(fraction = rate ?: 0f, color = if (good) verdict.present else verdict.absent, target = target)
            Text(
                buildString {
                    append("${ca.present} present")
                    if (ca.late > 0) append(" · ${ca.late} late")
                    append(" · ${ca.absent} absent")
                    if (ca.excused > 0) append(" · ${ca.excused} excused")
                },
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            Text(
                skipVerdict(ca, target),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ca.targetUnreachable(target)) verdict.absent else cs.onSurface
            )
        }
    }
}

private fun skipVerdict(ca: CourseAttendance, target: Float): String {
    val t = (target * 100).roundToInt()
    if (ca.totalPlanned == 0) {
        return if (ca.marked == 0) "No meetings logged yet — mark today's class above."
        else "${ca.attended}/${ca.marked} attended so far."
    }
    if (ca.targetUnreachable(target)) return "Can't reach $t% this term — finish as strong as you can."
    val canSkip = ca.canStillSkip(target) ?: 0
    if (canSkip > 0) return "You can still skip $canSkip and hold $t%."
    val mustAttend = ca.mustAttendOfRemaining(target) ?: 0
    return if (mustAttend > 0) "At the limit — attend all $mustAttend remaining to keep $t%."
    else "Right on the $t% line."
}

// --- Auto check-in -----------------------------------------------------------

@Composable
private fun AutoMarkCard(
    state: AttendanceUiState,
    onToggleAuto: (Boolean) -> Unit,
    onGrantLocation: () -> Unit,
    onGrantBackground: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LamlaSurface(modifier = Modifier.fillMaxWidth(), color = cs.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Geofence auto check-in", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text(
                        "Pin a lecture hall and Lamla marks you present the moment you arrive — even with the app closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Switch(checked = state.autoEnabled, onCheckedChange = onToggleAuto)
            }

            if (state.autoEnabled) {
                if (!state.hasLocationPermission) {
                    PermissionRow(
                        text = "Location access is needed to detect when you arrive.",
                        action = "Grant",
                        onClick = onGrantLocation
                    )
                } else if (!state.hasBackgroundPermission) {
                    PermissionRow(
                        text = "For check-in while the app is closed, allow location \"all the time\".",
                        action = "Allow",
                        onClick = onGrantBackground
                    )
                } else {
                    Text(
                        "Ready. Pin the venues below to arm them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = verdictColors().present
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(text: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
        LamlaSecondaryButton(label = action, onClick = onClick)
    }
}

@Composable
private fun VenueCard(
    row: VenueRow,
    pinning: Boolean,
    enabled: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    LamlaSurface(modifier = modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (row.pinned) Icons.Outlined.LocationOn else Icons.Outlined.MyLocation,
                contentDescription = null,
                tint = if (row.pinned) verdictColors().present else cs.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.displayName, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                Text(
                    if (row.courseCodes.isEmpty()) "Venue" else row.courseCodes.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            when {
                pinning -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                row.pinned -> LamlaSecondaryButton(label = "Unpin", onClick = onUnpin)
                else -> LamlaSecondaryButton(label = "Pin here", onClick = onPin, enabled = enabled)
            }
        }
    }
}

// --- Bits --------------------------------------------------------------------

@Composable
private fun TargetChips(target: Float, onSet: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.70f, 0.75f, 0.80f, 0.90f).forEach { t ->
            LamlaChip(
                label = "${(t * 100).roundToInt()}%",
                selected = kotlin.math.abs(t - target) < 0.001f,
                onClick = { onSet(t) }
            )
        }
    }
}

/** Thin rounded progress track with a tick at the [target] line. */
@Composable
private fun RateBar(fraction: Float, color: Color, target: Float) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "rate"
    )
    val track = MaterialTheme.lamla.colors.timelineRail
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        // Target tick
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(target.coerceIn(0f, 1f))
                .padding(end = 0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            )
        }
    }
}

private fun pct(rate: Float?): String = rate?.let { "${(it * 100).roundToInt()}%" } ?: "—"

private fun hhmm(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
