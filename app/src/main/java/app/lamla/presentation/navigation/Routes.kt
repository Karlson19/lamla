package app.lamla.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes (Navigation Compose 2.8+). No string concat, no manual URL escaping.
 */
sealed interface Route {

    @Serializable data object Onboarding : Route

    @Serializable data object HomeGraph : Route
    @Serializable data object Home : Route
    @Serializable data object Timetable : Route
    @Serializable data object Courses : Route
    @Serializable data object Study : Route
    @Serializable data object Settings : Route

    @Serializable data class ClassEdit(val classId: Long? = null) : Route
    @Serializable data class CourseDetail(val courseId: Long) : Route
    @Serializable data class CourseEdit(val courseId: Long? = null) : Route
    @Serializable data class DeadlineEdit(val deadlineId: Long? = null) : Route
    @Serializable data class LecturerDetail(val lecturerId: Long) : Route
    @Serializable data class LecturerEdit(val lecturerId: Long? = null) : Route
    @Serializable data object Deadlines : Route
    @Serializable data object Lecturers : Route
    @Serializable data object Grades : Route
    @Serializable data class PersonalEventEdit(val eventId: Long? = null) : Route
    @Serializable data object Pomodoro : Route
    @Serializable data class StudySessionEdit(val sessionId: Long? = null) : Route
    @Serializable data object ExamMode : Route
    @Serializable data class ExamEdit(val examId: Long? = null) : Route
    @Serializable data class CaptureGallery(val courseId: Long? = null) : Route

    @Serializable data object NotificationSettings : Route
    @Serializable data object BatteryGuide : Route
    @Serializable data object DataExportImport : Route
    @Serializable data object Diagnostics : Route
}
