package app.lamla.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lamla.notifications.OemBatteryGuide
import app.lamla.ui.components.LamlaButton
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.components.LamlaTopBar
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manufacturer = remember { OemBatteryGuide.detect() }
    val steps = remember(manufacturer) { OemBatteryGuide.steps(manufacturer) }
    val isOptimized = remember { mutableStateOf(!OemBatteryGuide.isBatteryOptimizationIgnored(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            LamlaTopBar(title = "Battery optimization", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("One small thing.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Your phone (${manufacturer.displayName}) aggressively kills background apps. To make sure class reminders fire on time, you'll want to disable battery optimization for Lamla.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LamlaSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("STEPS", style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    steps.forEachIndexed { idx, step ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface), contentAlignment = Alignment.Center) {
                                Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.surface)
                            }
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            LamlaButton(
                label = "Open settings",
                leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = { OemBatteryGuide.openSettings(context, manufacturer) },
                modifier = Modifier.fillMaxWidth()
            )

            if (!isOptimized.value) {
                Text("Lamla is currently exempt from battery optimization.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
