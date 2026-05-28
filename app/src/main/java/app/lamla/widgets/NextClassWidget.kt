package app.lamla.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.lamla.data.repo.ClassSessionRepository
import app.lamla.data.repo.CourseRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Home-screen widget: "NEXT — [Course code] — [Venue] at HH:MM".
 *
 * Glance reads from the same repositories via a Hilt entry point (widgets can't
 * @Inject directly). System triggers re-rendering every 15 min via the XML
 * provider config; we also nudge it after data mutations through WorkManager.
 *
 * Color choice: hard-coded to the light-theme tokens (ivory + ink) rather than
 * trying to follow Material You dynamic color. Widget chrome that flips with the
 * launcher wallpaper looks jarring next to a calm app interior. Users who prefer
 * dark wallpaper can switch via the system widget settings later if desired.
 */
class NextClassWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun classSessionRepo(): ClassSessionRepository
        fun courseRepo(): CourseRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val sessions = entry.classSessionRepo().observeAll().first()
        val courses = entry.courseRepo().observeAll().first().associateBy { it.id }

        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek
        val next = sessions
            .filter { it.dayOfWeek == today && it.startMinutes > (now.hour * 60 + now.minute) }
            .minByOrNull { it.startMinutes }
        val courseForNext = next?.let { courses[it.courseId] }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFAFAF7)))
                    .padding(16.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = "NEXT",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF8E8B84))
                    )
                )
                if (next == null) {
                    Text(
                        text = "Done for today",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF1A1A1A))
                        )
                    )
                } else {
                    Text(
                        text = courseForNext?.code ?: "Class",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF1A1A1A))
                        )
                    )
                    Text(
                        text = next.venue.ifBlank { "TBA" } + " · " +
                            "%02d:%02d".format(next.startMinutes / 60, next.startMinutes % 60),
                        style = TextStyle(color = ColorProvider(Color(0xFF4A4843)))
                    )
                }
            }
        }
    }
}

class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()
}
