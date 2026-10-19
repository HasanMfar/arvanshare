package com.arvanshare.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arvanshare.app.R
import com.arvanshare.app.data.BucketProfile
import com.arvanshare.app.data.Keys
import com.arvanshare.app.data.Settings
import com.arvanshare.app.viewmodel.AppViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    vm: AppViewModel,
    onBack: (() -> Unit)? = null,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var name by rememberSaveable(settings.name) { mutableStateOf(settings.name) }
    var endpoint by rememberSaveable(settings.endpoint) { mutableStateOf(settings.endpoint) }
    var bucket by rememberSaveable(settings.bucket) { mutableStateOf(settings.bucket) }
    var accessKey by rememberSaveable(settings.accessKey) { mutableStateOf(settings.accessKey) }
    var secretKey by rememberSaveable(settings.secretKey) { mutableStateOf(settings.secretKey) }
    var testResult by remember { mutableStateOf<String?>(null) }

    // Onboarding popup dialog state
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showAddBucketDialog by rememberSaveable { mutableStateOf(false) }

    // Auto-show guide once on first launch if setup is not complete
    var hasShownGuideAuto by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.setupComplete) {
        if (!settings.setupComplete && !hasShownGuideAuto) {
            hasShownGuideAuto = true
            showGuide = true
        }
    }

    val valid = name.isNotBlank() && endpoint.isNotBlank() && bucket.isNotBlank() &&
        accessKey.isNotBlank() && secretKey.isNotBlank()

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Settings & Buckets") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showGuide = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Setup Guide")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Brand header (shown on first-run setup or without onBack) ────────
            if (onBack == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color(0xFF2A2E55),
                                        androidx.compose.ui.graphics.Color(0xFF12131A),
                                    ),
                                ),
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = "ArvanShare logo",
                            modifier = Modifier.size(60.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                        )
                    }
                    Text(
                        "ArvanShare",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "A private feed for your circle, stored directly on ArvanCloud S3.\nEnter shared bucket details once — saved only on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = { showGuide = true },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("📖 How it works (Guide)")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Active Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { showGuide = true }) {
                        Text("📖 Setup Guide")
                    }
                }
            }

            // ── Form Fields ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your display name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    val handle = Keys.sanitizeName(name)
                    if (handle.isNotBlank() && handle != name.trim()) {
                        Text("Will appear as \"$handle\" (letters, digits and . _ - only)")
                    }
                },
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("S3 endpoint") },
                placeholder = { Text("https://s3.ir-thr-at1.arvanstorage.ir") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bucket,
                onValueChange = { bucket = it },
                label = { Text("Bucket name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("Access key") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                label = { Text("Secret key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Connection result chip ──────────────────────────────────────────
            testResult?.let { result ->
                val isOk = result == "Connection OK"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOk) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = if (isOk) "✓  $result" else "✗  $result",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // ── Actions ──────────────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    val draft = settings.copy(
                        name = name.trim(),
                        endpoint = endpoint.trim(),
                        bucket = bucket.trim(),
                        accessKey = accessKey.trim(),
                        secretKey = secretKey.trim(),
                    )
                    vm.testConnection(draft) { error ->
                        testResult = if (error == null) "Connection OK" else "Connection failed: $error"
                    }
                },
                enabled = valid && !busy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Test connection")
            }

            Button(
                onClick = {
                    val updated = settings.copy(
                        name = name.trim(),
                        endpoint = endpoint.trim(),
                        bucket = bucket.trim(),
                        accessKey = accessKey.trim(),
                        secretKey = secretKey.trim(),
                    )
                    vm.saveSettings(updated)
                    if (onBack != null) {
                        onBack()
                    }
                },
                enabled = valid && !busy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (onBack != null) "Save changes" else "Save & continue →")
            }

            // ── Multiple Buckets Section ────────────────────────────────────────
            if (onBack != null || settings.setupComplete) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DividerDefaults.color)
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Bucket Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = { showAddBucketDialog = true },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Add Bucket")
                    }
                }

                if (settings.profiles.isEmpty()) {
                    Text(
                        "Save the current bucket to add it to your profile list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (p in settings.profiles) {
                            val isActive = p.id == settings.activeProfileId ||
                                (p.bucket == settings.bucket && p.endpoint == settings.endpoint)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                     else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                p.label.ifBlank { p.bucket },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (isActive) {
                                                Spacer(Modifier.size(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = MaterialTheme.shapes.extraSmall,
                                                ) {
                                                    Text(
                                                        "Active",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "${p.bucket} · ${p.endpoint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Row {
                                        if (!isActive) {
                                            IconButton(onClick = {
                                                vm.switchBucketProfile(p.id)
                                                testResult = null
                                            }) {
                                                Icon(Icons.Default.Check, contentDescription = "Switch to bucket", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        if (settings.profiles.size > 1) {
                                            IconButton(onClick = { vm.deleteBucketProfile(p.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete profile", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Onboarding Guide Dialog ──────────────────────────────────────────────
    if (showGuide) {
        OnboardingDialog(onDismiss = { showGuide = false })
    }

    // ── Add Bucket Dialog ────────────────────────────────────────────────────
    if (showAddBucketDialog) {
        AddBucketDialog(
            onDismiss = { showAddBucketDialog = false },
            onAdd = { profile ->
                vm.addAndSwitchBucket(profile)
                showAddBucketDialog = false
                testResult = null
            },
        )
    }
}

@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🚀 Welcome to ArvanShare", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "ArvanShare is a serverless, private social feed designed for close circles (family, friends, or teams). All posts, images, and voice notes live directly in your ArvanCloud S3 bucket.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                HorizontalDivider()

                Text("📋 Setup Steps:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                GuideStepItem(
                    num = "1",
                    title = "Create an Object Storage Bucket",
                    desc = "Go to panel.arvancloud.ir, navigate to Object Storage, and create a bucket (e.g. 'my-family-feed').",
                )
                GuideStepItem(
                    num = "2",
                    title = "Get API Keys",
                    desc = "In ArvanCloud panel under Storage → API Keys, generate an Access Key and Secret Key with Read/Write permission.",
                )
                GuideStepItem(
                    num = "3",
                    title = "S3 Endpoint",
                    desc = "Use the default endpoint: https://s3.ir-thr-at1.arvanstorage.ir (or your specific ArvanCloud storage region).",
                )
                GuideStepItem(
                    num = "4",
                    title = "Share with your Circle",
                    desc = "Share the bucket name and keys with your circle members so everyone connects to the exact same bucket.",
                )
                GuideStepItem(
                    num = "5",
                    title = "Private Rooms & Passwords",
                    desc = "Use rooms (e.g. #family, #secret) with optional simple passwords to organize posts into private group spaces.",
                )
                GuideStepItem(
                    num = "6",
                    title = "Multiple Buckets",
                    desc = "You can add and switch between multiple buckets anytime from Settings & Buckets.",
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it! Let's go")
            }
        },
    )
}

@Composable
private fun GuideStepItem(num: String, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Text(num, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddBucketDialog(
    onDismiss: () -> Unit,
    onAdd: (BucketProfile) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("https://s3.ir-thr-at1.arvanstorage.ir") }
    var bucket by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    val valid = bucket.isNotBlank() && endpoint.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bucket Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Profile Label (e.g. Work, Family)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("S3 Endpoint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bucket,
                    onValueChange = { bucket = it },
                    label = { Text("Bucket Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    label = { Text("Access Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Secret Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        BucketProfile(
                            id = UUID.randomUUID().toString(),
                            label = label.ifBlank { bucket.trim() },
                            endpoint = endpoint.trim(),
                            bucket = bucket.trim(),
                            accessKey = accessKey.trim(),
                            secretKey = secretKey.trim(),
                        ),
                    )
                },
                enabled = valid,
            ) {
                Text("Add & Switch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
