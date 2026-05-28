package app.lamla.presentation.screens.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.ExamRepository
import app.lamla.domain.model.Course
import app.lamla.domain.model.Exam
import app.lamla.notifications.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ExamModeUiState(
    val examModeOn: Boolean = false,
    val exams: List<Exam> = emptyList(),
    val courses: Map<Long, Course> = emptyMap()
)

@HiltViewModel
class ExamModeViewModel @Inject constructor(
    private val prefs: AppPreferences,
    examRepo: ExamRepository,
    courseRepo: CourseRepository
) : ViewModel() {
    val state: StateFlow<ExamModeUiState> = combine(
        prefs.examMode,
        examRepo.observeUpcoming(),
        courseRepo.observeAll()
    ) { mode, exams, courses ->
        ExamModeUiState(examModeOn = mode, exams = exams, courses = courses.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ExamModeUiState())

    fun toggleExamMode(on: Boolean) {
        viewModelScope.launch { prefs.setExamMode(on) }
    }
}

data class ExamEditUiState(
    val examId: Long? = null,
    val selectedCourse: Course? = null,
    val allCourses: List<Course> = emptyList(),
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val venue: String = "",
    val topics: List<String> = emptyList()
) {
    val canSave: Boolean get() = selectedCourse != null && date != null && time != null
}

@HiltViewModel
class ExamEditViewModel @Inject constructor(
    private val examRepo: ExamRepository,
    private val courseRepo: CourseRepository,
    private val engine: ReminderEngine
) : ViewModel() {
    private val _state = MutableStateFlow(ExamEditUiState())
    val state: StateFlow<ExamEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            val courses = courseRepo.observeAll().first()
            val existing = id?.let { examRepo.get(it) }
            _state.update {
                if (existing == null) it.copy(allCourses = courses, selectedCourse = courses.firstOrNull())
                else {
                    val zone = ZoneId.systemDefault()
                    val ldt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(existing.examDateEpochMs), zone)
                    it.copy(
                        examId = existing.id,
                        selectedCourse = courses.firstOrNull { c -> c.id == existing.courseId },
                        allCourses = courses,
                        date = ldt.toLocalDate(),
                        time = ldt.toLocalTime(),
                        venue = existing.venue,
                        topics = existing.topics
                    )
                }
            }
        }
    }

    fun selectCourse(c: Course) { _state.update { it.copy(selectedCourse = c) } }
    fun setDate(d: LocalDate) { _state.update { it.copy(date = d) } }
    fun setTime(t: LocalTime) { _state.update { it.copy(time = t) } }
    fun setVenue(v: String) { _state.update { it.copy(venue = v) } }
    fun addTopic(t: String) { _state.update { it.copy(topics = it.topics + t) } }
    fun removeTopic(t: String) { _state.update { it.copy(topics = it.topics - t) } }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.canSave) return false
        val zone = ZoneId.systemDefault()
        val ms = LocalDateTime.of(s.date, s.time).atZone(zone).toInstant().toEpochMilli()
        s.examId?.let { id -> examRepo.get(id)?.let { engine.cancelForExam(it) } }
        val newId = examRepo.upsert(
            Exam(
                id = s.examId ?: 0L,
                courseId = s.selectedCourse!!.id,
                examDateEpochMs = ms,
                venue = s.venue.trim(),
                topics = s.topics
            )
        )
        examRepo.get(newId)?.let { engine.scheduleForExam(it) }
        return true
    }

    suspend fun delete() {
        val id = _state.value.examId ?: return
        examRepo.get(id)?.let {
            engine.cancelForExam(it)
            examRepo.delete(it)
        }
    }
}
