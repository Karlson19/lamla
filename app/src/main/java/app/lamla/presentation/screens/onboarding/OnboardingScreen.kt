package app.lamla.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaGhostButton
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/**
 * Onboarding, four steps.
 *
 *   Step 0: Welcome (brand badge + tone, no input)
 *   Step 1: Name ("What should we call you?"), optional, can skip
 *   Step 2: Semester name
 *   Step 3: Start/end dates, completes onboarding and lands on Home
 *
 * Why ask the name explicitly: a single line in the greeting ("Good morning, Karlson")
 * transforms how personal the app feels, for the cost of one extra step. The data is
 * stored locally only; it is a preference, not an account.
 */
private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var userName by rememberSaveable { mutableStateOf("") }
    var semesterName by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var endDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val scope = rememberCoroutineScope()

    val totalSteps = 4
    val datesValid = startDate != null && endDate != null && endDate!!.isAfter(startDate)
    val aura = MaterialTheme.lamla.gradients.auraWarm

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Warm sunrise bloom from the top, setting the tone the moment the
                // app opens.
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = aura,
                            center = Offset(size.width * 0.5f, 0f),
                            radius = size.height * 0.45f
                        )
                    )
                }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = MaterialTheme.lamla.spacing.gutter)
        ) {
            // Step progress: the current segment grows, completed ones stay filled.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { i ->
                    val active = i == step
                    val done = i < step
                    val weight by animateDpAsState(
                        targetValue = if (active) 28.dp else 10.dp,
                        animationSpec = MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.medium2),
                        label = "step-seg"
                    )
                    Box(
                        modifier = Modifier
                            .then(if (active) Modifier.width(weight) else Modifier.weight(1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (active || done) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.lamla.colors.timelineRail
                            )
                    )
                }
            }

            Spacer(Modifier.height(44.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(300)) + slideIntoContainer(Left, tween(400))).togetherWith(
                        fadeOut(tween(200)) + slideOutOfContainer(Left, tween(400))
                    )
                },
                label = "onboarding-step"
            ) { current ->
                when (current) {
                    0 -> StepWelcome()
                    1 -> StepName(value = userName, onChange = { userName = it })
                    2 -> StepSemesterName(value = semesterName, onChange = { semesterName = it })
                    3 -> StepDates(
                        start = startDate,
                        end = endDate,
                        valid = datesValid,
                        onPickStart = { startDate = it },
                        onPickEnd = { endDate = it }
                    )
                }
            }

            // Footer actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    LamlaGhostButton(
                        label = "Back",
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    )
                }
                // On the optional name step, offer Skip while it is blank.
                if (step == 1 && userName.isBlank()) {
                    LamlaGhostButton(
                        label = "Skip",
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
                LamlaButton(
                    label = when (step) {
                        0 -> "Get started"
                        in 1..2 -> "Continue"
                        else -> "Finish setup"
                    },
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> step = 2     // name is optional, always allowed to proceed
                            2 -> if (semesterName.isNotBlank()) step = 3
                            3 -> if (datesValid) {
                                scope.launch {
                                    viewModel.complete(
                                        userName = userName,
                                        semesterName = semesterName,
                                        start = startDate!!,
                                        end = endDate!!
                                    )
                                    onComplete()
                                }
                            }
                        }
                    },
                    enabled = when (step) {
                        0 -> true
                        1 -> true                          // skippable
                        2 -> semesterName.isNotBlank()
                        else -> datesValid
                    },
                    modifier = Modifier.weight(if (step > 0) 2f else 1f)
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------- */
/* Steps                                                                        */
/* ---------------------------------------------------------------------------- */

@Composable
private fun StepWelcome() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        BrandBadge()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "LAMLA",
                style = LamlaTextStyles.SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your day, considered.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Timetable, deadlines and study sessions, quietly tracked. Reminders fire right when they need to. No cloud. No accounts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The Lamla mark: a clock at five-to-twelve, nodding to "Last Minute Learners".
 * Drawn rather than bundled so it recolors with the active theme (hands take the
 * accent, ticks take the ink color).
 */
@Composable
private fun BrandBadge() {
    val cs = MaterialTheme.colorScheme
    val tick = cs.onSurface
    // The minute hand (the "five to" of five-to-twelve) burns in the ember coral,
    // and the badge carries a matching warm halo: the brand mark, lit.
    val ember = MaterialTheme.lamla.gradients.emberGlow
    Box(
        modifier = Modifier
            .size(88.dp)
            .glow(ember, CircleShape, radius = 24.dp, alpha = 0.5f)
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh, CircleShape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Hour ticks (cardinal ones slightly longer).
            for (h in 0 until 12) {
                val a = Math.toRadians(h * 30.0)
                val dx = sin(a).toFloat()
                val dy = -cos(a).toFloat()
                val outer = r - 1.dp.toPx()
                val len = if (h % 3 == 0) 5.dp.toPx() else 3.dp.toPx()
                drawLine(
                    color = tick,
                    start = Offset(c.x + dx * (outer - len), c.y + dy * (outer - len)),
                    end = Offset(c.x + dx * outer, c.y + dy * outer),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Hour hand, just shy of 12.
            val hourA = Math.toRadians(357.5)
            drawLine(
                color = tick,
                start = c,
                end = Offset(c.x + sin(hourA).toFloat() * r * 0.42f, c.y - cos(hourA).toFloat() * r * 0.42f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Minute hand, pointing at 11 (the "five to" of five-to-twelve), in ember.
            val minA = Math.toRadians(330.0)
            drawLine(
                color = ember,
                start = c,
                end = Offset(c.x + sin(minA).toFloat() * r * 0.66f, c.y - cos(minA).toFloat() * r * 0.66f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = ember, radius = 2.2.dp.toPx(), center = c)
        }
    }
}

@Composable
private fun StepName(value: String, onChange: (String) -> Unit) {
    val kb = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "HELLO",
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        EditorialInput(
            value = value,
            onChange = onChange,
            placeholder = "First name's fine",
            capitalization = KeyboardCapitalization.Words,
            onDone = { kb?.hide() }
        )
        Text(
            text = "Stays on your device. Used only to greet you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepSemesterName(value: String, onChange: (String) -> Unit) {
    val kb = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "WHICH SEMESTER",
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "What are we calling this one?",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        EditorialInput(
            value = value,
            onChange = onChange,
            placeholder = "e.g. Year 3, Semester 1",
            capitalization = KeyboardCapitalization.Words,
            onDone = { kb?.hide() }
        )
    }
}

/**
 * Big editorial text input used by the Name and Semester steps: large type on a
 * hairline underline. Distinctive without the heavy chrome of an outlined field.
 */
@Composable
private fun EditorialInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    onDone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = capitalization
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    inner()
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.lamla.colors.hairline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepDates(
    start: LocalDate?,
    end: LocalDate?,
    valid: Boolean,
    onPickStart: (LocalDate) -> Unit,
    onPickEnd: (LocalDate) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "WHEN",
            style = LamlaTextStyles.SectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "When does it run?",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        DateCard(label = "STARTS", date = start, onClick = { showStartPicker = true })
        DateCard(label = "ENDS", date = end, onClick = { showEndPicker = true })

        if (start != null && end != null && !valid) {
            Text(
                text = "The end date needs to come after the start date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showStartPicker) {
        DatePickerSheet(
            initial = start ?: LocalDate.now(),
            onPicked = { onPickStart(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerSheet(
            initial = end ?: (start ?: LocalDate.now()).plusMonths(4),
            onPicked = { onPickEnd(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
private fun DateCard(label: String, date: LocalDate?, onClick: () -> Unit) {
    LamlaSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = MaterialTheme.lamla.spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = date?.format(DateFmt) ?: "Pick a date",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (date != null) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis ?: return@TextButton
                onPicked(java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
