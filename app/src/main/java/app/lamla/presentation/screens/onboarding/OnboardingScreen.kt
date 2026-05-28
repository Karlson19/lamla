package app.lamla.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaGhostButton
import app.lamla.ui.components.LamlaSecondaryButton
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Onboarding — four steps.
 *
 *   Step 0: Welcome (no input, just sets tone)
 *   Step 1: Name ("What should we call you?")  — optional, can skip
 *   Step 2: Semester name
 *   Step 3: Start/end dates → completes onboarding, lands on Home
 *
 * Why ask the name explicitly: a single line in the greeting ("Good morning, Karlson")
 * transforms how personal the app feels — for the cost of one extra step. The data
 * is stored locally only; it's a *preference*, not an account.
 */
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.lamla.spacing.gutter)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Step dot indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { i ->
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.lamla.colors.timelineRail
                            )
                    )
                }
            }

            Spacer(Modifier.weight(0.3f))

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
                    1 -> StepName(
                        value = userName,
                        onChange = { userName = it }
                    )
                    2 -> StepSemesterName(
                        value = semesterName,
                        onChange = { semesterName = it }
                    )
                    3 -> StepDates(
                        start = startDate,
                        end = endDate,
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
                // On the (optional) name step, offer a Skip button if blank.
                if (step == 1 && userName.isBlank()) {
                    LamlaGhostButton(
                        label = "Skip",
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
                LamlaButton(
                    label = when (step) {
                        0 -> "Begin"
                        in 1..2 -> "Continue"
                        else -> "Finish"
                    },
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> step = 2     // name is optional; always allowed to proceed
                            2 -> if (semesterName.isNotBlank()) step = 3
                            3 -> if (startDate != null && endDate != null) {
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
                        else -> startDate != null && endDate != null
                    },
                    modifier = Modifier.weight(if (step > 0) 2f else 1f)
                )
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            text = "Timetable, deadlines, study sessions — quietly tracked. Reminders fire when they need to. No cloud, no accounts.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepName(
    value: String,
    onChange: (String) -> Unit
) {
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
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { kb?.hide() }),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "First name's fine",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.lamla.colors.hairline)
        Text(
            text = "Stays on your device. Used only to greet you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepSemesterName(
    value: String,
    onChange: (String) -> Unit
) {
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
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { kb?.hide() }),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "e.g. Year 3 · Semester 1",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LamlaSecondaryButton(
                label = start?.let { it.toString() } ?: "Start date",
                onClick = { showStartPicker = true },
                leadingIcon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
            LamlaSecondaryButton(
                label = end?.let { it.toString() } ?: "End date",
                onClick = { showEndPicker = true },
                leadingIcon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.weight(1f)
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
