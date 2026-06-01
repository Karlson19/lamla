package app.lamla.presentation.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.ClassSessionRepository
import app.lamla.data.repo.CourseRepository
import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import app.lamla.notifications.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class ClassEditUiState(
    val classId: Long? = null,
    val selectedCourse: Course? = null,
    val allCourses: List<Course> = emptyList(),
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startMinutes: Int = 8 * 60,
    val endMinutes: Int = 9 * 60,
    val venue: String = "",
    val reminderOffsetsMinutes: List<Int> = listOf(30, 10, 0)
) {
    val canSave: Boolean
        get() = selectedCourse != null && endMinutes > startMinutes
        // (venue blank is allowed → "Venue TBA")
}

@HiltViewModel
class ClassEditViewModel @Inject constructor(
    private val classRepo: ClassSessionRepository,
    private val courseRepo: CourseRepository,
    private val engine: ReminderEngine
) : ViewModel() {

    private val _state = MutableStateFlow(ClassEditUiState())
    val state: StateFlow<ClassEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            val courseList = courseRepo.observeAll().first()
            val existing = id?.let { classRepo.get(it) }
            _state.update {
                if (existing == null) {
                    it.copy(
                        classId = null,
                        allCourses = courseList,
                        selectedCourse = courseList.firstOrNull()
                    )
                } else {
                    it.copy(
                        classId = existing.id,
                        allCourses = courseList,
                        selectedCourse = courseList.firstOrNull { c -> c.id == existing.courseId },
                        dayOfWeek = existing.dayOfWeek,
                        startMinutes = existing.startMinutes,
                        endMinutes = existing.endMinutes,
                        venue = existing.venue,
                        reminderOffsetsMinutes = existing.reminderOffsetsMinutes
                    )
                }
            }
        }
    }

    fun selectCourse(c: Course) { _state.update { it.copy(selectedCourse = c) } }
    fun setDay(d: DayOfWeek) { _state.update { it.copy(dayOfWeek = d) } }
    fun setStartMinutes(m: Int) { _state.update { it.copy(startMinutes = m, endMinutes = maxOf(it.endMinutes, m + 30)) } }
    fun setEndMinutes(m: Int) { _state.update { it.copy(endMinutes = m) } }
    fun setVenue(v: String) { _state.update { it.copy(venue = v) } }
    fun toggleReminderOffset(o: Int) {
        _state.update {
            val list = it.reminderOffsetsMinutes.toMutableList()
            if (o in list) list.remove(o) else list.add(o)
            it.copy(reminderOffsetsMinutes = list.sortedDescending())
        }
    }

    /** Returns true on success. */
    suspend fun save(): Boolean {
        val s = _state.value
        val course = s.selectedCourse ?: return false
        val session = ClassSession(
            id = s.classId ?: 0L,
            courseId = course.id,
            dayOfWeek = s.dayOfWeek,
            startMinutes = s.startMinutes,
            endMinutes = s.endMinutes,
            venue = s.venue.trim(),
            reminderOffsetsMinutes = s.reminderOffsetsMinutes.ifEmpty { listOf(0) }
        )
        // Cancel old alarms (covers offset removal)
        s.classId?.let { id ->
            classRepo.get(id)?.let { engine.cancelForClassSession(it) }
        }
        val newId = classRepo.upsert(session)
        engine.scheduleForClassSession(session.copy(id = newId))
        return true
    }

    suspend fun delete() {
        val id = _state.value.classId ?: return
        val existing = classRepo.get(id) ?: return
        engine.cancelForClassSession(existing)
        classRepo.delete(existing)
    }
}
