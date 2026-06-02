package app.lamla.presentation.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.StudySessionRepository
import app.lamla.domain.model.StudySession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

enum class PomoPhase { Focus, ShortBreak, LongBreak }

data class PomodoroUiState(
    val phase: PomoPhase = PomoPhase.Focus,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val completedFocusCycles: Int = 0,
    val cyclesUntilLong: Int = 4,
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15
) {
    val progress: Float get() = if (totalSeconds == 0) 0f else 1f - remainingSeconds.toFloat() / totalSeconds
    val timerLabel: String get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    val phaseLabel: String get() = when (phase) {
        PomoPhase.Focus -> "Focus"
        PomoPhase.ShortBreak -> "Short break"
        PomoPhase.LongBreak -> "Long break"
    }
    val isBreak: Boolean get() = phase != PomoPhase.Focus
}

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val studyRepo: StudySessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PomodoroUiState())
    val state: StateFlow<PomodoroUiState> = _state.asStateFlow()
    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val focus = prefs.pomodoroFocusMin.first()
            val short = prefs.pomodoroShortBreakMin.first()
            val long = prefs.pomodoroLongBreakMin.first()
            val cycles = prefs.pomodoroCyclesUntilLong.first()
            val savedDeadline = prefs.pomodoroDeadline.first()
            val savedPhase = prefs.pomodoroPhase.first()

            _state.update {
                it.copy(
                    totalSeconds = focus * 60,
                    remainingSeconds = focus * 60,
                    focusMin = focus,
                    shortBreakMin = short,
                    longBreakMin = long,
                    cyclesUntilLong = cycles
                )
            }

            if (savedDeadline > 0 && savedPhase != null) {
                val phase = runCatching { PomoPhase.valueOf(savedPhase) }.getOrNull()
                if (phase != null) {
                    val remaining = ((savedDeadline - System.currentTimeMillis() + 999) / 1000L).toInt()
                    if (remaining > 0) {
                        _state.update { it.copy(
                            phase = phase,
                            remainingSeconds = remaining,
                            isRunning = true
                        ) }
                        start(savedDeadline)
                    } else {
                        // Timer expired while app was dead; reset to default
                        prefs.setPomodoroDeadline(0)
                        prefs.setPomodoroPhase(null)
                    }
                }
            }
        }
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    private fun start(existingDeadline: Long = -1L) {
        if (tickJob?.isActive == true) return
        _state.update { it.copy(isRunning = true) }

        val deadline = if (existingDeadline != -1L) {
            existingDeadline
        } else {
            System.currentTimeMillis() + _state.value.remainingSeconds * 1000L
        }

        viewModelScope.launch {
            prefs.setPomodoroDeadline(deadline)
            prefs.setPomodoroPhase(_state.value.phase.name)

            tickJob = viewModelScope.launch {
                while (true) {
                    delay(250)
                    if (!_state.value.isRunning) break
                    val remaining = ((deadline - System.currentTimeMillis() + 999) / 1000L).toInt()
                    if (remaining <= 0) {
                        _state.update { it.copy(remainingSeconds = 0) }
                        advancePhase()
                        break
                    }
                    if (remaining != _state.value.remainingSeconds) {
                        _state.update { it.copy(remainingSeconds = remaining) }
                    }
                }
            }
        }
    }

    private fun pause() {
        _state.update { it.copy(isRunning = false) }
        tickJob?.cancel()
        viewModelScope.launch {
            prefs.setPomodoroDeadline(0)
            prefs.setPomodoroPhase(null)
        }
    }

    fun reset() {
        pause()
        _state.update {
            val total = phaseDurationSec(it.phase, it)
            it.copy(remainingSeconds = total, totalSeconds = total)
        }
    }

    fun skip() {
        advancePhase()
    }

    private fun advancePhase() {
        // Record the focused minutes that just elapsed before we roll into the next
        // phase. Covers both natural completion and a mid-focus Skip - only whole
        // minutes of >= 1 are persisted so the StudyHub "This week" chart reflects
        // real focus time. Breaks are never recorded.
        val prev = _state.value
        if (prev.phase == PomoPhase.Focus) {
            val elapsedMin = ((prev.totalSeconds - prev.remainingSeconds) / 60.0).roundToInt()
            if (elapsedMin >= 1) recordFocusSession(elapsedMin)
        }
        _state.update {
            val nextPhase: PomoPhase
            val nextCompleted: Int
            when (it.phase) {
                PomoPhase.Focus -> {
                    val nc = it.completedFocusCycles + 1
                    nextPhase = if (nc % it.cyclesUntilLong == 0) PomoPhase.LongBreak else PomoPhase.ShortBreak
                    nextCompleted = nc
                }
                PomoPhase.ShortBreak, PomoPhase.LongBreak -> {
                    nextPhase = PomoPhase.Focus
                    nextCompleted = it.completedFocusCycles
                }
            }
            val total = phaseDurationSec(nextPhase, it)
            it.copy(
                phase = nextPhase,
                completedFocusCycles = nextCompleted,
                totalSeconds = total,
                remainingSeconds = total,
                isRunning = false
            )
        }
        tickJob?.cancel()
    }

    private fun phaseDurationSec(phase: PomoPhase, s: PomodoroUiState): Int = when (phase) {
        PomoPhase.Focus -> s.focusMin * 60
        PomoPhase.ShortBreak -> s.shortBreakMin * 60
        PomoPhase.LongBreak -> s.longBreakMin * 60
    }

    /** Persist a completed focus block so study-time charts/totals stay truthful. */
    private fun recordFocusSession(minutes: Int) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            studyRepo.upsert(
                StudySession(
                    courseId = null,
                    scheduledStartEpochMs = now - minutes * 60_000L,
                    scheduledEndEpochMs = now,
                    actualMinutesStudied = minutes,
                    completedAtEpochMs = now
                )
            )
        }
    }
}
