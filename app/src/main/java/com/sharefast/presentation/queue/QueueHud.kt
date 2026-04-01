package com.sharefast.presentation.queue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sharefast.domain.model.ShareableFile
import com.sharefast.utils.fileExtension
import com.sharefast.utils.mimeForFileName
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens (match HomeScreen)
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard  = 28.dp
private val RadiusChip  = 20.dp
private val RadiusPill  = 50.dp
private val PaddingPage = 20.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root overlay
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueHudOverlay(
    navController: NavController,
    viewModel: QueueHudViewModel = hiltViewModel(),
) {
    val items    by viewModel.items.collectAsState()
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Pulsing FAB scale for "attention" when queue has items
    val pulseAnim = rememberInfiniteTransition(label = "fab_pulse")
    val fabPulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue  = if (items.isNotEmpty()) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutSine),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible  = items.isNotEmpty(),
            enter    = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(),
            exit     = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 220.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                // Outer glow ring
                Box(
                    Modifier
                        .size(64.dp)
                        .scale(fabPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                FloatingActionButton(
                    onClick          = { sheetOpen = true },
                    modifier         = Modifier.scale(fabPulse),
                    containerColor   = MaterialTheme.colorScheme.primaryContainer,
                    contentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation        = FloatingActionButtonDefaults.elevation(8.dp),
                    shape            = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Outlined.FolderOpen, "Send queue", modifier = Modifier.size(24.dp))
                }
                // Badge
                Box(
                    Modifier
                        .offset(6.dp, (-6).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.errorContainer,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${items.size.coerceAtMost(99)}",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 10.sp,
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tonalElevation   = 0.dp,
            dragHandle       = {
                Box(
                    Modifier
                        .padding(top = 14.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
            },
        ) {
            QueueSheetContent(
                items      = items,
                viewModel  = viewModel,
                onNavigate = {
                    sheetOpen = false
                    navController.navigate("home") { launchSingleTop = true }
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheet body
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueSheetContent(
    items: List<ShareableFile>,
    viewModel: QueueHudViewModel,
    onNavigate: () -> Unit,
) {
    val listState   = rememberLazyListState()
    val totalBytes  = items.sumOf { it.sizeBytes }

    Column(
        Modifier
            .padding(horizontal = PaddingPage)
            .padding(bottom = 32.dp),
    ) {
        // ── Hero header ──────────────────────────────────────────────────────
        SheetHeader(count = items.size, totalBytes = totalBytes)

        Spacer(Modifier.height(20.dp))

        // ── File list ────────────────────────────────────────────────────────
        if (items.isEmpty()) {
            EmptyQueuePlaceholder()
        } else {
            LazyColumn(
                state                = listState,
                verticalArrangement  = Arrangement.spacedBy(10.dp),
                modifier             = Modifier.height(360.dp),
            ) {
                itemsIndexed(items, key = { _, f -> f.id }) { index, file ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.removeAt(index); true
                            } else false
                        },
                    )
                    SwipeToDismissBox(
                        state                    = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent        = { SwipeDismissBackground() },
                        modifier                 = Modifier.animateItem(
                            placementSpec = spring(Spring.DampingRatioMediumBouncy),
                        ),
                    ) {
                        StaggeredQueueRow(
                            file      = file,
                            index     = index,
                            lastIndex = items.lastIndex,
                            onRemove  = { viewModel.removeAt(index) },
                            onUp      = { viewModel.moveUp(index) },
                            onDown    = { viewModel.moveDown(index) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── CTA button ───────────────────────────────────────────────────────
        Button(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(RadiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) {
            Icon(Icons.Outlined.WifiTethering, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Go to Home & pick a device",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetHeader(count: Int, totalBytes: Long) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f),
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        // Decorative floating icon
        val t  = rememberInfiniteTransition(label = "hdr_float")
        val fy by t.animateFloat(
            0f, -6f,
            infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "fy",
        )
        Icon(
            Icons.Outlined.FolderOpen,
            null,
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(12.dp, fy.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Send Queue",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 36.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                QueueStatChip(label = "$count files", icon = Icons.Outlined.Description)
                QueueStatChip(label = formatTotalBytes(totalBytes), icon = Icons.Outlined.DataUsage)
            }
        }
    }
}

@Composable
private fun QueueStatChip(label: String, icon: ImageVector) {
    Row(
        Modifier
            .clip(RoundedCornerShape(RadiusChip))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Queue row — staggered entrance + glass card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredQueueRow(
    file: ShareableFile,
    index: Int,
    lastIndex: Int,
    onRemove: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L + 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(320)) + slideInHorizontally(
            tween(320, easing = EaseOutCubic),
            initialOffsetX = { -it / 5 },
        ),
    ) {
        QueueRow(
            file      = file,
            index     = index,
            lastIndex = lastIndex,
            onRemove  = onRemove,
            onUp      = onUp,
            onDown    = onDown,
        )
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
    val ctx   = LocalContext.current
    val ext   = fileExtension(file.displayName)
    val mime  = file.mimeType ?: mimeForFileName(file.displayName)
    val icon  = queueIconFor(ext, mime)
    val showThumb = mime.startsWith("image/")
    val isVideo   = mime.startsWith("video/")

    // Accent color based on type
    val accentColor = when {
        showThumb -> MaterialTheme.colorScheme.tertiary
        isVideo   -> MaterialTheme.colorScheme.secondary
        else      -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Thumbnail / icon
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (showThumb) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(file.uri)
                            .crossfade(200)
                            .build(),
                        contentDescription = null,
                        modifier     = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }

            // Name + size
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    file.displayName,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // File type pill
                    val typeLabel = when {
                        showThumb -> "Image"
                        isVideo   -> "Video"
                        else      -> ext.uppercase().ifBlank { "File" }
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            typeLabel,
                            style      = MaterialTheme.typography.labelSmall,
                            color      = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 9.sp,
                        )
                    }
                    Text(
                        formatTotalBytes(file.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Reorder controls
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ReorderButton(
                    icon    = Icons.Outlined.ArrowUpward,
                    enabled = index > 0,
                    onClick = onUp,
                )
                ReorderButton(
                    icon    = Icons.Outlined.ArrowDownward,
                    enabled = index < lastIndex,
                    onClick = onDown,
                )
            }

            // Remove button
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        "Remove",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.3f,
        label       = "reorder_alpha",
    )
    IconButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.size(30.dp).alpha(alpha),
    ) {
        Icon(
            icon,
            null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Swipe dismiss background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwipeDismissBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.errorContainer,
                    ),
                ),
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.Close,
                null,
                tint     = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Remove",
                color      = MaterialTheme.colorScheme.onErrorContainer,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyQueuePlaceholder() {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(52.dp)
                    .offset(y = fy.dp),
            )
            Text(
                "Queue is empty",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Pick files from Photos, Files or other apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun queueIconFor(ext: String, mime: String?): ImageVector {
    val m = mime ?: ""
    return when {
        m.startsWith("image/") || ext in setOf("jpg","jpeg","png","gif","webp","heic") -> Icons.Outlined.Image
        m.startsWith("video/") || ext in setOf("mp4","mkv","webm","mov") -> Icons.Outlined.VideoLibrary
        else -> Icons.Outlined.Description
    }
}

private fun formatTotalBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    var v = bytes.toDouble()
    val u = arrayOf("B", "KB", "MB", "GB")
    var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, u[i])
}