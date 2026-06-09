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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
    var showGoalPicker by remember { mutableStateOf(false) }

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
                state.targetPlan?.let { plan ->
                    if (plan.outcome !is GradeProjection.TargetOutcome.NoData) {
                        item { TargetCard(state, plan, onEditGoal = { showGoalPicker = true }) }
                    }
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

    if (showGoalPicker) {
        GoalPickerDialog(
            currentTarget = state.targetCwa,
            isCustom = state.targetIsCustom,
            onSave = { cwa -> viewModel.setTargetCwa(cwa); showGoalPicker = false },
            onDismiss = { showGoalPicker = false }
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

// --- Target engine ----------------------------------------------------------

/**
 * The goal-driven plan. Reverses the projection: pick a class/CWA, and this works out
 * the single effort level you need on everything still outstanding to land it — then
 * shows where each course finishes if you hold that line.
 */
@Composable
private fun TargetCard(
    state: GradesUiState,
    plan: GradeProjection.TargetPlan,
    onEditGoal: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LamlaSurface(modifier = Modifier.fillMaxWidth(), color = cs.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header — tappable to change the goal.
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEditGoal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (state.targetIsCustom) "YOUR GOAL" else "AIM · NEXT CLASS",
                        style = LamlaTextStyles.SectionLabel,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        "${plan.targetClass.label} · ${fmt(plan.targetCwa)} CWA",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )
                }
                Icon(Icons.Outlined.Flag, contentDescription = "Change goal", tint = cs.onSurfaceVariant)
            }

            HorizontalDivider(color = MaterialTheme.lamla.colors.hairline)

            // Verdict.
            when (val o = plan.outcome) {
                is GradeProjection.TargetOutcome.Reachable -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${fmt(o.percentOnRemaining)}%",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    brush = MaterialTheme.lamla.gradients.emberLinear,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                "on every mark still to come",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 7.dp)
                            )
                        }
                        Text(
                            "Hold a ${fmt(plan.requiredSwa)} average across this semester's ${plan.lines.sumOf { it.credits }} credits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
                is GradeProjection.TargetOutcome.Secured -> VerdictLine(
                    title = "Secured.",
                    body = "${plan.targetClass.label} is locked in — even a wipeout on what's left keeps you at ${fmt(o.floorCwa)}.",
                    accent = cs.tertiary
                )
                is GradeProjection.TargetOutcome.OutOfReach -> VerdictLine(
                    title = "Out of reach this term.",
                    body = "Max everything left and you land ${fmt(o.bestCwa)} (${o.bestClass.label}). Bank it and revisit next semester.",
                    accent = cs.error
                )
                GradeProjection.TargetOutcome.NoData -> Unit
            }

            // Per-course plan — the finish line each course lands on if you hold the effort.
            val showLines = plan.outcome is GradeProjection.TargetOutcome.Reachable ||
                plan.outcome is GradeProjection.TargetOutcome.OutOfReach
            if (showLines && plan.lines.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.lamla.colors.hairline)
                Text(
                    if (plan.outcome is GradeProjection.TargetOutcome.OutOfReach) "BEST-CASE FINISH" else "FINISH LINE",
                    style = LamlaTextStyles.SectionLabel,
                    color = cs.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    plan.lines.forEach { TargetLineRow(it) }
                }
            }
        }
    }
}

@Composable
private fun VerdictLine(title: String, body: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TargetLineRow(line: GradeProjection.TargetLine) {
    val cs = MaterialTheme.colorScheme
    val accent = Color(line.colorArgb)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            line.code,
            style = LamlaTextStyles.SectionLabel,
            color = accent,
            modifier = Modifier.width(60.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                line.name,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (line.locked) "Locked · ${fmt(line.gradedWeight)}% graded"
                else "${line.credits} cr · ${fmt(line.gradedWeight)}% graded so far",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        Text(
            if (line.locked) fmt(line.pointsEarned) else fmt(line.projectedFinishMark),
            style = LamlaTextStyles.Metric,
            color = if (line.locked) cs.onSurfaceVariant else cs.onSurface
        )
    }
}

@Composable
private fun GoalPickerDialog(
    currentTarget: Float?,
    isCustom: Boolean,
    onSave: (Float?) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        GradeProjection.DegreeClass.First,
        GradeProjection.DegreeClass.SecondUpper,
        GradeProjection.DegreeClass.SecondLower,
        GradeProjection.DegreeClass.Third
    )
    // selected == null → auto (next class up). Otherwise a concrete target CWA.
    var selected by remember { mutableStateOf(if (isCustom) currentTarget else null) }
    var customText by remember {
        mutableStateOf(
            if (isCustom && currentTarget != null && presets.none { kotlin.math.abs(it.minCwa - currentTarget) < 0.5f })
                fmt(currentTarget) else ""
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set your goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Aim for a class of degree, or enter a custom CWA. Lamla works out exactly what you need on what's left.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LamlaChip(
                        label = "Auto",
                        selected = selected == null && customText.isBlank(),
                        onClick = { selected = null; customText = "" }
                    )
                    presets.forEach { dc ->
                        LamlaChip(
                            label = dc.label,
                            selected = customText.isBlank() && selected?.let { kotlin.math.abs(it - dc.minCwa) < 0.5f } == true,
                            onClick = { selected = dc.minCwa; customText = "" }
                        )
                    }
                }
                LamlaField("Custom CWA") {
                    LamlaTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        placeholder = "e.g. 68",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val custom = customText.trim().toFloatOrNull()?.coerceIn(0f, 100f)
                onSave(custom ?: selected)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
