package com.arvanshare.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arvanshare.app.data.Post
import com.arvanshare.app.viewmodel.AppViewModel
import com.arvanshare.app.viewmodel.FeedState

// Avatar colours cycle through a curated set based on the author name hash.
// Shades are kept dark enough for white initials to stay readable (≥4.5:1).
private val avatarColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF3949AB),
    androidx.compose.ui.graphics.Color(0xFF00838F),
    androidx.compose.ui.graphics.Color(0xFF2E7D32),
    androidx.compose.ui.graphics.Color(0xFFD84315),
    androidx.compose.ui.graphics.Color(0xFF6A1B9A),
    androidx.compose.ui.graphics.Color(0xFF00695C),
    androidx.compose.ui.graphics.Color(0xFFAD1457),
    androidx.compose.ui.graphics.Color(0xFFEF6C00),
)

fun authorColor(name: String) = avatarColors[Math.abs(name.hashCode()) % avatarColors.size]

@Composable
fun AuthorAvatar(name: String, size: Int = 36) {
    val color = authorColor(name)
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
    ) {
        Text(
            text = initial,
            color = androidx.compose.ui.graphics.Color.White,
            style = if (size >= 40) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    vm: AppViewModel,
    onCompose: () -> Unit,
    onOpenPost: (Post) -> Unit,
) {
    val posts by vm.cachedPosts.collectAsStateWithLifecycle()
    val feedState by vm.feedState.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.refreshFeed() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeSnackbar()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ArvanShare",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    val isLoading = feedState is FeedState.Loading
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButton(onClick = { vm.refreshFeed() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New post")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                posts.isEmpty() && feedState is FeedState.Loading -> {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                posts.isEmpty() && feedState is FeedState.Error -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "Couldn't load the feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            (feedState as FeedState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Check Settings → connection details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                posts.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("✨", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "Nothing here yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Tap + to publish the first post.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        itemsIndexed(posts, key = { _, p -> p.postId }) { index, post ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { visible = true }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn() + slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                                    initialOffsetY = { it / 3 },
                                ),
                            ) {
                                PostCard(
                                    post = post,
                                    vm = vm,
                                    onClick = { onOpenPost(post) },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(88.dp)) } // FAB clearance
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post, vm: AppViewModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Author row ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AuthorAvatar(name = post.author, size = 38)
                Column(Modifier.weight(1f)) {
                    Text(
                        post.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatTime(post.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Media indicator badge
                if (post.mediaUrl != null) {
                    val emoji = if (post.mediaType == "image") "🖼️" else "📎"
                    Text(emoji, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ── Post text ──────────────────────────────────────────────────
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── Media preview ──────────────────────────────────────────────
            if (post.mediaUrl != null) {
                PostMedia(post = post, vm = vm, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}
