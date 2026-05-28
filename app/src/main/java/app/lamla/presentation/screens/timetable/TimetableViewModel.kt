package app.lamla.presentation.screens.timetable

import androidx.lifecycle.ViewModel
import app.lamla.data.repo.ClassSessionRepository
import app.lamla.data.repo.CourseRepository
import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class TimetableUiState(
    val sessionsByDay: Map<java.time.DayOfWeek, List<ClassSession>> = emptyMap(),
    val coursesById: Map<Long, Course> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    classRepo: ClassSessionRepository,
    courseRepo: CourseRepository
) : ViewModel() {
    val state: StateFlow<TimetableUiState> = combine(
        classRepo.observeAll(),
        courseRepo.observeAll()
    ) { sessions, courses ->
        TimetableUiState(
            sessionsByDay = sessions.groupBy { it.dayOfWeek }.mapValues { (_, list) -> list.sortedBy { it.startMinutes } },
            coursesById = courses.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TimetableUiState())
}
