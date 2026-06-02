package app.lamla.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.lamla.domain.usecase.StressScore
import app.lamla.ui.components.Hairline
import app.lamla.ui.components.LamlaChip
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.lamla
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Bottom sheet showing why today's stress score is what it is.
 *
 * Each row: deadline title + course tag + due-in + the points it contributes
 * to the headline number. Tap a row → jumps to the deadline.
 *
 * Visual: no traffic-light icons, no "high/medium/low" labels. The number IS
 * the signal; the bar to the right of each row shows the contribution share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StressBreakdownSheet(
    score: Int,
    contributions: List<StressScore.Contribution>,
    classLoad: StressScore.ClassLoad?,
    onDismiss: () -> Unit,
    onDeadlineClick: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.lamla.spacing.cornerXl,
            topEnd = MaterialTheme.lamla.spacing.cornerXl
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "WHAT'S DRIVING TODAY",
                style = LamlaTextStyles.SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
            }
            Spacer(Modifier.size(20.dp))

            if (contributions.isEmpty() && classLoad == null) {
                Text(
                    text = "Nothing pending. Your slate is clear.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Bar scale spans both drivers so class load and deadlines compare fairly.
                val maxPoints = (contributions.map { it.displayPoints } + listOfNotNull(classLoad?.displayPoints))
                    .maxOrNull()?.coerceAtLeast(1) ?: 1
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    classLoad?.let { cl ->
                        item(key = "class-load") {
                            ClassLoadRow(classLoad = cl, maxPoints = maxPoints)
                            Hairline(modifier = Modifier.fillMaxWidth().height(1.dp))
                        }
                    }
                    items(contributions, key = { it.deadline.id }) { c ->
                        ContributionRow(
                            contribution = c,
                            maxPoints = maxPoints,
                            onClick = { onDeadlineClick(c.deadline.id) }
                        )
                        Hairline(modifier = Modifier.fillMaxWidth().height(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(
    contribution: StressScore.Contribution,
    maxPoints: Int,
    onClick: () -> Unit
) {
    val course = contribution.course
    val accent = course?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val zone = ZoneId.systemDefault()
    val dueIn = remember(contribution.deadline.dueAtEpochMs) {
        humanizeDueIn(contribution.deadline.dueAtEpochMs - System.currentTimeMillis())
    }
    val dueDate = remember(contribution.deadline.dueAtEpochMs) {
        Instant.ofEpochMilli(contribution.deadline.dueAtEpochMs)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (course != null) LamlaChip(label = course.code, color = accent)
                Text(
                    text = dueIn,
                    style = LamlaTextStyles.Metric,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = contribution.deadline.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$dueDate · ${contribution.deadline.weightPercent.toInt()}% weight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        // Points + bar
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${contribution.displayPoints}",
                style = LamlaTextStyles.Metric,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(4.dp))
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.lamla.colors.timelineRail)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(contribution.displayPoints.toFloat() / maxPoints)
                        .background(accent.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun ClassLoadRow(classLoad: StressScore.ClassLoad, maxPoints: Int) {
    val accent = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                LamlaChip(label = "CLASSES", color = accent)
                Text(
                    text = "TODAY",
                    style = LamlaTextStyles.Metric,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (classLoad.classCount == 1) "1 class ahead" else "${classLoad.classCount} classes ahead",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${fmtHours(classLoad.contactHoursRemaining)} of contact time still to come today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${classLoad.displayPoints}",
                style = LamlaTextStyles.Metric,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(4.dp))
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.lamla.colors.timelineRail)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(classLoad.displayPoints.toFloat() / maxPoints)
                        .background(accent.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/** "4.0" → "4h", "2.5" → "2h 30m". */
private fun fmtHours(hours: Float): String {
    val totalMin = (hours * 60).roundToInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun humanizeDueIn(deltaMs: Long): String {
    val minutes = deltaMs / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        deltaMs <= 0 -> "OVERDUE"
        days >= 2 -> "IN ${days}D"
        hours >= 2 -> "IN ${hours}H"
        minutes >= 1 -> "IN ${minutes}M"
        else -> "DUE NOW"
    }
}

