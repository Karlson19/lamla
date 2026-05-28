package app.lamla.presentation.screens.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.PersonalEventRepository
import app.lamla.domain.model.PersonalEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class PersonalEventEditUiState(
    val eventId: Long? = null,
    val title: String = "",
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val notes: String = ""
) {
    val canSave: Boolean
        get() = title.isNotBlank() &&
            date != null && startTime != null && endTime != null &&
            endTime.isAfter(startTime)
}

@HiltViewModel
class PersonalEventEditViewModel @Inject constructor(
    private val repo: PersonalEventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PersonalEventEditUiState())
    val state: StateFlow<PersonalEventEditUiState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch {
            if (id == null) return@launch
            val existing = repo.all().firstOrNull { it.id == id } ?: return@launch
            val zone = ZoneId.systemDefault()
            val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(existing.startEpochMs), zone)
            val endLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(existing.endEpochMs), zone)
            _state.update {
                it.copy(
                    eventId = existing.id,
                    title = existing.title,
                    date = startLdt.toLocalDate(),
                    startTime = startLdt.toLocalTime(),
                    endTime = endLdt.toLocalTime(),
                    notes = existing.notes
                )
            }
        }
    }

    fun setTitle(v: String) { _state.update { it.copy(title = v) } }
    fun setDate(d: LocalDate) { _state.update { it.copy(date = d) } }
    fun setStartTime(t: LocalTime) {
        _state.update {
            // If end is now before start, push it 1h after start.
            val newEnd = if (it.endTime == null || !it.endTime.isAfter(t)) t.plusHours(1) else it.endTime
            it.copy(startTime = t, endTime = newEnd)
        }
    }
    fun setEndTime(t: LocalTime) { _state.update { it.copy(endTime = t) } }
    fun setNotes(v: String) { _state.update { it.copy(notes = v) } }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.canSave) return false
        val zone = ZoneId.systemDefault()
        val startMs = LocalDateTime.of(s.date, s.startTime).atZone(zone).toInstant().toEpochMilli()
        val endMs = LocalDateTime.of(s.date, s.endTime).atZone(zone).toInstant().toEpochMilli()
        repo.upsert(
            PersonalEvent(
                id = s.eventId ?: 0L,
                title = s.title.trim(),
                startEpochMs = startMs,
                endEpochMs = endMs,
                recurrenceRule = null,
                notes = s.notes.trim()
            )
        )
        return true
    }

    suspend fun delete() {
        val id = _state.value.eventId ?: return
        val existing = repo.all().firstOrNull { it.id == id } ?: return
        repo.delete(existing)
    }
}
