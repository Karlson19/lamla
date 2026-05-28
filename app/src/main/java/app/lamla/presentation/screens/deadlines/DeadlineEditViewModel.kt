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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class DeadlineEditUiState(
    val deadlineId: Long? = null,
    val title: String = "",
    val description: String = "",
    val selectedCourse: Course? = null,
    val allCourses: List<Course> = emptyList(),
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val weightPercent: Float = 0f
) {
    val canSave: Boolean get() = title.isNotBlank() && selectedCourse != null && dueDate != null && dueTime != null
}

@HiltViewModel
class DeadlineEditViewModel @Inject constructor(
    private val deadlineRepo: DeadlineRepository,
    private val courseRepo: CourseRepository,
    private val engine: ReminderEngine
) : ViewModel() {

    private val _state = MutableStateFlow(DeadlineEditUiState())
    val state: StateFlow<DeadlineEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            val courses = courseRepo.observeAll().first()
            val existing = id?.let { deadlineRepo.get(it) }
            _state.update {
                if (existing == null) it.copy(allCourses = courses, selectedCourse = courses.firstOrNull())
                else {
                    val zone = ZoneId.systemDefault()
                    val ldt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(existing.dueAtEpochMs), zone)
                    it.copy(
                        deadlineId = existing.id,
                        title = existing.title,
                        description = existing.description,
                        selectedCourse = courses.firstOrNull { c -> c.id == existing.courseId },
                        allCourses = courses,
                        dueDate = ldt.toLocalDate(),
                        dueTime = ldt.toLocalTime(),
                        weightPercent = existing.weightPercent
                    )
                }
            }
        }
    }

    fun setTitle(v: String) { _state.update { it.copy(title = v) } }
    fun setDescription(v: String) { _state.update { it.copy(description = v) } }
    fun selectCourse(c: Course) { _state.update { it.copy(selectedCourse = c) } }
    fun setDate(d: LocalDate) { _state.update { it.copy(dueDate = d) } }
    fun setTime(t: LocalTime) { _state.update { it.copy(dueTime = t) } }
    fun setWeight(v: Float) { _state.update { it.copy(weightPercent = v.coerceIn(0f, 100f)) } }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.canSave) return false
        val zone = ZoneId.systemDefault()
        val dueAt = LocalDateTime.of(s.dueDate, s.dueTime).atZone(zone).toInstant().toEpochMilli()
        // Cancel old alarms first
        s.deadlineId?.let { id -> deadlineRepo.get(id)?.let { engine.cancelForDeadline(it) } }
        val newId = deadlineRepo.upsert(
            Deadline(
                id = s.deadlineId ?: 0L,
                courseId = s.selectedCourse!!.id,
                title = s.title.trim(),
                description = s.description.trim(),
                dueAtEpochMs = dueAt,
                weightPercent = s.weightPercent,
                status = DeadlineStatus.Pending
            )
        )
        deadlineRepo.get(newId)?.let { engine.scheduleForDeadline(it) }
        return true
    }

    suspend fun delete() {
        val id = _state.value.deadlineId ?: return
        val existing = deadlineRepo.get(id) ?: return
        engine.cancelForDeadline(existing)
        deadlineRepo.delete(existing)
    }
}
