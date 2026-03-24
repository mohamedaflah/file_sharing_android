package com.sharefast.presentation.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sharefast.domain.model.ShareableFile
import com.sharefast.utils.fileExtension
import com.sharefast.utils.mimeForFileName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueHudOverlay(
    navController: NavController,
    viewModel: QueueHudViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = items.isNotEmpty(),
            enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
        ) {
            BadgedBox(
                badge = {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${items.size.coerceAtMost(99)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                },
            ) {
                FloatingActionButton(
                    onClick = { sheetOpen = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = "Send queue")
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(
                    "Send queue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${items.size} files · ${formatTotalBytes(items.sumOf { it.sizeBytes })}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp),
                ) {
                    itemsIndexed(items, key = { _, f -> f.id }) { index, file ->
                        QueueRow(
                            file = file,
                            index = index,
                            lastIndex = items.lastIndex,
                            onRemove = { viewModel.removeAt(index) },
                            onUp = { viewModel.moveUp(index) },
                            onDown = { viewModel.moveDown(index) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        sheetOpen = false
                        navController.navigate("home") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Go to Home & pick a device")
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    file: ShareableFile,
    index: Int,
    lastIndex: Int,
    onRemove: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val ext = fileExtension(file.displayName)
    val icon = queueIconFor(ext, file.mimeType)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    file.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    formatTotalBytes(file.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onUp, enabled = index > 0) {
                    Icon(Icons.Outlined.ArrowUpward, "Move up")
                }
                IconButton(onClick = onDown, enabled = index < lastIndex) {
                    Icon(Icons.Outlined.ArrowDownward, "Move down")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, "Remove")
            }
        }
    }
}

private fun queueIconFor(ext: String, mime: String?): ImageVector {
    val m = mime ?: ""
    return when {
        m.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "gif", "webp", "heic") -> Icons.Outlined.Image
        m.startsWith("video/") || ext in setOf("mp4", "mkv", "webm", "mov") -> Icons.Outlined.VideoLibrary
        else -> Icons.Outlined.Description
    }
}

private fun formatTotalBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    var v = bytes.toDouble()
    val u = arrayOf("B", "KB", "MB", "GB")
    var i = 0
    while (v >= 1024 && i < u.lastIndex) {
        v /= 1024
        i++
    }
    return "%.1f %s".format(v, u[i])
}
