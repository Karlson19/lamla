package app.lamla.presentation.screens.scaffold

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import app.lamla.presentation.navigation.Route
import app.lamla.presentation.screens.courses.CoursesScreen
import app.lamla.presentation.screens.home.HomeScreen
import app.lamla.presentation.screens.settings.SettingsScreen
import app.lamla.presentation.screens.study.StudyHubScreen
import app.lamla.presentation.screens.timetable.TimetableScreen
import app.lamla.ui.components.SectionLabel
import app.lamla.ui.theme.lamla
import app.lamla.ui.theme.softElevation
import kotlinx.coroutines.launch

/**
 * Vertical room the floating bottom nav needs *above* the system navigation inset:
 * the bar height (~52dp) + its 18dp gap + a little breathing space. Full-bleed tab
 * screens pad the bottom of their scrollable content by this (on top of the nav
 * inset) so the last row clears the bar, and float their FABs above it by the same
 * amount. Keeping the number here means the bar and the screens never drift apart.
 */
val FloatingNavClearance: Dp = 88.dp

/**
 * Top padding for a full-bleed tab screen: the status-bar inset plus a small [extra].
 * Content still scrolls *under* the transparent status bar; this just keeps the first
 * row from starting beneath it.
 */
@Composable
fun tabTopInset(extra: Dp = 8.dp): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra

/**
 * Bottom padding for a full-bleed tab screen (or its FAB): the system navigation
 * inset plus the floating-nav clearance, plus an optional [extra].
 */
@Composable
fun tabBottomInset(extra: Dp = 0.dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingNavClearance + extra

private enum class Tab(val label: String, val icon: ImageVector, val activeIcon: ImageVector) {
    Today("Today", Icons.Outlined.WbSunny, Icons.Outlined.WbSunny),
    Timetable("Timetable", Icons.Outlined.CalendarMonth, Icons.Outlined.CalendarMonth),
    Courses("Courses", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Study("Study", Icons.Outlined.Timer, Icons.Outlined.Timer),
    Settings("Settings", Icons.Outlined.Tune, Icons.Outlined.Tune)
}

/**
 * Bottom-nav scaffold.
 *
 * Differs from `androidx.compose.material3.NavigationBar` defaults in three ways:
 *   1. Floating bar with hairline border + rounded-large shape - not edge-to-edge "tab strip"
 *      that looks identical to every other Android app.
 *   2. Labels visible only on the active tab - keeps the bar tight on small phones,
 *      readable on first install.
 *   3. Animated content between tabs with directional slide based on tab index.
 *
 * Tabs are an enum so callers can't typo a route.
 */
@Composable
fun MainScaffold(rootNavController: NavController) {
    var selected by rememberSaveable { mutableStateOf(Tab.Today) }
    val haptic = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Hoisted out of transitionSpec below - that lambda is not @Composable, so it
    // can't read the @Composable `MaterialTheme.lamla.motion` getter directly.
    val motion = MaterialTheme.lamla.motion
    val medium2 = motion.medium2
    val short4 = motion.short4

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LamlaDrawerSheet(
                selected = selected,
                onSelectTab = { tab ->
                    if (tab != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selected = tab
                    scope.launch { drawerState.close() }
                },
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    rootNavController.navigate(route)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Screen content - animated between tabs.
            AnimatedContent(
                targetState = selected,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val direction = if (forward) Left else Right
                    (fadeIn(animationSpec = tween(medium2)) +
                        slideIntoContainer(direction, tween(medium2)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(short4)) +
                                slideOutOfContainer(direction, tween(medium2))
                        )
                },
                label = "tab-content"
            ) { tab ->
                when (tab) {
                    Tab.Today -> HomeScreen(
                        onOpenDeadlines = { rootNavController.navigate(Route.Deadlines) },
                        onAddCapture = { /* opens quick-capture sheet - wired in HomeScreen */ },
                        onClassClick = { id -> rootNavController.navigate(Route.ClassEdit(id)) },
                        onDeadlineClick = { id -> rootNavController.navigate(Route.DeadlineEdit(id)) },
                        onStartPomodoro = { rootNavController.navigate(Route.Pomodoro) },
                        onScheduleEvent = { rootNavController.navigate(Route.PersonalEventEdit()) },
                        onPersonalEventClick = { id -> rootNavController.navigate(Route.PersonalEventEdit(id)) }
                    )
                    Tab.Timetable -> TimetableScreen(
                        onAddClass = { rootNavController.navigate(Route.ClassEdit()) },
                        onClassClick = { id -> rootNavController.navigate(Route.ClassEdit(id)) }
                    )
                    Tab.Courses -> CoursesScreen(
                        onCourseClick = { id -> rootNavController.navigate(Route.CourseDetail(id)) },
                        onAddCourse = { rootNavController.navigate(Route.CourseEdit()) },
                        onOpenLecturers = { rootNavController.navigate(Route.Lecturers) },
                        onOpenGrades = { rootNavController.navigate(Route.Grades) }
                    )
                    Tab.Study -> StudyHubScreen(
                        onStartPomodoro = { rootNavController.navigate(Route.Pomodoro) },
                        onOpenExamMode = { rootNavController.navigate(Route.ExamMode) }
                    )
                    Tab.Settings -> SettingsScreen(
                        onNotificationSettings = { rootNavController.navigate(Route.NotificationSettings) },
                        onBatteryGuide = { rootNavController.navigate(Route.BatteryGuide) },
                        onDataExportImport = { rootNavController.navigate(Route.DataExportImport) },
                        onDiagnostics = { rootNavController.navigate(Route.Diagnostics) }
                    )
                }
            }

            // Floating bottom nav bar
            FloatingBottomNav(
                selected = selected,
                onSelect = {
                    if (it != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selected = it
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Sit above the system navigation bar (gesture pill or 3-button)
                    // so the floating tab bar never merges with the OS nav buttons.
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp, start = 20.dp, end = 20.dp)
            )

            // Floating hamburger - opens the navigation drawer. Pinned top-end so it
            // clears the left-aligned editorial headers on every tab screen.
            FloatingMenuButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 20.dp)
            )
        }
    }
}

@Composable
private fun FloatingBottomNav(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Frosted float: a soft lift + slight translucency so the page's aurora
            // and content glow through the bar instead of hard-stopping under it.
            .softElevation(shape, radius = 18.dp)
            .clip(shape)
            .background(cs.surfaceContainerLow.copy(alpha = 0.92f), shape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Inactive tabs hug their icon; the active tab expands to fit its label.
        // SpaceBetween then distributes the slack evenly between them.
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Tab.entries.forEach { tab ->
            NavItem(
                tab = tab,
                isActive = tab == selected,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val motion = MaterialTheme.lamla.motion
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull)

    // Cross-fade the pill + content colors so the selection glides between tabs
    // instead of snapping; the label expands/collapses on the same beat.
    val bg by animateColorAsState(
        targetValue = if (isActive) cs.onSurface else Color.Transparent,
        animationSpec = motion.tweenStandard(motion.medium2),
        label = "nav-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (isActive) cs.surface else cs.onSurfaceVariant,
        animationSpec = motion.tweenStandard(motion.medium2),
        label = "nav-fg"
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isActive) 14.dp else 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isActive) tab.activeIcon else tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier.size(20.dp)
        )
        AnimatedVisibility(
            visible = isActive,
            enter = androidx.compose.animation.fadeIn(tween(motion.short4)) +
                expandHorizontally(tween(motion.medium2)),
            exit = androidx.compose.animation.fadeOut(tween(motion.short4)) +
                shrinkHorizontally(tween(motion.medium2))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Slide-in navigation drawer (the "hamburger").
 *
 * Surfaces *every* destination - including the ones otherwise buried a tap or two
 * deep inside a tab (Deadlines, Lecturers, Grades, Exam mode) - so nothing in the
 * app is hidden behind the five-tab bar. Tapping a primary destination switches the
 * bottom-nav tab; tapping a secondary one pushes its screen above the scaffold.
 */
@Composable
private fun LamlaDrawerSheet(
    selected: Tab,
    onSelectTab: (Tab) -> Unit,
    onNavigate: (Route) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    ModalDrawerSheet(
        drawerContainerColor = cs.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Brand mark
            Column(modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 12.dp)) {
                Text(
                    text = "LAMLA",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.primary
                )
                Text(
                    text = "Last-minute learners, sorted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            SectionLabel(text = "Navigate", modifier = Modifier.padding(start = 6.dp, top = 4.dp))
            DrawerItem(Icons.Outlined.WbSunny, "Today", selected == Tab.Today) { onSelectTab(Tab.Today) }
            DrawerItem(Icons.Outlined.CalendarMonth, "Timetable", selected == Tab.Timetable) { onSelectTab(Tab.Timetable) }
            DrawerItem(Icons.AutoMirrored.Outlined.MenuBook, "Courses", selected == Tab.Courses) { onSelectTab(Tab.Courses) }
            DrawerItem(Icons.Outlined.Timer, "Study", selected == Tab.Study) { onSelectTab(Tab.Study) }

            SectionLabel(text = "Academics", modifier = Modifier.padding(start = 6.dp, top = 12.dp))
            DrawerItem(Icons.AutoMirrored.Outlined.EventNote, "Deadlines") { onNavigate(Route.Deadlines) }
            DrawerItem(Icons.Outlined.Person, "Lecturers") { onNavigate(Route.Lecturers) }
            DrawerItem(Icons.Outlined.School, "Grades & CWA") { onNavigate(Route.Grades) }
            DrawerItem(Icons.AutoMirrored.Outlined.FactCheck, "Attendance") { onNavigate(Route.Attendance) }
            DrawerItem(Icons.AutoMirrored.Outlined.Assignment, "Exam mode") { onNavigate(Route.ExamMode) }

            SectionLabel(text = "More", modifier = Modifier.padding(start = 6.dp, top = 12.dp))
            DrawerItem(Icons.Outlined.Tune, "Settings", selected == Tab.Settings) { onSelectTab(Tab.Settings) }
        }
    }
}

/** A single drawer row. Selected rows invert (filled pill) like the bottom-nav active tab. */
@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val motion = MaterialTheme.lamla.motion
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerMd)
    val bg by animateColorAsState(
        targetValue = if (selected) cs.onSurface else Color.Transparent,
        animationSpec = motion.tweenStandard(motion.medium2),
        label = "drawer-bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) cs.surface else cs.onSurface,
        animationSpec = motion.tweenStandard(motion.medium2),
        label = "drawer-fg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = fg, maxLines = 1)
    }
}

/** Floating circular hamburger that opens the drawer - matches the floating bottom-nav language. */
@Composable
private fun FloatingMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(44.dp)
            .softElevation(CircleShape, radius = 12.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerLow.copy(alpha = 0.92f), CircleShape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Open navigation menu",
            tint = cs.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}
