package app.lamla.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.SemesterRepository
import app.lamla.domain.model.Semester
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val semesterRepo: SemesterRepository
) : ViewModel() {

    /**
     * Persist all onboarding answers in one atomic-ish step.
     * `userName` may be blank — that's allowed, name is optional.
     */
    suspend fun complete(
        userName: String,
        semesterName: String,
        start: LocalDate,
        end: LocalDate
    ) {
        val zone = ZoneId.systemDefault()
        val id = semesterRepo.upsert(
            Semester(
                name = semesterName.trim(),
                startDateEpochMs = start.atStartOfDay(zone).toInstant().toEpochMilli(),
                endDateEpochMs = end.atStartOfDay(zone).toInstant().toEpochMilli(),
                isActive = true
            )
        )
        semesterRepo.setActive(id)
        prefs.setUserName(userName)
        prefs.setOnboarded(true)
    }
}
