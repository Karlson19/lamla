package app.lamla.presentation.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CaptureRepository
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.DeadlineRepository
import app.lamla.data.repo.LecturerRepository
import app.lamla.data.repo.StudySessionRepository
import app.lamla.domain.model.Course
import app.lamla.domain.model.Deadline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiState(
    val course: Course? = null,
    val lecturerName: String? = null,
    val deadlines: List<Deadline> = emptyList(),
    val minutesThisWeek: Int = 0,
    val captureCount: Int = 0
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    private val lecturerRepo: LecturerRepository,
    private val deadlineRepo: DeadlineRepository,
    private val studyRepo: StudySessionRepository,
    private val captureRepo: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CourseDetailUiState())
    val state: StateFlow<CourseDetailUiState> = _state.asStateFlow()

    fun load(courseId: Long) {
        viewModelScope.launch {
            val course = courseRepo.get(courseId) ?: return@launch
            val lecturer = course.lecturerId?.let { lecturerRepo.get(it) }
            val deadlines = deadlineRepo.observeForCourse(courseId).first()
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60_000
            val mins = studyRepo.observeMinutesPerCourse(weekAgo, System.currentTimeMillis()).first()[courseId] ?: 0
            val captures = captureRepo.observeForCourse(courseId).first().size
            _state.update {
                it.copy(
                    course = course,
                    lecturerName = lecturer?.name,
                    deadlines = deadlines,
                    minutesThisWeek = mins,
                    captureCount = captures
                )
            }
        }
    }
}
