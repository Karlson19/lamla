package app.lamla.presentation.screens.lecturers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.LecturerRepository
import app.lamla.data.repo.QuestionRepository
import app.lamla.domain.model.Lecturer
import app.lamla.domain.model.OfficeHourSlot
import app.lamla.domain.model.Question
import app.lamla.notifications.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

// --- List ---
data class LecturersUiState(val lecturers: List<Lecturer> = emptyList())

@HiltViewModel
class LecturersViewModel @Inject constructor(
    lecturerRepo: LecturerRepository
) : ViewModel() {
    val state: StateFlow<LecturersUiState> = lecturerRepo.observeAll()
        .map { LecturersUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LecturersUiState())
}

// --- Detail ---
data class LecturerDetailUiState(
    val lecturer: Lecturer? = null,
    val questions: List<Question> = emptyList()
)

@HiltViewModel
class LecturerDetailViewModel @Inject constructor(
    private val lecturerRepo: LecturerRepository,
    private val questionRepo: QuestionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LecturerDetailUiState())
    val state: StateFlow<LecturerDetailUiState> = _state.asStateFlow()
    private var lecturerId: Long = 0

    fun load(id: Long) {
        lecturerId = id
        viewModelScope.launch {
            lecturerRepo.observe(id).collect { lec ->
                _state.update { it.copy(lecturer = lec) }
            }
        }
        viewModelScope.launch {
            questionRepo.observeForLecturer(id).collect { qs ->
                _state.update { it.copy(questions = qs) }
            }
        }
    }

    suspend fun addQuestion(text: String) {
        questionRepo.upsert(
            Question(lecturerId = lecturerId, courseId = null, text = text, createdAtEpochMs = System.currentTimeMillis())
        )
    }

    suspend fun toggleQuestion(q: Question) {
        if (q.isAnswered) {
            questionRepo.upsert(q.copy(answeredAtEpochMs = null))
        } else {
            questionRepo.markAnswered(q.id, System.currentTimeMillis())
        }
    }
}

// --- Edit ---
data class LecturerEditUiState(
    val lecturerId: Long? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val officeLocation: String = "",
    val officeHours: List<OfficeHourSlot> = emptyList(),
    val notes: String = ""
) {
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class LecturerEditViewModel @Inject constructor(
    private val lecturerRepo: LecturerRepository,
    private val engine: ReminderEngine
) : ViewModel() {
    private val _state = MutableStateFlow(LecturerEditUiState())
    val state: StateFlow<LecturerEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            val existing = id?.let { lecturerRepo.get(it) }
            if (existing != null) {
                _state.update {
                    it.copy(
                        lecturerId = existing.id,
                        name = existing.name,
                        email = existing.email,
                        phone = existing.phone,
                        officeLocation = existing.officeLocation,
                        officeHours = existing.officeHours,
                        notes = existing.notes
                    )
                }
            }
        }
    }

    fun setName(v: String) { _state.update { it.copy(name = v) } }
    fun setEmail(v: String) { _state.update { it.copy(email = v) } }
    fun setPhone(v: String) { _state.update { it.copy(phone = v) } }
    fun setOfficeLocation(v: String) { _state.update { it.copy(officeLocation = v) } }
    fun setNotes(v: String) { _state.update { it.copy(notes = v) } }

    fun addOfficeHourSlot() {
        _state.update { it.copy(officeHours = it.officeHours + OfficeHourSlot(DayOfWeek.MONDAY, 14 * 60, 16 * 60)) }
    }
    fun removeOfficeHour(index: Int) {
        _state.update {
            val list = it.officeHours.toMutableList().also { l -> if (index in l.indices) l.removeAt(index) }
            it.copy(officeHours = list)
        }
    }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.canSave) return false
        val lecturer = Lecturer(
            id = s.lecturerId ?: 0L,
            name = s.name.trim(),
            email = s.email.trim(),
            phone = s.phone.trim(),
            officeLocation = s.officeLocation.trim(),
            officeHours = s.officeHours,
            notes = s.notes.trim()
        )
        val newId = lecturerRepo.upsert(lecturer)
        // Re-schedule office-hour reminders
        engine.scheduleForLecturerOfficeHours(lecturer.copy(id = newId))
        return true
    }

    suspend fun delete() {
        val id = _state.value.lecturerId ?: return
        lecturerRepo.get(id)?.let {
            engine.cancelForLecturerOfficeHours(it)
            lecturerRepo.delete(it)
        }
    }
}
