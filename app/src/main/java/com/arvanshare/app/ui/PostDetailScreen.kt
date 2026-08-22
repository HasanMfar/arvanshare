package com.arvanshare.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arvanshare.app.data.Post
import com.arvanshare.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(vm: AppViewModel, onBack: () -> Unit) {
    val detail by vm.detail.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refreshDetail() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshDetail() }, enabled = !busy) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        val d = detail
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Post header card ────────────────────────────────────────────
            item { PostDetailHeader(d.post, vm) }

            // ── Likes row ──────────────────────────────────────────────────
            item {
                LikesRow(
                    likedByMe = d.likedByMe,
                    likes = d.likes,
                    busy = busy,
                    onToggle = { vm.toggleLike() },
                )
            }

            // ── Comments heading ──────────────────────────────────────────
            item {
                Text(
                    "Comments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (d.comments.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            "No comments yet. Be the first!",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(d.comments) { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AuthorAvatar(name = comment.author, size = 30)
                                Column {
                                    Text(comment.author, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatTime(comment.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                comment.text,
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // ── Comment input ─────────────────────────────────────────────
            item {
                var draft by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Add a comment") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium,
                    )
                    Button(
                        onClick = {
                            vm.addComment(draft.trim())
                            draft = ""
                        },
                        enabled = draft.isNotBlank() && !busy,
                    ) {
                        Text("Send")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LikesRow(
    likedByMe: Boolean,
    likes: List<String>,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    // Spring-scale animation on the heart icon
    var animTrigger by remember { mutableStateOf(likedByMe) }
    val scale by animateFloatAsState(
        targetValue = if (animTrigger) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { animTrigger = likedByMe },
        label = "like_scale",
    )
    val iconColor by animateColorAsState(
        targetValue = if (likedByMe) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "like_color",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    animTrigger = !animTrigger
                    onToggle()
                },
                enabled = !busy,
            ) {
                Icon(
                    imageVector = if (likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (likedByMe) "Unlike" else "Like",
                    tint = iconColor,
                    modifier = Modifier.scale(scale),
                )
            }
            Text(
                text = if (likes.isEmpty()) "No likes yet"
                       else "${likes.size} like${if (likes.size == 1) "" else "s"}: ${likes.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PostDetailHeader(post: Post, vm: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuthorAvatar(name = post.author, size = 44)
                Column {
                    Text(
                        post.author,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatTime(post.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (post.text.isNotBlank()) {
                Text(
                    post.text,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (post.mediaUrl != null) {
                PostMedia(post = post, vm = vm, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
