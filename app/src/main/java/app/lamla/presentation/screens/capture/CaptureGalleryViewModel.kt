package app.lamla.presentation.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.data.repo.CaptureRepository
import app.lamla.domain.model.Capture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureGalleryUiState(val captures: List<Capture> = emptyList())

@HiltViewModel
class CaptureGalleryViewModel @Inject constructor(
    private val captureRepo: CaptureRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureGalleryUiState())
    val state: StateFlow<CaptureGalleryUiState> = _state.asStateFlow()

    fun load(courseId: Long?) {
        viewModelScope.launch {
            val source = if (courseId == null) captureRepo.observeAll() else captureRepo.observeForCourse(courseId)
            source.collect { list -> _state.update { it.copy(captures = list) } }
        }
    }
}
