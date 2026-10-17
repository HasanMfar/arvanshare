package com.arvanshare.app.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arvanshare.app.data.Post
import com.arvanshare.app.viewmodel.AppViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Inline voice-note player (works for both AAC/M4A from Android and WAV from
 * the desktop client). Downloads via authenticated S3, then plays with
 * MediaPlayer: play/pause, progress bar, elapsed/total duration.
 */
@Composable
fun VoicePlayer(post: Post, vm: AppViewModel, modifier: Modifier = Modifier) {
    val file by produceState<File?>(null, post.mediaUrl) { value = vm.mediaFile(post) }
    var playing by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    val player = remember { MediaPlayer() }

    // Prepare once the file is downloaded, and reset when playback ends.
    LaunchedEffect(file) {
        val f = file ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                player.reset()
                player.setDataSource(f.absolutePath)
                player.prepare()
            }
        }
        if (player.duration > 0) durationMs = player.duration
        player.setOnCompletionListener {
            playing = false
            positionMs = 0
            runCatching { player.seekTo(0) }
        }
    }

    // Progress ticker while playing.
    LaunchedEffect(playing) {
        while (playing && player.isPlaying) {
            positionMs = player.currentPosition
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.stop() }
            player.release()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledIconButton(
                onClick = {
                    runCatching {
                        if (playing) {
                            player.pause()
                            playing = false
                        } else {
                            if (player.currentPosition >= durationMs && durationMs > 0) {
                                player.seekTo(0)
                            }
                            player.start()
                            playing = true
                        }
                    }
                },
                enabled = file != null && durationMs > 0,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play voice note",
                )
            }
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = {
                        if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${formatMs(positionMs)} / ${if (durationMs > 0) formatMs(durationMs) else "…"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
