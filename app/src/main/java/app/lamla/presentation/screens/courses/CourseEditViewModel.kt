package app.lamla.presentation.screens.courses

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.LecturerRepository
import app.lamla.data.repo.SemesterRepository
import app.lamla.domain.model.Course
import app.lamla.domain.model.Lecturer
import app.lamla.ui.theme.Palette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseEditUiState(
    val courseId: Long? = null,
    val code: String = "",
    val name: String = "",
    val creditHours: Int = 3,
    val selectedLecturer: Lecturer? = null,
    val allLecturers: List<Lecturer> = emptyList(),
    val colorArgb: Int = Palette.CoursePalette[0].toArgb(),
    val semesterId: Long = 0
) {
    val canSave: Boolean get() = code.isNotBlank() && name.isNotBlank() && semesterId != 0L
}

@HiltViewModel
class CourseEditViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    private val lecturerRepo: LecturerRepository,
    private val semesterRepo: SemesterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CourseEditUiState())
    val state: StateFlow<CourseEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            val lecturers = lecturerRepo.observeAll().first()
            val sem = semesterRepo.active() ?: return@launch
            val existing = id?.let { courseRepo.get(it) }
            _state.update {
                if (existing == null) {
                    it.copy(allLecturers = lecturers, semesterId = sem.id)
                } else {
                    it.copy(
                        courseId = existing.id,
                        code = existing.code,
                        name = existing.name,
                        creditHours = existing.creditHours,
                        selectedLecturer = lecturers.firstOrNull { l -> l.id == existing.lecturerId },
                        allLecturers = lecturers,
                        colorArgb = existing.colorArgb,
                        semesterId = existing.semesterId
                    )
                }
            }
        }
    }

    fun setCode(v: String) { _state.update { it.copy(code = v) } }
    fun setName(v: String) { _state.update { it.copy(name = v) } }
    fun setCreditHours(v: Int) { _state.update { it.copy(creditHours = v.coerceIn(0, 12)) } }
    fun setLecturer(l: Lecturer?) { _state.update { it.copy(selectedLecturer = l) } }
    fun setColor(argb: Int) { _state.update { it.copy(colorArgb = argb) } }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.canSave) return false
        courseRepo.upsert(
            Course(
                id = s.courseId ?: 0L,
                code = s.code.trim(),
                name = s.name.trim(),
                lecturerId = s.selectedLecturer?.id,
                colorArgb = s.colorArgb,
                creditHours = s.creditHours,
                semesterId = s.semesterId
            )
        )
        return true
    }

    suspend fun delete() {
        val id = _state.value.courseId ?: return
        courseRepo.get(id)?.let { courseRepo.delete(it) }
    }
}
