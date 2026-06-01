package app.lamla.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import app.lamla.data.repo.DeadlineRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * "TODAY - N classes / M deadlines" summary widget.
 */
class TodaySummaryWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun classRepo(): ClassSessionRepository
        fun deadlineRepo(): DeadlineRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val day = LocalDate.now().dayOfWeek
        val classesToday = entry.classRepo().observeAll().first().count { it.dayOfWeek == day }

        // End-of-today in millis
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = startOfDay + 24L * 60 * 60_000
        val now = System.currentTimeMillis()
        val deadlinesToday = entry.deadlineRepo().observeAll().first()
            .count { it.dueAtEpochMs in now until endOfDay }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFAFAF7)))
                    .padding(16.dp),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = "TODAY",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF8E8B84))
                    )
                )
                Text(
                    text = "$classesToday class" + if (classesToday == 1) "" else "es",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF1A1A1A))
                    )
                )
                Text(
                    text = "$deadlinesToday deadline" + if (deadlinesToday == 1) "" else "s",
                    style = TextStyle(color = ColorProvider(Color(0xFF4A4843)))
                )
            }
        }
    }
}

class TodaySummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaySummaryWidget()
}
