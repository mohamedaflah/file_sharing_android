package com.sharefast.presentation.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.utils.openAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val mediaOk = LocalMediaReadGranted.current
    val items by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) viewModel.importUris(uris)
    }

    LaunchedEffect(mediaOk) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = selected.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        onClick = viewModel::addSelectedToQueue,
                        icon = { Icon(Icons.Outlined.Description, null) },
                        text = { Text("Add ${selected.size} to queue") },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { pickLauncher.launch("*/*") },
                    icon = { Icon(Icons.Outlined.FolderOpen, null) },
                    text = { Text("Pick files") },
                )
            }
        },
    ) { padding ->
        when {
            !mediaOk && items.isEmpty() && !loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "To scan PDFs and Office files from storage, allow media access. You can also pick files directly.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { pickLauncher.launch("*/*") }) {
                        Text("Pick documents")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.openAppSettings() }) {
                        Text("Open app settings")
                    }
                }
            }
            loading && items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Files & downloads",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Indexed storage plus anything you pick. Tap a row to select for sending.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                    if (items.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ) {
                                Text(
                                    "No documents indexed yet. Use Pick files to add PDFs, archives, or anything else.",
                                    Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    items(items, key = { it.id }) { doc ->
                        val isSel = doc.id in selected
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggle(doc.id) },
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(doc.displayName, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    Text(
                                        formatDocSize(doc.sizeBytes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    if (isSel) "✓" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDocSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    var v = bytes.toDouble()
    val units = arrayOf("B", "KB", "MB", "GB")
    var u = 0
    while (v >= 1024 && u < units.lastIndex) {
        v /= 1024
        u++
    }
    return "%.1f %s".format(v, units[u])
}
