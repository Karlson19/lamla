package app.lamla.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.lamla.presentation.screens.home.HomeScreen
import app.lamla.presentation.screens.onboarding.OnboardingScreen
import app.lamla.presentation.screens.scaffold.MainScaffold
import app.lamla.ui.theme.lamla

/**
 * Top-level nav host.
 *
 * Structure:
 *   - If not onboarded → Onboarding flow → flips pref → re-enters at Home.
 *   - Otherwise → MainScaffold with bottom-bar + nested screen routing.
 *
 * Transition language:
 *   - Forward (deeper) — slide left + fade
 *   - Back (shallower) — slide right + fade
 *   - Both use [lamla.motion] emphasized tween for the slide.
 */
@Composable
fun LamlaNavHost(startOnboarded: Boolean) {
    val navController = rememberNavController()
    val motion = MaterialTheme.lamla.motion

    NavHost(
        navController = navController,
        startDestination = if (startOnboarded) Route.HomeGraph else Route.Onboarding,
        enterTransition = {
            fadeIn(animationSpec = tween(motion.medium2, easing = motion.standard)) +
                slideIntoContainer(Left, animationSpec = tween(motion.medium2, easing = motion.emphasized))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(motion.short4, easing = motion.standard)) +
                slideOutOfContainer(Left, animationSpec = tween(motion.medium2, easing = motion.emphasized))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(motion.medium2, easing = motion.standard)) +
                slideIntoContainer(Right, animationSpec = tween(motion.medium2, easing = motion.emphasized))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(motion.short4, easing = motion.standard)) +
                slideOutOfContainer(Right, animationSpec = tween(motion.medium2, easing = motion.emphasized))
        }
    ) {
        composable<Route.Onboarding> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Route.HomeGraph) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.HomeGraph> {
            MainScaffold(rootNavController = navController)
        }

        // Sub-routes that push above the scaffold (full-screen edit forms, detail screens).
        // (Implemented in respective screen packages; wired here so they're reachable from any tab.)
        composable<Route.ClassEdit> {
            val args = it.toRoute<Route.ClassEdit>()
            app.lamla.presentation.screens.timetable.ClassEditScreen(
                classId = args.classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.DeadlineEdit> {
            val args = it.toRoute<Route.DeadlineEdit>()
            app.lamla.presentation.screens.deadlines.DeadlineEditScreen(
                deadlineId = args.deadlineId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.CourseEdit> {
            val args = it.toRoute<Route.CourseEdit>()
            app.lamla.presentation.screens.courses.CourseEditScreen(
                courseId = args.courseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.CourseDetail> {
            val args = it.toRoute<Route.CourseDetail>()
            app.lamla.presentation.screens.courses.CourseDetailScreen(
                courseId = args.courseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.LecturerEdit> {
            val args = it.toRoute<Route.LecturerEdit>()
            app.lamla.presentation.screens.lecturers.LecturerEditScreen(
                lecturerId = args.lecturerId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.LecturerDetail> {
            val args = it.toRoute<Route.LecturerDetail>()
            app.lamla.presentation.screens.lecturers.LecturerDetailScreen(
                lecturerId = args.lecturerId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.Lecturers> {
            app.lamla.presentation.screens.lecturers.LecturersScreen(
                onBack = { navController.popBackStack() },
                onLecturer = { id -> navController.navigate(Route.LecturerDetail(id)) },
                onAdd = { navController.navigate(Route.LecturerEdit()) }
            )
        }
        composable<Route.Deadlines> {
            app.lamla.presentation.screens.deadlines.DeadlinesScreen(
                onBack = { navController.popBackStack() },
                onDeadline = { id -> navController.navigate(Route.DeadlineEdit(id)) },
                onAdd = { navController.navigate(Route.DeadlineEdit()) }
            )
        }
        composable<Route.Pomodoro> {
            app.lamla.presentation.screens.study.PomodoroScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.ExamMode> {
            app.lamla.presentation.screens.exam.ExamModeScreen(
                onBack = { navController.popBackStack() },
                onAddExam = { navController.navigate(Route.ExamEdit()) }
            )
        }
        composable<Route.ExamEdit> {
            val args = it.toRoute<Route.ExamEdit>()
            app.lamla.presentation.screens.exam.ExamEditScreen(
                examId = args.examId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.CaptureGallery> {
            val args = it.toRoute<Route.CaptureGallery>()
            app.lamla.presentation.screens.capture.CaptureGalleryScreen(
                courseId = args.courseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.PersonalEventEdit> {
            val args = it.toRoute<Route.PersonalEventEdit>()
            app.lamla.presentation.screens.personal.PersonalEventEditScreen(
                eventId = args.eventId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.NotificationSettings> {
            app.lamla.presentation.screens.settings.NotificationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.BatteryGuide> {
            app.lamla.presentation.screens.settings.BatteryGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.DataExportImport> {
            app.lamla.presentation.screens.settings.DataExportImportScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Route.Diagnostics> {
            app.lamla.presentation.screens.settings.DiagnosticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
