package com.sharefast.presentation.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharefast.domain.model.InstalledApp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — match HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard  = 28.dp
private val RadiusChip  = 20.dp
private val RadiusPill  = 50.dp
private val PaddingPage = 20.dp
private val SpacingSection = 24.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val apps      by viewModel.apps.collectAsState()
    val selected  by viewModel.selected.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState  = rememberLazyListState()

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppsTopBar(selectedCount = selected.size)
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut(),
            ) {
                AddToQueueFab(count = selected.size, onClick = viewModel::addSelectedToQueue)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Ambient background
            AppsAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(500)) + slideInVertically(
                    tween(500, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaddingPage),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // ── Hero info banner ─────────────────────────────────────
                    item {
                        AppsHeroBanner()
                    }

                    // ── Selection summary chip ───────────────────────────────
                    item {
                        AnimatedVisibility(
                            visible = selected.isNotEmpty(),
                            enter   = fadeIn() + expandVertically(),
                            exit    = fadeOut() + shrinkVertically(),
                        ) {
                            SelectionBanner(count = selected.size)
                        }
                    }

                    // ── Loading skeletons ────────────────────────────────────
                    if (isLoading) {
                        items(7) { index ->
                            StaggeredSkeleton(index = index)
                        }
                    } else if (apps.isEmpty()) {
                        item {
                            EmptyAppsPlaceholder()
                        }
                    }

                    // ── App list ─────────────────────────────────────────────
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        val isSel = app.packageName in selected
                        StaggeredAppRow(
                            app     = app,
                            isSel   = isSel,
                            index   = index,
                            onToggle = { viewModel.toggle(app.packageName) },
                        )
                    }

                    item { Spacer(Modifier.height(96.dp)) }
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
private fun AppsTopBar(selectedCount: Int) {
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
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Apps,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Apps",
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        "Share installed APKs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
// Hero banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppsHeroBanner() {
    val t  = rememberInfiniteTransition(label = "banner_float")
    val fy by t.animateFloat(
        0f, -6f,
        infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
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
        Icon(
            Icons.Outlined.Android,
            null,
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(10.dp, fy.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier            = Modifier.padding(end = 60.dp),
        ) {
            Text(
                "Installed Apps",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Tap to select APKs for sharing. System apps may be hidden by the OS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selection banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectionBanner(count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "$count APK${if (count > 1) "s" else ""} selected — tap FAB to queue",
            style      = MaterialTheme.typography.labelLarge,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App row with staggered entrance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredAppRow(
    app: InstalledApp,
    isSel: Boolean,
    index: Int,
    onToggle: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L + 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(300)) + slideInHorizontally(
            tween(300, easing = EaseOutCubic),
            initialOffsetX = { -it / 6 },
        ),
    ) {
        AppRow(app = app, isSel = isSel, onToggle = onToggle)
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    isSel: Boolean,
    onToggle: () -> Unit,
) {
    val selAnim by animateFloatAsState(
        targetValue   = if (isSel) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "sel_scale",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(250),
        label         = "border_col",
    )
    val cardBg by animateColorAsState(
        targetValue   = if (isSel)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(250),
        label         = "card_bg",
    )

    Card(
        shape     = RoundedCornerShape(20.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSel) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            ),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSel) 4.dp else 2.dp),
        onClick   = onToggle,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // App icon
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = app.launcherIcon
                if (bmp != null) {
                    Image(
                        bitmap             = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier           = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    // Fallback initial letter
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // App info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    app.label,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Size pill
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        formatSize(app.sizeBytes),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Animated check mark
            Box(
                Modifier
                    .size(32.dp)
                    .scale(0.6f + selAnim * 0.4f)
                    .clip(CircleShape)
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSel) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    tint        = if (isSel) Color.White
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier    = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddToQueueFab(count: Int, onClick: () -> Unit) {
    val breathe by rememberInfiniteTransition(label = "fab").animateFloat(
        1f, 1.04f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe",
    )
    ExtendedFloatingActionButton(
        onClick           = onClick,
        modifier          = Modifier.scale(breathe),
        shape             = RoundedCornerShape(RadiusPill),
        containerColor    = MaterialTheme.colorScheme.primary,
        contentColor      = Color.White,
        elevation         = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        icon = {
            Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(20.dp))
        },
        text = {
            Text(
                "Queue $count APK${if (count > 1) "s" else ""}",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
            )
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer skeleton rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredSkeleton(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(300)),
    ) {
        SkeletonAppRow()
    }
}

@Composable
private fun SkeletonAppRow() {
    val t     = rememberInfiniteTransition(label = "shimmer")
    val shimX by t.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimX",
    )
    val shimBrush = Brush.linearGradient(
        colors      = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1.0f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        start = Offset(shimX * 600f, 0f),
        end   = Offset(shimX * 600f + 300f, 0f),
    )

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon placeholder
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimBrush),
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.50f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimBrush),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.78f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimBrush),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.28f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimBrush),
                )
            }
            // Check placeholder
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(shimBrush),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyAppsPlaceholder() {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .clip(RoundedCornerShape(RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Apps,
                null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(52.dp)
                    .offset(y = fy.dp),
            )
            Text(
                "No apps found",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Check that ShareFast can see installed packages.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background — same as HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppsAmbientBackground(modifier: Modifier = Modifier) {
    val t     = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val tertiary  = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(brush = Brush.radialGradient(listOf(primary,   Color.Transparent), Offset(w * 0.2f + shift * w * 0.3f,   h * 0.15f), w * 0.7f))
        drawRect(brush = Brush.radialGradient(listOf(tertiary,  Color.Transparent), Offset(w * 0.85f - shift * w * 0.25f, h * 0.55f), w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(secondary, Color.Transparent), Offset(w * 0.5f, h * 0.9f + shift * h * 0.05f),   w * 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble(); var u = 0
    while (v >= 1024 && u < units.lastIndex) { v /= 1024; u++ }
    return "%.1f %s".format(v, units[u])
}