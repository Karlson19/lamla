package app.lamla.presentation.screens.deadlines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.DeadlineRepository
import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import app.lamla.domain.model.DeadlineStatus
import app.lamla.notifications.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.exp
import javax.inject.Inject

data class DeadlinesUiState(
    val deadlines: List<Deadline> = emptyList(),
    val coursesById: Map<Long, Course> = emptyMap()
)

@HiltViewModel
class DeadlinesViewModel @Inject constructor(
    private val deadlineRepo: DeadlineRepository,
    private val courseRepo: CourseRepository,
    private val engine: ReminderEngine
) : ViewModel() {

    val state: StateFlow<DeadlinesUiState> = combine(
        deadlineRepo.observeAll(),
        courseRepo.observeAll()
    ) { ds, courses ->
        // Sort: pending first, then by urgency × weight (heavier and closer = higher).
        val now = System.currentTimeMillis()
        val sorted = ds.sortedWith(
            compareBy<Deadline> { it.status == DeadlineStatus.Done }
                .thenByDescending { d ->
                    if (d.status == DeadlineStatus.Done) 0.0
                    else {
                        val hoursLeft = (d.dueAtEpochMs - now) / 3_600_000.0
                        d.weightPercent * exp(-hoursLeft / 48.0)
                    }
                }
        )
        DeadlinesUiState(deadlines = sorted, coursesById = courses.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DeadlinesUiState())

    fun toggleDone(d: Deadline) {
        viewModelScope.launch {
            val next = if (d.status == DeadlineStatus.Done) DeadlineStatus.Pending else DeadlineStatus.Done
            deadlineRepo.setStatus(d.id, next)
            val updated = d.copy(status = next)
            if (next == DeadlineStatus.Done) engine.cancelForDeadline(updated)
            else engine.scheduleForDeadline(updated)
        }
    }
}
