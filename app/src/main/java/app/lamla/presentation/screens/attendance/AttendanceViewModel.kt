package app.lamla.presentation.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lamla.attendance.GeofenceManager
import app.lamla.attendance.LocationProvider
import app.lamla.data.prefs.AppPreferences
import app.lamla.data.repo.AttendanceRepository
import app.lamla.data.repo.ClassSessionRepository
import app.lamla.data.repo.CourseRepository
import app.lamla.data.repo.SemesterRepository
import app.lamla.data.repo.VenueLocationRepository
import app.lamla.domain.model.AttendanceRecord
import app.lamla.domain.model.AttendanceStatus
import app.lamla.domain.model.ClassSession
import app.lamla.domain.model.Course
import app.lamla.domain.model.Semester
import app.lamla.domain.model.VenueLocation
import app.lamla.domain.usecase.AttendanceStats
import app.lamla.domain.usecase.AttendanceStats.CourseAttendance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One of today's class meetings, with whatever verdict it already carries. */
data class TodayClass(
    val session: ClassSession,
    val course: Course,
    val status: AttendanceStatus?
)

/** A distinct venue drawn from the timetable, and whether it has a GPS pin. */
data class VenueRow(
    val key: String,
    val displayName: String,
    val pinned: Boolean,
    val courseCodes: List<String>
)

data class AttendanceUiState(
    val loading: Boolean = true,
    val semesterName: String? = null,
    val target: Float = AttendanceStats.DEFAULT_TARGET,
    val autoEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasBackgroundPermission: Boolean = false,
    val courses: List<CourseAttendance> = emptyList(),
    val today: List<TodayClass> = emptyList(),
    val venues: List<VenueRow> = emptyList()
) {
    /** Credit-blind overall rate across every marked meeting this semester. */
    val overallRate: Float?
        get() {
            val attended = courses.sumOf { it.attended }
            val marked = courses.sumOf { it.marked }
            return if (marked > 0) attended.toFloat() / marked else null
        }
    val pinnedVenueCount: Int get() = venues.count { it.pinned }
    val hasVenues: Boolean get() = venues.isNotEmpty()
}

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    private val classSessionRepo: ClassSessionRepository,
    private val attendanceRepo: AttendanceRepository,
    private val semesterRepo: SemesterRepository,
    private val venueRepo: VenueLocationRepository,
    private val prefs: AppPreferences,
    private val geofenceManager: GeofenceManager,
    private val locationProvider: LocationProvider
) : ViewModel() {

    // Bumped to force a re-read of the (non-reactive) runtime permission state.
    private val permissionTick = MutableStateFlow(0)

    private val _pinningVenue = MutableStateFlow<String?>(null)
    val pinningVenue: StateFlow<String?> = _pinningVenue.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private data class Base(
        val semester: Semester?,
        val courses: List<Course>,
        val sessions: List<ClassSession>,
        val records: List<AttendanceRecord>,
        val venues: List<VenueLocation>
    )

    private val base = combine(
        semesterRepo.observeActive(),
        courseRepo.observeAll(),
        classSessionRepo.observeAll(),
        attendanceRepo.observeAll(),
        venueRepo.observeAll()
    ) { semester, courses, sessions, records, venues ->
        Base(semester, courses, sessions, records, venues)
    }

    val state: StateFlow<AttendanceUiState> = combine(
        base,
        prefs.attendanceTarget,
        prefs.attendanceAutoEnabled,
        permissionTick
    ) { b, target, auto, _ ->
        val sem = b.semester
        val courses = if (sem != null) b.courses.filter { it.semesterId == sem.id } else b.courses
        val courseIds = courses.map { it.id }.toSet()
        val sessions = b.sessions.filter { it.courseId in courseIds }
        val records = b.records.filter { it.courseId in courseIds }

        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val zone = ZoneId.systemDefault()
        fun msToEpochDay(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
        val semStartDay = sem?.let { msToEpochDay(it.startDateEpochMs) }
        val semEndDay = sem?.let { msToEpochDay(it.endDateEpochMs) }

        val courseStats = AttendanceStats.forCourses(
            courses = courses,
            sessionsByCourse = sessions.groupBy { it.courseId },
            records = records,
            semesterStartEpochDay = semStartDay,
            semesterEndEpochDay = semEndDay,
            todayEpochDay = todayEpochDay
        ).sortedBy { it.course.code }

        val courseById = courses.associateBy { it.id }
        val recordByOccurrence = records.associateBy { it.classSessionId to it.dateEpochDay }
        val dow = today.dayOfWeek
        val todayClasses = sessions
            .filter { it.dayOfWeek == dow }
            .sortedBy { it.startMinutes }
            .mapNotNull { s ->
                val course = courseById[s.courseId] ?: return@mapNotNull null
                TodayClass(s, course, recordByOccurrence[s.id to todayEpochDay]?.status)
            }

        val pinnedKeys = b.venues.map { it.venueKey }.toSet()
        val venueRows = sessions
            .filter { it.venue.isNotBlank() }
            .groupBy { VenueLocationRepository.keyOf(it.venue) }
            .map { (key, list) ->
                VenueRow(
                    key = key,
                    displayName = list.first().venue.trim(),
                    pinned = key in pinnedKeys,
                    courseCodes = list.mapNotNull { courseById[it.courseId]?.code }.distinct().sorted()
                )
            }
            .sortedBy { it.displayName.lowercase() }

        AttendanceUiState(
            loading = false,
            semesterName = sem?.name,
            target = target,
            autoEnabled = auto,
            hasLocationPermission = geofenceManager.hasForegroundLocation(),
            hasBackgroundPermission = geofenceManager.hasBackgroundLocation(),
            courses = courseStats,
            today = todayClasses,
            venues = venueRows
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AttendanceUiState())

    fun refreshPermissions() = permissionTick.update { it + 1 }

    fun markToday(session: ClassSession, status: AttendanceStatus) {
        viewModelScope.launch {
            val epochDay = LocalDate.now().toEpochDay()
            attendanceRepo.upsert(
                AttendanceRecord(
                    id = attendanceRepo.forOccurrence(session.id, epochDay)?.id ?: 0,
                    classSessionId = session.id,
                    courseId = session.courseId,
                    dateEpochDay = epochDay,
                    status = status,
                    markedAtEpochMs = System.currentTimeMillis(),
                    auto = false
                )
            )
        }
    }

    fun clearToday(session: ClassSession) {
        viewModelScope.launch { attendanceRepo.clearOccurrence(session.id, LocalDate.now().toEpochDay()) }
    }

    fun setTarget(value: Float) {
        viewModelScope.launch { prefs.setAttendanceTarget(value) }
    }

    fun setAutoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAttendanceAutoEnabled(enabled)
            if (enabled) geofenceManager.refresh() else geofenceManager.clear()
        }
    }

    /** Capture the current GPS fix and anchor it to this venue, then (re)arm geofences. */
    fun pinVenue(row: VenueRow) {
        viewModelScope.launch {
            if (!locationProvider.hasForegroundPermission()) {
                _message.value = "Grant location permission to pin a venue."
                return@launch
            }
            _pinningVenue.value = row.key
            val loc = locationProvider.current()
            _pinningVenue.value = null
            if (loc == null) {
                _message.value = "Couldn't get a location fix. Try again with GPS on."
                return@launch
            }
            venueRepo.upsert(
                VenueLocation(
                    id = venueRepo.getByKey(row.key)?.id ?: 0,
                    venueKey = row.key,
                    displayName = row.displayName,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    radiusMeters = 130f
                )
            )
            geofenceManager.refresh()
            _message.value = "Pinned ${row.displayName} — you'll be auto-marked here."
        }
    }

    fun unpinVenue(row: VenueRow) {
        viewModelScope.launch {
            venueRepo.deleteByKey(row.key)
            geofenceManager.refresh()
            _message.value = "Unpinned ${row.displayName}."
        }
    }

    fun consumeMessage() { _message.value = null }

    /** Called after a permission result so geofences re-arm once access is granted. */
    fun onPermissionsChanged() {
        refreshPermissions()
        viewModelScope.launch {
            if (venueRepo.all().isNotEmpty()) geofenceManager.refresh()
        }
    }
}
