package app.lamla.presentation.screens.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import app.lamla.domain.model.Course
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.lamla
import kotlinx.coroutines.launch
import java.io.File

/**
 * Quick capture bottom sheet.
 *
 * Three options: Text / Photo / Voice (Voice scoped to "record" via a launcher
 * to system audio recorder via intent; we save the URI returned).
 *
 * Default course tag = currently-active class. If none, user picks before saving.
 *
 * Improvement over spec: text option (most common student input).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    activeCourse: Course?,
    allCourses: List<Course>,
    onDismiss: () -> Unit,
    onScheduleEvent: () -> Unit = {},
    viewModel: CaptureViewModel = hiltViewModel()
) {
    var mode by rememberSaveable { mutableStateOf<CaptureMode?>(null) }
    var selectedCourse by remember { mutableStateOf(activeCourse) }
    var textNote by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved && photoUri.value != null) {
            val course = selectedCourse
            scope.launch {
                viewModel.savePhoto(course?.id, photoUri.value!!.path ?: "")
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.lamla.spacing.cornerXl,
            topEnd = MaterialTheme.lamla.spacing.cornerXl
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("QUICK CAPTURE", style = LamlaTextStyles.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (activeCourse != null) "Will be tagged to ${activeCourse.code}"
                else "Pick a course to tag this to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Course chips
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LamlaChip(label = "No course", selected = selectedCourse == null, onClick = { selectedCourse = null })
                allCourses.forEach { c ->
                    LamlaChip(
                        label = c.code,
                        color = Color(c.colorArgb),
                        selected = c.id == selectedCourse?.id,
                        onClick = { selectedCourse = c }
                    )
                }
            }

            when (mode) {
                null -> {
                    // 2x2 grid - Text / Photo on top, Voice / Event on bottom.
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModeButton(label = "Text", icon = Icons.Outlined.EditNote, onClick = { mode = CaptureMode.Text }, modifier = Modifier.weight(1f))
                            ModeButton(label = "Photo", icon = Icons.Outlined.PhotoCamera, onClick = {
                                val tempFile = File(context.filesDir, "captures/photo_${System.currentTimeMillis()}.jpg")
                                tempFile.parentFile?.mkdirs()
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                photoUri.value = uri
                                photoLauncher.launch(uri)
                            }, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModeButton(label = "Voice", icon = Icons.Outlined.Mic, onClick = { mode = CaptureMode.Voice }, modifier = Modifier.weight(1f))
                            ModeButton(label = "Event", icon = Icons.Outlined.Event, onClick = {
                                onDismiss()
                                onScheduleEvent()
                            }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                CaptureMode.Text -> {
                    LamlaTextField(
                        value = textNote,
                        onValueChange = { textNote = it },
                        placeholder = "Quick note…",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 8
                    )
                    LamlaButton(
                        label = "Save note",
                        onClick = {
                            scope.launch {
                                if (textNote.isNotBlank()) {
                                    viewModel.saveText(selectedCourse?.id, textNote.trim())
                                }
                                onDismiss()
                            }
                        },
                        enabled = textNote.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CaptureMode.Voice -> {
                    Text("Voice recording is via the system recorder app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LamlaButton(label = "Open recorder", leadingIcon = Icons.Outlined.Mic, onClick = {
                        // Opens system audio recorder via intent. The user picks the file
                        // in a follow-up flow (kept simple to avoid a foreground service here).
                        val intent = android.content.Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                        runCatching { context.startActivity(intent) }
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private enum class CaptureMode { Text, Voice }

@Composable
private fun ModeButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg))
            .background(cs.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurface, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
    }
}
