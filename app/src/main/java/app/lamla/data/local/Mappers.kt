package app.lamla.data.local

import app.lamla.data.local.entities.*
import app.lamla.domain.model.*

// Course
internal fun CourseEntity.toDomain() = Course(
    id = id, code = code, name = name, lecturerId = lecturerId,
    colorArgb = colorArgb, creditHours = creditHours, semesterId = semesterId
)
internal fun Course.toEntity() = CourseEntity(
    id = id, code = code, name = name, lecturerId = lecturerId,
    colorArgb = colorArgb, creditHours = creditHours, semesterId = semesterId
)

// ClassSession
internal fun ClassSessionEntity.toDomain() = ClassSession(
    id = id, courseId = courseId, dayOfWeek = dayOfWeek,
    startMinutes = startMinutes, endMinutes = endMinutes,
    venue = venue, reminderOffsetsMinutes = reminderOffsetsMinutes
)
internal fun ClassSession.toEntity() = ClassSessionEntity(
    id = id, courseId = courseId, dayOfWeek = dayOfWeek,
    startMinutes = startMinutes, endMinutes = endMinutes,
    venue = venue, reminderOffsetsMinutes = reminderOffsetsMinutes
)

// Deadline
internal fun DeadlineEntity.toDomain() = Deadline(
    id = id, courseId = courseId, title = title, description = description,
    dueAtEpochMs = dueAtEpochMs, weightPercent = weightPercent,
    status = status, reminderOffsetsMinutes = reminderOffsetsMinutes,
    scoreObtained = scoreObtained, scoreMax = scoreMax
)
internal fun Deadline.toEntity() = DeadlineEntity(
    id = id, courseId = courseId, title = title, description = description,
    dueAtEpochMs = dueAtEpochMs, weightPercent = weightPercent,
    status = status, reminderOffsetsMinutes = reminderOffsetsMinutes,
    scoreObtained = scoreObtained, scoreMax = scoreMax
)

// Lecturer
internal fun LecturerEntity.toDomain() = Lecturer(
    id = id, name = name, email = email, phone = phone,
    officeLocation = officeLocation, officeHours = officeHours, notes = notes
)
internal fun Lecturer.toEntity() = LecturerEntity(
    id = id, name = name, email = email, phone = phone,
    officeLocation = officeLocation, officeHours = officeHours, notes = notes
)

// Question
internal fun QuestionEntity.toDomain() = Question(
    id = id, lecturerId = lecturerId, courseId = courseId,
    text = text, createdAtEpochMs = createdAtEpochMs, answeredAtEpochMs = answeredAtEpochMs
)
internal fun Question.toEntity() = QuestionEntity(
    id = id, lecturerId = lecturerId, courseId = courseId,
    text = text, createdAtEpochMs = createdAtEpochMs, answeredAtEpochMs = answeredAtEpochMs
)

// PersonalEvent
internal fun PersonalEventEntity.toDomain() = PersonalEvent(
    id = id, title = title, startEpochMs = startEpochMs, endEpochMs = endEpochMs,
    recurrenceRule = recurrenceRule, notes = notes
)
internal fun PersonalEvent.toEntity() = PersonalEventEntity(
    id = id, title = title, startEpochMs = startEpochMs, endEpochMs = endEpochMs,
    recurrenceRule = recurrenceRule, notes = notes
)

// StudySession
internal fun StudySessionEntity.toDomain() = StudySession(
    id = id, courseId = courseId,
    scheduledStartEpochMs = scheduledStartEpochMs, scheduledEndEpochMs = scheduledEndEpochMs,
    actualMinutesStudied = actualMinutesStudied, completedAtEpochMs = completedAtEpochMs
)
internal fun StudySession.toEntity() = StudySessionEntity(
    id = id, courseId = courseId,
    scheduledStartEpochMs = scheduledStartEpochMs, scheduledEndEpochMs = scheduledEndEpochMs,
    actualMinutesStudied = actualMinutesStudied, completedAtEpochMs = completedAtEpochMs
)

// Capture
internal fun CaptureEntity.toDomain() = Capture(
    id = id, courseId = courseId, type = type,
    filePath = filePath, createdAtEpochMs = createdAtEpochMs, note = note
)
internal fun Capture.toEntity() = CaptureEntity(
    id = id, courseId = courseId, type = type,
    filePath = filePath, createdAtEpochMs = createdAtEpochMs, note = note
)

// Exam
internal fun ExamEntity.toDomain() = Exam(
    id = id, courseId = courseId, examDateEpochMs = examDateEpochMs,
    venue = venue, topics = topics, pastPaperPaths = pastPaperPaths
)
internal fun Exam.toEntity() = ExamEntity(
    id = id, courseId = courseId, examDateEpochMs = examDateEpochMs,
    venue = venue, topics = topics, pastPaperPaths = pastPaperPaths
)

// Semester
internal fun SemesterEntity.toDomain() = Semester(
    id = id, name = name, startDateEpochMs = startDateEpochMs,
    endDateEpochMs = endDateEpochMs, isActive = isActive
)
internal fun Semester.toEntity() = SemesterEntity(
    id = id, name = name, startDateEpochMs = startDateEpochMs,
    endDateEpochMs = endDateEpochMs, isActive = isActive
)
