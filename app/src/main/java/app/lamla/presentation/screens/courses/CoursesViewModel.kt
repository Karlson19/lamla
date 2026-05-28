package app.lamla.presentation.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.LecturerRepository
import app.lamla.data.repo.SemesterRepository
import app.lamla.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CoursesUiState(
    val courses: List<Course> = emptyList(),
    val lecturerNameById: Map<Long, String> = emptyMap(),
    val semesterName: String? = null
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    courseRepo: CourseRepository,
    lecturerRepo: LecturerRepository,
    semesterRepo: SemesterRepository
) : ViewModel() {
    val state: StateFlow<CoursesUiState> = combine(
        courseRepo.observeAll(),
        lecturerRepo.observeAll(),
        semesterRepo.observeActive()
    ) { courses, lecturers, sem ->
        CoursesUiState(
            courses = courses,
            lecturerNameById = lecturers.associate { it.id to it.name },
            semesterName = sem?.name
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), CoursesUiState())
}
