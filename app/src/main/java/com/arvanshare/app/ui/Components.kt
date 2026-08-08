package com.arvanshare.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.arvanshare.app.data.Post
import com.arvanshare.app.viewmodel.AppViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a post's attachment:
 * - images: downloaded via authenticated S3, shown inline with Coil
 * - anything else: an attachment chip (name + type) with a download/open action
 */
@Composable
fun PostMedia(post: Post, vm: AppViewModel, modifier: Modifier = Modifier) {
    val mediaUrl = post.mediaUrl ?: return
    val isImage = post.mediaType == "image" || (post.mediaMime ?: "").startsWith("image/")
    if (isImage) {
        val file by produceState<File?>(null, mediaUrl) {
            value = vm.mediaFile(post)
        }
        if (file != null) {
            AsyncImage(
                model = file,
                contentDescription = post.mediaName ?: "Post image",
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .aspectRatio(4f / 3f),
                contentScale = ContentScale.Crop,
            )
        }
    } else {
        AttachmentChip(post = post, vm = vm, modifier = modifier)
    }
}

/** Non-image attachment: name, MIME, and an open-in-viewer action (downloads first). */
@Composable
fun AttachmentChip(post: Post, vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val file by produceState<File?>(null, post.mediaUrl) {
        value = vm.mediaFile(post)
    }
    val label = post.mediaName ?: post.mediaUrl?.substringAfterLast('/') ?: "Attachment"
    val mime = post.mediaMime ?: "application/octet-stream"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(mime, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            val ready = file != null
            Button(
                onClick = {
                    val f = file
                    if (f != null) openFile(context, f, mime)
                },
                enabled = ready,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(if (ready) "Open" else "Downloading…")
            }
        }
    }
}

/** Opens a downloaded file with the system's default app for its MIME type. */
private fun openFile(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

fun formatTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp * 1000L))

/** Result of copying a picked file into a cache File the S3 client can upload. */
data class PickedFile(
    val file: File,
    val mimeType: String,
    val originalName: String,
)

/** Copies a picked Uri (any file type) into a cache File, keeping name + MIME. */
fun copyUriToCache(context: Context, uri: Uri): PickedFile? = runCatching {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    val originalName = queryDisplayName(resolver, uri) ?: "file_${System.currentTimeMillis()}"
    val ext = originalName.substringAfterLast('.', "")
    val out = File(context.cacheDir, "picked_${System.currentTimeMillis()}.${ext.ifBlank { "bin" }}")
    resolver.openInputStream(uri)?.use { input ->
        out.outputStream().use { input.copyTo(it) }
    }
    PickedFile(out, mimeType, originalName)
}.getOrNull()

/** Best-effort read of the document's display name via the OpenableColumns contract. */
private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? =
    runCatching {
        resolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
