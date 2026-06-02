package app.lamla.presentation.screens.grades

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.usecase.GradeProjection
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    onBack: () -> Unit,
    viewModel: GradesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPriorEdit by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Grades & CWA", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
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

            if (!state.hasAnyGrades) {
                item {
                    EmptyState(
                        title = "No marks yet.",
                        body = "Add the mark you scored on a deadline (its weight feeds the course), and your projected CWA appears here.",
                        icon = Icons.Outlined.Insights
                    )
                }
            } else {
                item { ProjectionHero(state, onResetAll = { viewModel.resetSimulation() }) }
                if (state.requiredSwaForNextClass != null && state.nextClassTarget != null) {
                    item { NextClassHint(state) }
                }
            }

            item { PriorStandingCard(state, onEdit = { showPriorEdit = true }) }

            if (state.courseGrades.isNotEmpty()) {
                item {
                    SectionLabel(
                        "This semester",
                        trailing = if (state.hasAnyGrades) "Drag to explore" else "${state.courseGrades.size}"
                    )
                }
                items(state.courseGrades, key = { it.course.id }) { cg ->
                    CourseGradeCard(
                        cg = cg,
                        simulatedMark = state.simulatedMarks[cg.course.id],
                        onSimulate = { viewModel.simulateMark(cg.course.id, it) },
                        onResetSimulation = { viewModel.clearSimulation(cg.course.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showPriorEdit) {
        PriorStandingDialog(
            initialCwa = state.priorCwa,
            initialCredits = state.priorCredits,
            onSave = { cwa, credits -> viewModel.setPriorStanding(cwa, credits); showPriorEdit = false },
            onDismiss = { showPriorEdit = false }
        )
    }
}

// --- Hero -------------------------------------------------------------------

@Composable
private fun ProjectionHero(state: GradesUiState, onResetAll: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ember = MaterialTheme.lamla.gradients.emberLinear
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        glowColor = MaterialTheme.lamla.gradients.emberGlow,
        contentPadding = MaterialTheme.lamla.spacing.lg
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.isSimulating) "WHAT-IF CWA" else "PROJECTED CWA",
                    style = LamlaTextStyles.SectionLabel,
                    color = if (state.isSimulating) cs.tertiary else cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // Only offer a global escape hatch once a hypothetical is in play.
                if (state.isSimulating) {
                    Text(
                        text = "Reset all",
                        style = LamlaTextStyles.SectionLabel,
                        color = cs.tertiary,
                        modifier = Modifier.clickable(onClick = onResetAll)
                    )
                }
            }
            // The hero number eases up from 0 when the screen lands - the projection
            // "tallies itself" - then glides smoothly to track any what-if drag, so the
            // CWA visibly responds to the sliders below.
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val target = state.projectedCwa ?: 0f
            val animatedCwa by animateFloatAsState(
                targetValue = if (shown) target else 0f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "cwa"
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (state.projectedCwa != null) fmt(animatedCwa) else "—",
                    style = MaterialTheme.typography.displayMedium.copy(brush = ember, fontWeight = FontWeight.SemiBold)
                )
                state.projectedClass?.let {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.lamla.colors.hairline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock(
                    label = "THIS SEMESTER",
                    value = state.projectedSwa?.let { fmt(it) } ?: "—",
                    sub = "${state.projectedCredits} cr projected"
                )
                state.nextClassTarget?.let { target ->
                    val gap = (target - (state.projectedCwa ?: 0f)).coerceAtLeast(0f)
                    val nextLabel = GradeProjection.classOf(target).label
                    MetricBlock(
                        label = "TO NEXT CLASS",
                        value = "+${fmt(gap)}",
                        sub = nextLabel,
                        alignEnd = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, sub: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NextClassHint(state: GradesUiState) {
    val target = state.nextClassTarget ?: return
    val req = state.requiredSwaForNextClass ?: return
    val nextLabel = GradeProjection.classOf(target).label
    val message = when {
        req > 100f -> "$nextLabel is out of reach this semester — keep banking marks and revisit next term."
        req <= 0f -> "You're already on track for $nextLabel."
        else -> "Average ${fmt(req)} across this semester's ${state.projectedCredits} credits to reach $nextLabel."
    }
    LamlaSurface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Prior standing ---------------------------------------------------------

@Composable
private fun PriorStandingCard(state: GradesUiState, onEdit: () -> Unit) {
    LamlaSurface(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("PRIOR STANDING", style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.priorCwa != null) {
                    Text(
                        "CWA ${fmt(state.priorCwa)} over ${state.priorCredits} credits",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Layered under this semester's projection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Add your CWA so far", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Tap to enter your cumulative average and credits earned. First semester? Leave it blank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Outlined.Edit, contentDescription = "Edit prior standing", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PriorStandingDialog(
    initialCwa: Float?,
    initialCredits: Int,
    onSave: (Float?, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var cwaText by remember { mutableStateOf(initialCwa?.let { fmt(it) } ?: "") }
    var creditsText by remember { mutableStateOf(if (initialCredits > 0) initialCredits.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prior standing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Your cumulative average so far and the credit hours it covers. Leave the CWA blank if this is your first semester.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LamlaField("CWA so far") {
                    LamlaTextField(value = cwaText, onValueChange = { cwaText = it }, placeholder = "62.4", keyboardType = KeyboardType.Decimal)
                }
                LamlaField("Credits earned") {
                    LamlaTextField(value = creditsText, onValueChange = { creditsText = it }, placeholder = "54", keyboardType = KeyboardType.Number)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cwa = cwaText.trim().toFloatOrNull()?.coerceIn(0f, 100f)
                val credits = creditsText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                onSave(cwa, credits)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// --- Per-course -------------------------------------------------------------

@Composable
private fun CourseGradeCard(
    cg: CourseGrade,
    simulatedMark: Float?,
    onSimulate: (Float) -> Unit,
    onResetSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val s = cg.standing
    val accent = Color(cg.course.colorArgb)
    // Projectable = there's a current pace to extrapolate, so a what-if has meaning.
    val projectable = cg.projectedMark != null
    val isSimulating = simulatedMark != null
    val displayMark = simulatedMark ?: cg.projectedMark
    LamlaSurface(modifier = modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(cg.course.code, style = LamlaTextStyles.SectionLabel, color = accent)
                    Text(cg.course.name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayMark?.let { fmt(it) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            isSimulating -> accent
                            displayMark != null -> cs.onSurface
                            else -> cs.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (isSimulating) "what-if" else "projected",
                        style = LamlaTextStyles.SectionLabel,
                        color = if (isSimulating) accent else cs.onSurfaceVariant
                    )
                }
            }

            val detail = when {
                s.gradedWeight <= 0f && s.definedWeight <= 0f ->
                    "No assessments yet. Add deadlines with weights to track this course."
                s.gradedWeight <= 0f ->
                    "Nothing graded yet · ${fmt(s.definedWeight)}% of weight defined."
                else ->
                    "Scored ${fmt(s.markOnGraded ?: 0f)} on the ${fmt(s.gradedWeight)}% graded · ${fmt(s.remainingWeight)}% still to come."
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)

            if (s.gradedWeight > 0f && s.remainingWeight > 0.01f) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeChip("Floor", s.floorMark, cs)
                    RangeChip("Ceiling", s.ceilingMark, cs)
                }
            }

            if (s.definedWeight > 0f && !s.coverageComplete) {
                Text(
                    "Weights add up to ${fmt(s.definedWeight)}% — add the rest for a complete projection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Drag this course to a hypothetical mark and the hero CWA re-projects live.
            if (projectable) {
                WhatIfSlider(
                    value = displayMark ?: 0f,
                    accent = accent,
                    isSimulating = isSimulating,
                    onValueChange = onSimulate,
                    onReset = onResetSimulation
                )
            }
        }
    }
}

@Composable
private fun WhatIfSlider(
    value: Float,
    accent: Color,
    isSimulating: Boolean,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "WHAT IF",
                style = LamlaTextStyles.SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (isSimulating) {
                Text(
                    text = "Reset",
                    style = LamlaTextStyles.SectionLabel,
                    color = accent,
                    modifier = Modifier.clickable(onClick = onReset)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.lamla.colors.timelineRail
            )
        )
    }
}

@Composable
private fun RangeChip(label: String, value: Float, cs: ColorScheme) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label.uppercase(), style = LamlaTextStyles.SectionLabel, color = cs.onSurfaceVariant)
        Text(fmt(value), style = LamlaTextStyles.Metric, color = cs.onSurface)
    }
}

/** "62.0" → "62", "62.4" → "62.4". One decimal, trimmed. */
private fun fmt(v: Float): String {
    val rounded = Math.round(v * 10f) / 10f
    return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString() else rounded.toString()
}
