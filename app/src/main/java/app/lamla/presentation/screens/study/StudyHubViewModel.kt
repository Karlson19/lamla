package app.lamla.presentation.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.ExamRepository
import app.lamla.data.repo.StudySessionRepository
import app.lamla.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StudyHubUiState(
    val perCourse: List<Pair<Course?, Int>> = emptyList(),
    val totalMinutesThisWeek: Int = 0,
    val upcomingExamCount: Int = 0
)

@HiltViewModel
class StudyHubViewModel @Inject constructor(
    courseRepo: CourseRepository,
    studyRepo: StudySessionRepository,
    examRepo: ExamRepository
) : ViewModel() {

    val state: StateFlow<StudyHubUiState> = run {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * 24 * 60 * 60_000
        combine(
            courseRepo.observeAll(),
            studyRepo.observeMinutesPerCourse(weekAgo, now),
            examRepo.observeUpcoming(now)
        ) { courses, mins, exams ->
            val byId = courses.associateBy { it.id }
            val rows = mins.entries
                .sortedByDescending { it.value }
                .map { byId[it.key] to it.value }
            StudyHubUiState(
                perCourse = rows,
                totalMinutesThisWeek = mins.values.sum(),
                upcomingExamCount = exams.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), StudyHubUiState())
    }
}
