package com.sharefast.presentation.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharefast.domain.model.MediaItem
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.utils.openAppSettings
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — match HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard     = 28.dp
private val RadiusChip     = 20.dp
private val RadiusPill     = 50.dp
private val PaddingPage    = 20.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel = hiltViewModel()) {
    val context  = LocalContext.current
    val mediaOk  = LocalMediaReadGranted.current
    val items    by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading  by viewModel.loading.collectAsState()
    val listState = rememberLazyListState()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris -> if (!uris.isNullOrEmpty()) viewModel.importUris(uris) }

    LaunchedEffect(mediaOk) { viewModel.refresh() }

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DocsTopBar(selectedCount = selected.size)
        },
        floatingActionButton = {
            DocsFloatingActions(
                selectedCount   = selected.size,
                onAddToQueue    = viewModel::addSelectedToQueue,
                onPick          = { pickLauncher.launch("*/*") },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Ambient background
            DocsAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(500)) + slideInVertically(
                    tween(500, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                when {
                    // ── Permission gate ──────────────────────────────────────
                    !mediaOk && items.isEmpty() && !loading -> {
                        PermissionGate(
                            onPick        = { pickLauncher.launch("*/*") },
                            onOpenSettings = { context.openAppSettings() },
                        )
                    }

                    // ── Loading ──────────────────────────────────────────────
                    loading && items.isEmpty() -> {
                        DocsLoadingState()
                    }

                    // ── Content ──────────────────────────────────────────────
                    else -> {
                        LazyColumn(
                            state               = listState,
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(horizontal = PaddingPage),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item { Spacer(Modifier.height(8.dp)) }

                            // Hero banner
                            item { DocsHeroBanner() }

                            // Selection summary
                            item {
                                AnimatedVisibility(
                                    visible = selected.isNotEmpty(),
                                    enter   = fadeIn() + expandVertically(),
                                    exit    = fadeOut() + shrinkVertically(),
                                ) {
                                    DocSelectionBanner(count = selected.size)
                                }
                            }

                            // Empty docs
                            if (items.isEmpty()) {
                                item { DocsEmptyPlaceholder(onPick = { pickLauncher.launch("*/*") }) }
                            }

                            // Document rows
                            itemsIndexed(items, key = { _, doc -> doc.id }) { index, doc ->
                                val isSel = doc.id in selected
                                StaggeredDocRow(
                                    doc      = doc,
                                    isSel    = isSel,
                                    index    = index,
                                    onToggle = { viewModel.toggle(doc.id) },
                                )
                            }

                            item { Spacer(Modifier.height(120.dp)) }
                        }
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
private fun DocsTopBar(selectedCount: Int) {
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
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Folder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "Documents",
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        "Files & downloads",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState  = selectedCount,
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
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "$count selected",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
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
// Floating action buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocsFloatingActions(
    selectedCount: Int,
    onAddToQueue: () -> Unit,
    onPick: () -> Unit,
) {
    val breathe by rememberInfiniteTransition(label = "fab").animateFloat(
        1f, 1.04f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe",
    )
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Add to queue FAB — shown only when items selected
        AnimatedVisibility(
            visible = selectedCount > 0,
            enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            ExtendedFloatingActionButton(
                onClick        = onAddToQueue,
                shape          = RoundedCornerShape(RadiusPill),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White,
                elevation      = FloatingActionButtonDefaults.elevation(8.dp),
                modifier       = Modifier.scale(breathe),
                icon           = {
                    Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(20.dp))
                },
                text           = {
                    Text(
                        "Queue $selectedCount file${if (selectedCount > 1) "s" else ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                    )
                },
            )
        }

        // Pick files FAB — always visible
        ExtendedFloatingActionButton(
            onClick        = onPick,
            shape          = RoundedCornerShape(RadiusPill),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation      = FloatingActionButtonDefaults.elevation(6.dp),
            icon           = {
                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(20.dp))
            },
            text           = {
                Text("Pick files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocsHeroBanner() {
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
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f),
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        Icon(
            Icons.Outlined.FolderOpen,
            null,
            tint     = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
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
                "Files & Downloads",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Indexed storage plus anything you pick. Tap a row to select for sending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selection summary banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocSelectionBanner(count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            null,
            tint     = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "$count file${if (count > 1) "s" else ""} selected — tap Queue to send",
            style      = MaterialTheme.typography.labelLarge,
            color      = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Document row — staggered entrance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredDocRow(
    doc: MediaItem,
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
        DocRow(doc = doc, isSel = isSel, onToggle = onToggle)
    }
}

@Composable
private fun DocRow(
    doc: MediaItem,
    isSel: Boolean,
    onToggle: () -> Unit,
) {
    // Derive file type accent
    val ext         = doc.displayName.substringAfterLast('.', "").lowercase()
    val accentColor = docAccentColor(ext)

    val selAnim by animateFloatAsState(
        targetValue   = if (isSel) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "sel_scale",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSel) accentColor else Color.Transparent,
        animationSpec = tween(250),
        label         = "border_col",
    )
    val cardBg by animateColorAsState(
        targetValue   = if (isSel)
            accentColor.copy(alpha = 0.10f)
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(250),
        label         = "card_bg",
    )

    Card(
        onClick   = onToggle,
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
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // File type icon
            Box(
                Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = docIconFor(ext),
                    contentDescription = null,
                    tint     = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Name + meta
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    doc.displayName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Extension pill
                    if (ext.isNotBlank()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                ext.uppercase(),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 9.sp,
                            )
                        }
                    }
                    Text(
                        formatDocSize(doc.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Animated checkmark
            Box(
                Modifier
                    .size(32.dp)
                    .scale(0.6f + selAnim * 0.4f)
                    .clip(CircleShape)
                    .background(
                        if (isSel) accentColor
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = if (isSel) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    tint     = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission gate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionGate(
    onPick: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(36.dp)
                .clip(RoundedCornerShape(RadiusCard))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                "Storage access",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Allow media access to auto-scan PDFs, archives and Office files. Or pick files directly without granting storage.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = onPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape    = RoundedCornerShape(RadiusPill),
            ) {
                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pick documents", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            OutlinedButton(
                onClick  = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape    = RoundedCornerShape(RadiusPill),
            ) {
                Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open app settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading state — shimmer skeletons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocsLoadingState() {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = PaddingPage),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        items(8) { index ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 55L)
                visible = true
            }
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
                DocSkeletonRow()
            }
        }
    }
}

@Composable
private fun DocSkeletonRow() {
    val t     = rememberInfiniteTransition(label = "shimmer")
    val shimX by t.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimX",
    )
    val shimBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1.0f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        start  = Offset(shimX * 600f, 0f),
        end    = Offset(shimX * 600f + 300f, 0f),
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
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(shimBrush))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.65f).height(13.dp).clip(RoundedCornerShape(8.dp)).background(shimBrush))
                Box(Modifier.fillMaxWidth(0.40f).height(10.dp).clip(RoundedCornerShape(8.dp)).background(shimBrush))
            }
            Box(Modifier.size(32.dp).clip(CircleShape).background(shimBrush))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocsEmptyPlaceholder(onPick: () -> Unit) {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
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
                Icons.Outlined.FolderOpen,
                null,
                tint     = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                modifier = Modifier.size(52.dp).offset(y = fy.dp),
            )
            Text(
                "No documents yet",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Use Pick files to add PDFs, archives, or any other file.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(
                onClick = onPick,
                shape   = RoundedCornerShape(RadiusChip),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pick files", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocsAmbientBackground(modifier: Modifier = Modifier) {
    val t     = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f)
    val tertiary  = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(brush = Brush.radialGradient(listOf(secondary, Color.Transparent), Offset(w * 0.15f + shift * w * 0.3f, h * 0.12f), w * 0.7f))
        drawRect(brush = Brush.radialGradient(listOf(tertiary,  Color.Transparent), Offset(w * 0.85f - shift * w * 0.2f,  h * 0.6f),  w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(primary,   Color.Transparent), Offset(w * 0.5f, h * 0.88f + shift * h * 0.05f),  w * 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun docAccentColor(ext: String): Color {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val error     = MaterialTheme.colorScheme.error
    return when (ext) {
        "pdf"                    -> error
        "doc", "docx"            -> primary
        "xls", "xlsx"            -> Color(0xFF16A34A)
        "ppt", "pptx"            -> Color(0xFFF97316)
        "zip", "rar", "7z", "gz" -> tertiary
        "txt", "md"              -> secondary
        else                     -> primary
    }
}

private fun docIconFor(ext: String): androidx.compose.ui.graphics.vector.ImageVector = when (ext) {
    "pdf"                    -> Icons.Outlined.PictureAsPdf
    "doc", "docx"            -> Icons.Outlined.Article
    "xls", "xlsx"            -> Icons.Outlined.TableChart
    "ppt", "pptx"            -> Icons.Outlined.Slideshow
    "zip", "rar", "7z", "gz" -> Icons.Outlined.FolderZip
    "txt", "md"              -> Icons.Outlined.TextSnippet
    else                     -> Icons.Outlined.Description
}

private fun formatDocSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    var v = bytes.toDouble()
    val units = arrayOf("B", "KB", "MB", "GB"); var u = 0
    while (v >= 1024 && u < units.lastIndex) { v /= 1024; u++ }
    return "%.1f %s".format(v, units[u])
}