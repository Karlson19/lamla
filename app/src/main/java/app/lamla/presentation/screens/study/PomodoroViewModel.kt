package app.lamla.presentation.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val prefs: AppPreferences
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
        }
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    private fun start() {
        if (tickJob?.isActive == true) return
        _state.update { it.copy(isRunning = true) }
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (!s.isRunning) break
                if (s.remainingSeconds <= 1) {
                    advancePhase()
                } else {
                    _state.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                }
            }
        }
    }

    private fun pause() {
        _state.update { it.copy(isRunning = false) }
        tickJob?.cancel()
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
}
