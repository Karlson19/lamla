package app.lamla.presentation.screens.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.domain.model.Capture
import app.lamla.domain.model.CaptureType
import app.lamla.ui.components.EmptyState
import app.lamla.ui.components.LamlaSurface
import app.lamla.ui.components.SectionLabel
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.lamla
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureGalleryScreen(
    courseId: Long?,
    onBack: () -> Unit,
    viewModel: CaptureGalleryViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Captures", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (state.captures.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(title = "Nothing captured yet.", body = "Notes, photos, and voice memos will appear here.")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(MaterialTheme.lamla.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.captures, key = { it.id }) { c -> CaptureRow(c) }
        }
    }
}

@Composable
private fun CaptureRow(c: Capture) {
    val date = remember(c.createdAtEpochMs) {
        Instant.ofEpochMilli(c.createdAtEpochMs).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
    }
    LamlaSurface(modifier = Modifier.fillMaxWidth(), contentPadding = MaterialTheme.lamla.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = when (c.type) {
                    CaptureType.Text -> Icons.Outlined.EditNote
                    CaptureType.Photo -> Icons.Outlined.PhotoCamera
                    CaptureType.Voice -> Icons.Outlined.Mic
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (c.type == CaptureType.Text) c.note else c.filePath.substringAfterLast('/').ifEmpty { c.note },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
