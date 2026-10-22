package com.arvanshare.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.arvanshare.app.data.VoiceRecorder
import com.arvanshare.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostScreen(vm: AppViewModel, onDone: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // ── Voice recording state ─────────────────────────────────────────────
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        recordSeconds = 0
        while (recording) {
            delay(1_000)
            recordSeconds += 1
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            copyUriToCache(vm.getApplication(), uri)?.let { picked = it }
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            runCatching {
                recorder.start()
                recording = true
            }.onFailure { error = it.message ?: "Could not start recording" }
        } else {
            error = "Microphone permission is needed to record voice notes."
        }
    }

    val postEnabled = (text.isNotBlank() || picked != null) && !busy && !recording

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Text input ─────────────────────────────────────────────────
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's on your mind?") },
                minLines = 5,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )

            // ── Recording indicator ────────────────────────────────────────
            if (recording) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Recording voice note…",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "%d:%02d".format(recordSeconds / 60, recordSeconds % 60),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                val file = recorder.stop()
                                recording = false
                                if (file != null) {
                                    picked = PickedFile(
                                        file = file,
                                        mimeType = "audio/mp4",
                                        originalName = file.name,
                                    )
                                } else {
                                    error = "Recording was too short — try again."
                                }
                            },
                            enabled = recordSeconds >= 1,
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Text(" Stop", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            // ── Attachment preview ─────────────────────────────────────────
            picked?.let { pickedItem ->
                val isImage = pickedItem.mimeType.startsWith("image/")
                val isAudio = pickedItem.mimeType.startsWith("audio/")
                Box {
                    if (isImage) {
                        Image(
                            painter = rememberAsyncImagePainter(pickedItem.file),
                            contentDescription = pickedItem.originalName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    if (isAudio) Icons.Filled.Mic else Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = if (isAudio) MaterialTheme.colorScheme.secondary
                                           else MaterialTheme.colorScheme.primary,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (isAudio) "Voice note (%d:%02d)".format(recordSeconds / 60, recordSeconds % 60)
                                        else pickedItem.originalName,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        pickedItem.mimeType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    // Remove button (top-right of the preview)
                    IconButton(
                        onClick = { picked = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove attachment",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }

            // ── Attach / record buttons ─────────────────────────────────────
            if (picked == null && !recording) {
                FilledTonalButton(
                    onClick = { picker.launch("*/*") },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Text(" Attach a file or image", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            runCatching {
                                recorder.start()
                                recording = true
                            }.onFailure { err -> error = err.message ?: "Could not start recording" }
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Text(" Record a voice note", modifier = Modifier.padding(start = 6.dp))
                }
            }

            // ── Error ──────────────────────────────────────────────────────
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Publish ────────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.composePost(
                        text,
                        picked?.file,
                        picked?.mimeType,
                        picked?.originalName,
                    ) { composeError ->
                        if (composeError == null) onDone() else error = composeError
                    }
                },
                enabled = postEnabled,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    "Publish",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
