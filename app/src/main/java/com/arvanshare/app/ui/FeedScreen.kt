package com.arvanshare.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arvanshare.app.data.Keys
import com.arvanshare.app.data.Post
import com.arvanshare.app.data.RoomInfo
import com.arvanshare.app.viewmodel.AppViewModel
import com.arvanshare.app.viewmodel.FeedState

// Avatar colours cycle through a curated set based on the author name hash.
private val avatarColors = listOf(
    Color(0xFF3949AB),
    Color(0xFF00838F),
    Color(0xFF2E7D32),
    Color(0xFFD84315),
    Color(0xFF6A1B9A),
    Color(0xFF00695C),
    Color(0xFFAD1457),
    Color(0xFFEF6C00),
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
            color = Color.White,
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
    onOpenSettings: () -> Unit,
) {
    val posts by vm.cachedPosts.collectAsStateWithLifecycle()
    val feedState by vm.feedState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRoomDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.refreshFeed()
        vm.loadRooms()
    }

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ArvanShare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // Room selector chip
                        val roomLabel = if (Keys.isDefaultRoom(settings.activeRoom)) "🌐 General" else "#${settings.activeRoom}"
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    vm.loadRooms()
                                    showRoomDialog = true
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                if (!Keys.isDefaultRoom(settings.activeRoom)) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.size(3.dp))
                                }
                                Text(
                                    roomLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select room",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
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
                        TextButton(onClick = onOpenSettings) {
                            Text("Check Settings → connection details")
                        }
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
                            "Nothing in this room yet",
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

    if (showRoomDialog) {
        RoomSelectionDialog(
            activeRoom = settings.activeRoom,
            availableRooms = rooms,
            onDismiss = { showRoomDialog = false },
            onSelectRoom = { room, password ->
                vm.switchRoom(room, password) { error ->
                    if (error == null) {
                        showRoomDialog = false
                    }
                }
            },
        )
    }
}

@Composable
fun RoomSelectionDialog(
    activeRoom: String,
    availableRooms: List<RoomInfo>,
    onDismiss: () -> Unit,
    onSelectRoom: (String, String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var passwordPromptRoom by remember { mutableStateOf<RoomInfo?>(null) }

    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { roomName, pwd ->
                showCreateDialog = false
                onSelectRoom(roomName, pwd)
            },
        )
        return
    }

    if (passwordPromptRoom != null) {
        RoomPasswordDialog(
            room = passwordPromptRoom!!,
            onDismiss = { passwordPromptRoom = null },
            onSubmit = { pwd ->
                val r = passwordPromptRoom!!
                passwordPromptRoom = null
                onSelectRoom(r.name, pwd)
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Room") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Option: General (Root)
                val isGeneralActive = Keys.isDefaultRoom(activeRoom)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGeneralActive) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRoom("", "") },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("🌐 General (Default Feed)", fontWeight = FontWeight.SemiBold)
                        if (isGeneralActive) {
                            Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Discovered rooms
                if (availableRooms.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Discovered Rooms:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    for (r in availableRooms) {
                        val isRoomActive = r.name.equals(activeRoom, ignoreCase = true)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRoomActive) MaterialTheme.colorScheme.primaryContainer
                                                 else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (r.hasPassword) {
                                        passwordPromptRoom = r
                                    } else {
                                        onSelectRoom(r.name, "")
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (r.hasPassword) {
                                        Icon(Icons.Default.Lock, contentDescription = "Password protected", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(6.dp))
                                    }
                                    Text("#${r.name}", fontWeight = FontWeight.SemiBold)
                                }
                                if (isRoomActive) {
                                    Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { showCreateDialog = true }) {
                Text("➕ Join / Create Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var roomName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join / Create Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter a room name. If the room does not exist, it will be created. You can optionally set a simple password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name (e.g. family, work)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(roomName.trim(), password.trim()) },
                enabled = roomName.isNotBlank(),
            ) {
                Text("Enter Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun RoomPasswordDialog(
    room: RoomInfo,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password for #${room.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This room is protected by a password. Please enter it to continue.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Room Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(password.trim()) }, enabled = password.isNotBlank()) {
                Text("Unlock & Enter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
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
                    val mime = post.mediaMime ?: ""
                    val emoji = when {
                        post.mediaType == "image" || mime.startsWith("image/") -> "🖼️"
                        post.mediaType == "audio" || mime.startsWith("audio/") -> "🎙️"
                        else -> "📎"
                    }
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
