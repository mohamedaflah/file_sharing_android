package com.sharefast.presentation.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.sharefast.domain.model.MediaItem
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.utils.openAppSettings
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — match HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard  = 28.dp
private val RadiusChip  = 20.dp
private val RadiusPill  = 50.dp
private val PaddingPage = 16.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickScreen(
    kind: MediaKind,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val context  = LocalContext.current
    val mediaOk  = LocalMediaReadGranted.current
    val items    by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading  by viewModel.loading.collectAsState()

    val isPhotos = kind == MediaKind.IMAGES

    LaunchedEffect(kind, mediaOk) {
        if (mediaOk) viewModel.load(kind)
    }

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediaTopBar(
                isPhotos      = isPhotos,
                selectedCount = selected.size,
                itemCount     = items.size,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut(),
            ) {
                MediaQueueFab(
                    count    = selected.size,
                    isPhotos = isPhotos,
                    onClick  = { viewModel.addSelectedToQueue(kind) },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Ambient background
            MediaAmbientBackground(isPhotos = isPhotos, modifier = Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(500)) + slideInVertically(
                    tween(500, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                when {
                    // ── Permission gate ──────────────────────────────────────
                    !mediaOk -> {
                        MediaPermissionGate(
                            isPhotos       = isPhotos,
                            onOpenSettings = { context.openAppSettings() },
                            onRetry        = { viewModel.load(kind) },
                        )
                    }

                    // ── Loading ──────────────────────────────────────────────
                    loading -> {
                        MediaLoadingState(isPhotos = isPhotos)
                    }

                    // ── Empty ────────────────────────────────────────────────
                    items.isEmpty() -> {
                        MediaEmptyState(isPhotos = isPhotos)
                    }

                    // ── Grid ─────────────────────────────────────────────────
                    else -> {
                        MediaGrid(
                            items    = items,
                            selected = selected,
                            kind     = kind,
                            isPhotos = isPhotos,
                            onToggle = { viewModel.toggle(it) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaTopBar(
    isPhotos: Boolean,
    selectedCount: Int,
    itemCount: Int,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                if (isPhotos)
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                else
                                    listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPhotos) Icons.Outlined.PhotoLibrary else Icons.Outlined.VideoLibrary,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        if (isPhotos) "Photos" else "Videos",
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    if (itemCount > 0) {
                        Text(
                            "$itemCount item${if (itemCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = {
                    fadeIn(tween(200)) + scaleIn(initialScale = 0.85f) togetherWith
                        fadeOut(tween(150)) + scaleOut(targetScale = 0.85f)
                },
                label = "sel_count",
            ) { count ->
                if (count > 0) {
                    Box(
                        Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(RadiusChip))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "$count selected",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaQueueFab(count: Int, isPhotos: Boolean, onClick: () -> Unit) {
    val breathe by rememberInfiniteTransition(label = "fab").animateFloat(
        1f, 1.04f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe",
    )
    ExtendedFloatingActionButton(
        onClick        = onClick,
        modifier       = Modifier.scale(breathe),
        shape          = RoundedCornerShape(RadiusPill),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = Color.White,
        elevation      = FloatingActionButtonDefaults.elevation(8.dp),
        icon = {
            Icon(
                if (isPhotos) Icons.Outlined.PhotoLibrary else Icons.Outlined.VideoLibrary,
                null,
                modifier = Modifier.size(20.dp),
            )
        },
        text = {
            Text(
                "Queue $count ${if (isPhotos) "photo${if (count > 1) "s" else ""}" else "video${if (count > 1) "s" else ""}"}",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
            )
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Media grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    selected: Set<Long>,
    kind: MediaKind,
    isPhotos: Boolean,
    onToggle: (Long) -> Unit,
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns             = GridCells.Fixed(3),
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(
            start  = PaddingPage,
            end    = PaddingPage,
            top    = 4.dp,
            bottom = 120.dp,
        ),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Info strip header
        item(span = { GridItemSpan(3) }) {
            MediaInfoStrip(
                isPhotos  = isPhotos,
                itemCount = items.size,
                selCount  = selected.size,
            )
        }

        // Thumbnails
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val isSel = item.id in selected
            val imageModel = remember(item.uri, kind) {
                ImageRequest.Builder(context)
                    .data(item.uri)
                    .apply { if (kind == MediaKind.VIDEOS) videoFrameMillis(1_000L) }
                    .crossfade(true)
                    .build()
            }
            MediaThumb(
                imageModel = imageModel,
                isSelected = isSel,
                isVideo    = kind == MediaKind.VIDEOS,
                durationMs = item.durationMs,
                index      = index,
                onClick    = { onToggle(item.id) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info strip (full-width header above grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaInfoStrip(
    isPhotos: Boolean,
    itemCount: Int,
    selCount: Int,
) {
    val t  = rememberInfiniteTransition(label = "strip_float")
    val fy by t.animateFloat(
        0f, -5f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(RadiusCard))
            .background(
                Brush.linearGradient(
                    if (isPhotos)
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f),
                        )
                    else
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.60f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f),
                        ),
                ),
            )
            .padding(18.dp),
    ) {
        // Decorative icon
        Icon(
            if (isPhotos) Icons.Outlined.PhotoLibrary else Icons.Outlined.VideoLibrary,
            null,
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterEnd)
                .offset(12.dp, fy.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier            = Modifier.padding(end = 56.dp),
        ) {
            Text(
                if (isPhotos) "Tap photos to select" else "Tap videos to select",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (isPhotos)
                    "Selected thumbnails are added to your send queue."
                else
                    "Video thumbnails load from the first frame.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Count row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                MediaStatChip(
                    label = "$itemCount item${if (itemCount != 1) "s" else ""}",
                    icon  = Icons.Outlined.GridView,
                )
                AnimatedVisibility(
                    visible = selCount > 0,
                    enter   = fadeIn() + expandHorizontally(),
                    exit    = fadeOut() + shrinkHorizontally(),
                ) {
                    MediaStatChip(
                        label = "$selCount selected",
                        icon  = Icons.Outlined.CheckCircle,
                        tint  = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaStatChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Media thumbnail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaThumb(
    imageModel: ImageRequest,
    isSelected: Boolean,
    isVideo: Boolean,
    durationMs: Long?,
    index: Int,
    onClick: () -> Unit,
) {
    // Staggered entrance (capped so it doesn't take forever in large galleries)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index % 30) * 25L + 40L)
        visible = true
    }

    val selAnim by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "sel_$index",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label         = "border_$index",
    )
    val dimAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.25f else 0f,
        animationSpec = tween(200),
        label         = "dim_$index",
    )

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(280)) + scaleIn(
            tween(280, easing = EaseOutBack),
            initialScale = 0.88f,
        ),
    ) {
        Box(
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp),
                )
                .scale(1f - selAnim * 0.03f)  // subtle shrink while selected
                .clickable { onClick() },
        ) {
            // Thumbnail
            SubcomposeAsyncImage(
                model              = imageModel,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        // Shimmer placeholder
                        val t     = rememberInfiniteTransition(label = "thumb_shimmer")
                        val shimX by t.animateFloat(
                            -1f, 2f,
                            infiniteRepeatable(tween(1000, easing = LinearEasing)),
                            label = "shimX",
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                        start = Offset(shimX * 300f, 0f),
                                        end   = Offset(shimX * 300f + 200f, 0f),
                                    ),
                                ),
                        )
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (isVideo) Icons.Outlined.BrokenImage else Icons.Outlined.BrokenImage,
                                null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }

            // Selected dim overlay
            if (dimAlpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = dimAlpha)),
                )
            }

            // Video duration badge
            if (isVideo && durationMs != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                ) {
                    Text(
                        formatDuration(durationMs),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 9.sp,
                    )
                }
            }

            // Video play icon overlay
            if (isVideo) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            // Selected checkmark — animated scale in from center
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
                    .scale(selAnim)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission gate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaPermissionGate(
    isPhotos: Boolean,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(36.dp)
                .clip(RoundedCornerShape(RadiusCard))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            if (isPhotos)
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            else
                                listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPhotos) Icons.Outlined.PhotoLibrary else Icons.Outlined.VideoLibrary,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                if (isPhotos) "Gallery access needed" else "Video library access",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Photos & videos access is required to display your gallery for sharing.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(RadiusPill),
            ) {
                Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open app settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            OutlinedButton(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(RadiusPill),
            ) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("I already allowed — retry", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading state — skeleton grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaLoadingState(isPhotos: Boolean) {
    val t     = rememberInfiniteTransition(label = "shimmer")
    val shimX by t.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimX",
    )
    val shimBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        start  = Offset(shimX * 600f, 0f),
        end    = Offset(shimX * 600f + 300f, 0f),
    )
    LazyVerticalGrid(
        columns             = GridCells.Fixed(3),
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(PaddingPage),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Header skeleton
        item(span = { GridItemSpan(3) }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(RadiusCard))
                    .background(shimBrush),
            )
        }
        // Tile skeletons
        items(18) { index ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(index * 30L); visible = true }
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(250))) {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(shimBrush),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaEmptyState(isPhotos: Boolean) {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (isPhotos) Icons.Outlined.PhotoLibrary else Icons.Outlined.VideoLibrary,
                null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                modifier = Modifier.size(56.dp).offset(y = fy.dp),
            )
            Text(
                if (isPhotos) "No photos found" else "No videos found",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Grant media access or add ${if (isPhotos) "photos" else "videos"} to your device.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaAmbientBackground(isPhotos: Boolean, modifier: Modifier = Modifier) {
    val t     = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val c1 = if (isPhotos)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    else
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f)
    val c2 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)
    val c3 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(brush = Brush.radialGradient(listOf(c1, Color.Transparent), Offset(w * 0.2f + shift * w * 0.3f,  h * 0.15f), w * 0.7f))
        drawRect(brush = Brush.radialGradient(listOf(c2, Color.Transparent), Offset(w * 0.85f - shift * w * 0.25f, h * 0.55f), w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(c3, Color.Transparent), Offset(w * 0.5f, h * 0.9f + shift * h * 0.05f),  w * 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    val s = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val m = s / 60; val r = s % 60
    return "%d:%02d".format(m, r)
}