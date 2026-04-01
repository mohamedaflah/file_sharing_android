package com.sharefast.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sharefast.core.ShareConstants
import com.sharefast.data.repository.SavedPeer
import com.sharefast.domain.model.PeerDevice
import com.sharefast.domain.model.TransferDirection
import com.sharefast.presentation.MainAppearanceViewModel
import com.sharefast.presentation.TransferNavExtras
import com.sharefast.presentation.chat.chatRoute
import com.sharefast.presentation.permissions.LocalPermissionsFlowCompleted
import com.sharefast.presentation.theme.GlassDark
import com.sharefast.presentation.theme.GlassLight
import com.sharefast.presentation.ui.pressScale
import com.sharefast.utils.QrUtils
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private val RadiusCard = 28.dp
private val RadiusChip = 20.dp
private val RadiusPill = 50.dp
private val PaddingPage = 20.dp
private val SpacingSection = 24.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    amoledBlack: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    onThemeCycle: () -> Unit,
    transferNavExtras: TransferNavExtras = TransferNavExtras(),
    onConsumedTransferNav: () -> Unit = {},
) {
    val ui by viewModel.uiState.collectAsState()
    val recent by viewModel.recentTransfers.collectAsState()
    val lastPeer by viewModel.lastPeer.collectAsState()
    val progress by viewModel.transferProgress.collectAsState()
    val activity = LocalContext.current as ComponentActivity
    val appearanceVm = hiltViewModel<MainAppearanceViewModel>(activity)
    val greetingStored by appearanceVm.greetingName.collectAsStateWithLifecycle()

    var greetingEditOpen by remember { mutableStateOf(false) }
    var greetingDraft by remember { mutableStateOf("") }
    var homeTab by rememberSaveable { mutableIntStateOf(0) }
    val corePermissionsReady = LocalPermissionsFlowCompleted.current
    val listState = rememberLazyListState()

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(ui.deviceName) }
    var qrOpen by remember { mutableStateOf(false) }
    var transferSheetDismissed by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Entrance animation ──────────────────────────────────────────────────
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
    }

    LaunchedEffect(corePermissionsReady) {
        if (corePermissionsReady) viewModel.startLanFeaturesIfNeeded()
    }

    LaunchedEffect(transferNavExtras.openUri, transferNavExtras.scrollToRecent) {
        when {
            transferNavExtras.openUri != null -> {
                viewModel.openUriForFile(
                    transferNavExtras.openUri!!,
                    transferNavExtras.openFileNameHint ?: "file",
                    onFinished = onConsumedTransferNav,
                )
            }
            transferNavExtras.scrollToRecent -> {
                delay(220)
                val n = listState.layoutInfo.totalItemsCount
                if (n > 0) listState.scrollToItem((n - 1).coerceAtLeast(0))
                onConsumedTransferNav()
            }
        }
    }

    LaunchedEffect(progress) {
        if (progress != null) transferSheetDismissed = false
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────

    if (ui.error != null) {
        ModernAlertDialog(
            title = "Notice",
            message = ui.error ?: "",
            onDismiss = viewModel::clearError,
        )
    }

    if (greetingEditOpen) {
        AlertDialog(
            onDismissRequest = { greetingEditOpen = false },
            shape = RoundedCornerShape(RadiusCard),
            title = { Text("Your greeting name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Shown on the home screen — stored only on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = greetingDraft,
                        onValueChange = { greetingDraft = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("e.g. Alex") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    appearanceVm.setGreeting(greetingDraft)
                    greetingEditOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { greetingEditOpen = false }) { Text("Cancel") }
            },
        )
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            shape = RoundedCornerShape(RadiusCard),
            title = { Text("Device name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    viewModel.renameDevice(renameText)
                    renameOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text("Cancel") }
            },
        )
    }

    if (qrOpen && ui.ipAddress != null) {
        val payload = remember(ui.ipAddress) {
            QrUtils.buildConnectPayload(ui.ipAddress!!, ShareConstants.TCP_PORT)
        }
        val bmp = remember(payload) { QrUtils.encodeQrBitmap(payload) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { qrOpen = false }) {
            Card(
                shape = RoundedCornerShape(RadiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Scan to connect", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .size(230.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(10.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        payload,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { qrOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusChip),
                    ) { Text("Close") }
                }
            }
        }
    }

    // ── Transfer bottom sheet ────────────────────────────────────────────────

    if (progress != null && !transferSheetDismissed) {
        val p = progress!!
        val fracRaw = if (p.totalBytes > 0) {
            (p.bytesTransferred.toFloat() / p.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val fracAnimated by animateFloatAsState(
            targetValue = fracRaw,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "xfer",
        )
        val remaining = (p.totalBytes - p.bytesTransferred).coerceAtLeast(0L)
        val etaText = if (p.speedBytesPerSecond > 1024.0 && remaining > 0) {
            val sec = (remaining / p.speedBytesPerSecond).toLong().coerceAtLeast(1L)
            when {
                sec < 60 -> "~${sec}s remaining"
                sec < 3600 -> "~${sec / 60}m remaining"
                else -> "~${sec / 3600}h remaining"
            }
        } else "Calculating…"
        val isComplete = p.totalBytes > 0 && p.bytesTransferred >= p.totalBytes - 2
        val mbps = p.speedBytesPerSecond / (1024.0 * 1024.0)

        ModalBottomSheet(
            onDismissRequest = { transferSheetDismissed = true },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            TransferSheet(
                fracAnimated = fracAnimated,
                fileName = p.currentFileName,
                fileIndex = p.currentFileIndex,
                totalFiles = p.totalFiles,
                mbps = mbps,
                etaText = etaText,
                isComplete = isComplete,
                isPaused = p.isPaused,
                onPause = viewModel::pauseTransfer,
                onResume = viewModel::resumeTransfer,
                onCancel = viewModel::cancelTransfer,
            )
        }
    }

    // ── Main scaffold ────────────────────────────────────────────────────────

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ModernTopBar(
                amoledBlack = amoledBlack,
                onAmoledChange = onAmoledChange,
                onThemeCycle = onThemeCycle,
                onAccentCycle = { appearanceVm.cycleAccent() },
            )
        },
    ) { padding ->
        val sentRecent = recent.filter {
            it.direction == TransferDirection.SENT && !it.storageUri.isNullOrBlank()
        }.take(8)

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HomeAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(500)) + slideInVertically(
                    animationSpec = tween(500, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaddingPage),
                    verticalArrangement = Arrangement.spacedBy(SpacingSection),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        HomeGreetingCard(
                            greetingName = greetingStored,
                            deviceName = ui.deviceName,
                            onEdit = {
                                greetingDraft = greetingStored
                                greetingEditOpen = true
                            },
                        )
                    }

                    item {
                        ModernTabRow(selected = homeTab, onSelect = { homeTab = it })
                    }

                    if (homeTab == 0) {

                        if (lastPeer != null) {
                            item {
                                AnimatedItem(index = 0) {
                                    LastPeerCard(
                                        peer = lastPeer!!,
                                        onReconnect = { viewModel.reconnectLastPeer() },
                                    )
                                }
                            }
                        }

                        item {
                            AnimatedItem(index = 1) {
                                DeviceInfoCard(
                                    deviceName = ui.deviceName,
                                    ip = ui.ipAddress ?: "No LAN address",
                                    connection = ui.connectionLabel,
                                    discoveryOn = ui.discoveryActive,
                                    queueCount = ui.queueCount,
                                    wifiBars = ui.wifiBars,
                                    onRename = {
                                        renameText = ui.deviceName
                                        renameOpen = true
                                    },
                                    onShowQr = { qrOpen = true },
                                )
                            }
                        }

                        item {
                            AnimatedItem(index = 2) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    GradientActionCard(
                                        label = "Queue",
                                        subtitle = if (ui.queueCount > 0) "${ui.queueCount} ready" else "Add from tabs",
                                        gradient = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                        ),
                                        icon = Icons.Outlined.CloudUpload,
                                        modifier = Modifier.weight(1f),
                                        onClick = { homeTab = 0 },
                                    )
                                    GradientActionCard(
                                        label = "Receive",
                                        subtitle = "Port ${ShareConstants.TCP_PORT}",
                                        gradient = listOf(
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.secondary,
                                        ),
                                        icon = Icons.Outlined.CloudDownload,
                                        modifier = Modifier.weight(1f),
                                        onClick = { homeTab = 1 },
                                    )
                                }
                            }
                        }

                        item {
                            AnimatedItem(index = 3) {
                                SectionHeader(
                                    title = "Nearby Devices",
                                    subtitle = "Tap a device to send your queue",
                                )
                            }
                        }

                        if (ui.peers.isEmpty()) {
                            item { ScanningIndicator() }
                        } else {
                            items(ui.peers, key = { it.id }) { peer ->
                                PeerCard(
                                    peer = peer,
                                    onClick = { viewModel.onPeerTapped(peer) },
                                    onMessage = { navController.navigate(chatRoute(peer)) },
                                )
                            }
                        }

                        if (sentRecent.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Recent Sends",
                                    subtitle = "Re-queue with one tap",
                                )
                            }
                            item {
                                LazyRow(
                                    modifier = Modifier.heightIn(min = 116.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(sentRecent, key = { "${it.id}-${it.timestampEpochMs}" }) { entry ->
                                        RecentSendChip(
                                            entry = entry,
                                            onResend = { viewModel.resendToQueue(entry) },
                                        )
                                    }
                                }
                            }
                        }

                    } else {
                        item {
                            AnimatedItem(index = 0) {
                                ReceiveModeCard(
                                    onShowQr = { qrOpen = true },
                                    onScanQr = { navController.navigate("qr_scan") },
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Transfer History",
                            subtitle = "Open received files or resend",
                        )
                    }

                    items(recent, key = { "${it.id}-${it.fileName}-${it.timestampEpochMs}" }) { entry ->
                        RecentTransferRow(entry = entry, onOpen = { viewModel.openTransferEntry(entry) })
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated list item wrapper — staggered entrance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L + 100L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = tween(400, easing = EaseOutBack),
            initialOffsetY = { it / 6 },
        ),
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated mesh background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeAmbientBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(brush = Brush.radialGradient(listOf(primary, Color.Transparent), Offset(w * 0.2f + shift * w * 0.3f, h * 0.15f), w * 0.7f))
        drawRect(brush = Brush.radialGradient(listOf(tertiary, Color.Transparent), Offset(w * 0.85f - shift * w * 0.25f, h * 0.55f), w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(secondary, Color.Transparent), Offset(w * 0.5f, h * 0.9f + shift * h * 0.05f), w * 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — branded
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTopBar(
    amoledBlack: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    onThemeCycle: () -> Unit,
    onAccentCycle: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.WifiTethering, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("ShareFast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("Local network sharing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        actions = {
            IconButton(onClick = onAccentCycle) { Icon(Icons.Outlined.ColorLens, "Accent") }
            IconButton(onClick = onThemeCycle) { Icon(Icons.Outlined.Palette, "Theme") }
            IconButton(onClick = { onAmoledChange(!amoledBlack) }) {
                Icon(if (amoledBlack) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Background")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero greeting card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeGreetingCard(greetingName: String, deviceName: String, onEdit: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greet, emoji) = when (hour) {
        in 5..11 -> "Good morning" to "☀️"
        in 12..16 -> "Good afternoon" to "🌤"
        in 17..21 -> "Good evening" to "🌙"
        else -> "Hello" to "✨"
    }
    val who = greetingName.trim().ifBlank {
        deviceName.split(" ").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "there"
    }
    val t = rememberInfiniteTransition(label = "float")
    val floatY by t.animateFloat(
        0f, -7f,
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
        // Floating emoji
        Text(
            "📡",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = floatY.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 60.dp)) {
            Text("$greet $emoji", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(who, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 40.sp)
            Text("Ready to share. Add files below, pick a device or scan QR. 🚀", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp),
        ) {
            Icon(Icons.Outlined.Edit, "Edit name", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pill tab row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernTabRow(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Send" to Icons.Outlined.CloudUpload, "Receive" to Icons.Outlined.CloudDownload)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusPill))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            .padding(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, (label, icon) ->
                val isSelected = selected == i
                val bg by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tween(280), label = "tab_bg",
                )
                val fg by animateColorAsState(
                    if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(280), label = "tab_fg",
                )
                val scale by animateFloatAsState(
                    if (isSelected) 1f else 0.94f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_s",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                        Text(label, color = fg, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Device info card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceInfoCard(
    deviceName: String,
    ip: String,
    connection: String,
    discoveryOn: Boolean,
    queueCount: Int,
    wifiBars: Int,
    onRename: () -> Unit,
    onShowQr: () -> Unit,
) {
    val glass = if (MaterialTheme.colorScheme.background.surfaceLuminance() > 0.5f) GlassLight else GlassDark
    Card(
        shape = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(containerColor = glass),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("This Device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRename) { Icon(Icons.Outlined.Edit, "Rename", modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip("IP", ip, Modifier.weight(1f))
                InfoChip("Network", connection, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseDot(active = discoveryOn)
                Text(
                    if (discoveryOn) "Discovering & advertising" else "Idle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (connection.contains("Wi", ignoreCase = true)) {
                    WifiBarsIndicator(wifiBars)
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onShowQr,
                    shape = RoundedCornerShape(RadiusChip),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR", style = MaterialTheme.typography.labelMedium)
                }
            }
            AnimatedVisibility(
                visible = queueCount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Inventory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("$queueCount files ready to send", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient action cards — breathing animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradientActionCard(
    label: String,
    subtitle: String,
    gradient: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val breathe by rememberInfiniteTransition(label = "btn").animateFloat(
        1f, 1.028f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe",
    )
    Card(
        modifier = modifier.height(130.dp).scale(breathe).clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(gradient, Offset.Zero, Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                .fillMaxSize()
                .padding(18.dp),
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(60.dp).align(Alignment.TopEnd).offset(8.dp, (-8).dp))
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Peer card — animated signal bars
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PeerCard(peer: PeerDevice, onClick: () -> Unit, onMessage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().pressScale(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${peer.hostAddress}:${peer.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedSignalBars()
            IconButton(onClick = onMessage) {
                Icon(Icons.AutoMirrored.Outlined.Chat, "Chat", tint = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnimatedSignalBars() {
    val t = rememberInfiniteTransition(label = "signal")
    val alpha by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "sig_a")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(18.dp)) {
        listOf(0.4f, 0.65f, 0.85f, 1f).forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (i == 3) alpha else 1f)),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scanning ripple animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanningIndicator() {
    val t = rememberInfiniteTransition(label = "scan")
    val r1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "r1")
    val r2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, delayMillis = 600), RepeatMode.Restart), label = "r2")
    val color = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(90.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(color.copy(alpha = (1f - r1) * 0.25f), size.minDimension / 2 * r1, c)
            drawCircle(color.copy(alpha = (1f - r2) * 0.25f), size.minDimension / 2 * r2, c)
            drawCircle(color.copy(alpha = 0.15f), size.minDimension / 5, c)
        }
        Icon(Icons.Outlined.WifiTethering, null, tint = color, modifier = Modifier.size(30.dp))
        Text(
            "Scanning… same Wi-Fi or hotspot",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Last peer quick card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LastPeerCard(peer: SavedPeer, onReconnect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Last connected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(peer.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${peer.hostAddress}:${peer.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(onClick = onReconnect, shape = RoundedCornerShape(RadiusChip)) {
                Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reconnect")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Receive mode card — floating icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReceiveModeCard(onShowQr: () -> Unit, onScanQr: () -> Unit) {
    val t = rememberInfiniteTransition(label = "recv")
    val floatY by t.animateFloat(0f, -9f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "fy")
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))))
            .padding(24.dp),
    ) {
        Icon(Icons.Outlined.CloudDownload, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(110.dp).align(Alignment.CenterEnd).offset(20.dp, floatY.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text("Receive Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Listening on port ${ShareConstants.TCP_PORT}. Senders on the same network can connect while the app is open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onShowQr, shape = RoundedCornerShape(RadiusChip)) {
                    Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("My QR")
                }
                OutlinedButton(onClick = onScanQr, shape = RoundedCornerShape(RadiusChip)) {
                    Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent send chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentSendChip(
    entry: com.sharefast.domain.model.TransferHistoryEntry,
    onResend: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.width(140.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(entry.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            FilledTonalButton(
                onClick = onResend,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusChip),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Queue", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent transfer row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTransferRow(entry: com.sharefast.domain.model.TransferHistoryEntry, onOpen: () -> Unit) {
    val canOpen = !entry.storageUri.isNullOrBlank()
    val isSent = entry.direction == TransferDirection.SENT
    Card(
        modifier = Modifier.fillMaxWidth().then(if (canOpen) Modifier.pressScale(onClick = onOpen) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isSent) Icons.Outlined.Upload else Icons.Outlined.Download,
                    null,
                    tint = if (isSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${if (isSent) "Sent to" else "Received from"} ${entry.peerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canOpen) Text("Not available on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            if (canOpen) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transfer sheet content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransferSheet(
    fracAnimated: Float,
    fileName: String,
    fileIndex: Int,
    totalFiles: Int,
    mbps: Double,
    etaText: String,
    isComplete: Boolean,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Transferring", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fracAnimated },
                modifier = Modifier.size(104.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(fracAnimated * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${"%.1f".format(mbps)} MB/s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("File $fileIndex of $totalFiles · $etaText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { fracAnimated }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
        AnimatedVisibility(isComplete, enter = fadeIn() + scaleIn(initialScale = 0.8f)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                Text("All files transferred successfully", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (isPaused) Text("Paused", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPause, shape = RoundedCornerShape(RadiusChip), modifier = Modifier.weight(1f)) { Text("Pause") }
            OutlinedButton(onClick = onResume, shape = RoundedCornerShape(RadiusChip), modifier = Modifier.weight(1f)) { Text("Resume") }
            Button(onClick = onCancel, shape = RoundedCornerShape(RadiusChip), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Cancel", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernAlertDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(RadiusCard),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { FilledTonalButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun PulseDot(active: Boolean) {
    val t = rememberInfiniteTransition(label = "dot")
    val scale by t.animateFloat(0.85f, if (active) 1.2f else 1f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "dot_s")
    Box(Modifier.size(10.dp).scale(scale).clip(CircleShape).background(if (active) Color(0xFF22C55E) else Color(0xFF9CA3AF)))
}

@Composable
private fun WifiBarsIndicator(bars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(14.dp)) {
        listOf(0.5f, 0.7f, 0.85f, 1f).forEachIndexed { i, h ->
            Box(
                Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp))
                    .background(if (i < bars.coerceIn(0, 4)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            )
        }
    }
}

private fun Color.surfaceLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

// ─────────────────────────────────────────────────────────────────────────────
// NOTE: Ensure material-icons-extended is in your dependencies for:
//   Icons.Outlined.CloudUpload, CloudDownload, QrCode, QrCodeScanner,
//   PhoneAndroid, History, Inventory, InsertDriveFile, Upload, Download
// ─────────────────────────────────────────────────────────────────────────────