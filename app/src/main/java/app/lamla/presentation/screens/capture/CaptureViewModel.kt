package app.lamla.presentation.screens.capture

import androidx.lifecycle.ViewModel
import app.lamla.data.repo.CaptureRepository
import app.lamla.domain.model.Capture
import app.lamla.domain.model.CaptureType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepo: CaptureRepository
) : ViewModel() {

    suspend fun saveText(courseId: Long?, text: String) {
        captureRepo.upsert(
            Capture(
                courseId = courseId,
                type = CaptureType.Text,
                filePath = "",
                createdAtEpochMs = System.currentTimeMillis(),
                note = text
            )
        )
    }

    suspend fun savePhoto(courseId: Long?, path: String) {
        captureRepo.upsert(
            Capture(
                courseId = courseId,
                type = CaptureType.Photo,
                filePath = path,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveVoice(courseId: Long?, path: String, note: String = "") {
        captureRepo.upsert(
            Capture(
                courseId = courseId,
                type = CaptureType.Voice,
                filePath = path,
                createdAtEpochMs = System.currentTimeMillis(),
                note = note
            )
        )
    }
}
