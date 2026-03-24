package com.sharefast.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sharefast.presentation.TransferNavExtras
import com.sharefast.presentation.permissions.LocalPermissionsFlowCompleted
import com.sharefast.core.ShareConstants
import com.sharefast.data.repository.SavedPeer
import com.sharefast.domain.model.PeerDevice
import com.sharefast.domain.model.TransferDirection
import com.sharefast.presentation.theme.GlassDark
import com.sharefast.presentation.theme.GlassLight
import com.sharefast.utils.QrUtils
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import java.util.Calendar

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
    var homeTab by rememberSaveable { mutableIntStateOf(0) }
    val corePermissionsReady = LocalPermissionsFlowCompleted.current
    val listState = rememberLazyListState()

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

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(ui.deviceName) }
    var qrOpen by remember { mutableStateOf(false) }
    var transferSheetDismissed by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(progress) {
        if (progress != null) transferSheetDismissed = false
    }

    if (ui.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("OK") }
            },
            title = { Text("Notice") },
            text = { Text(ui.error ?: "") },
        )
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Device name") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameDevice(renameText)
                        renameOpen = false
                    },
                ) { Text("Save") }
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
        Dialog(onDismissRequest = { qrOpen = false }) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan to connect", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "QR",
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(payload, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { qrOpen = false }) { Text("Close") }
                }
            }
        }
    }

    if (progress != null && !transferSheetDismissed) {
        val p = progress!!
        val fracRaw = if (p.totalBytes > 0) {
            (p.bytesTransferred.toFloat() / p.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val fracAnimated by animateFloatAsState(
            targetValue = fracRaw,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "xfer",
        )
        val remaining = (p.totalBytes - p.bytesTransferred).coerceAtLeast(0L)
        val etaText = if (p.speedBytesPerSecond > 1024.0 && remaining > 0) {
            val sec = (remaining / p.speedBytesPerSecond).toLong().coerceAtLeast(1L)
            when {
                sec < 60 -> "~${sec}s left"
                sec < 3600 -> "~${sec / 60}m left"
                else -> "~${sec / 3600}h left"
            }
        } else {
            "Calculating…"
        }
        ModalBottomSheet(
            onDismissRequest = { transferSheetDismissed = true },
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Transfer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    p.currentFileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "File ${p.currentFileIndex} of ${p.totalFiles}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { fracAnimated }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                val mbps = p.speedBytesPerSecond / (1024.0 * 1024.0)
                Text(
                    "${"%.2f".format(mbps)} MB/s · $etaText",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (p.isPaused) {
                    Text("Paused", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { viewModel.pauseTransfer() }) { Text("Pause") }
                    TextButton(onClick = { viewModel.resumeTransfer() }) { Text("Resume") }
                    TextButton(onClick = { viewModel.cancelTransfer() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ShareFast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Share on your network",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onThemeCycle) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Theme")
                    }
                    IconButton(onClick = { onAmoledChange(!amoledBlack) }) {
                        Icon(
                            if (amoledBlack) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "AMOLED",
                        )
                    }
                    IconButton(onClick = { navController.navigate("qr_scan") }) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = "Scan QR")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
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
            HomeAmbientGradient(Modifier.fillMaxSize())
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { HomeGreetingLine() }
                item {
                    TabRow(selectedTabIndex = homeTab) {
                        Tab(
                            selected = homeTab == 0,
                            onClick = { homeTab = 0 },
                            text = { Text("Send") },
                        )
                        Tab(
                            selected = homeTab == 1,
                            onClick = { homeTab = 1 },
                            text = { Text("Receive") },
                        )
                    }
                }
                if (homeTab == 0) {
                    item {
                        if (lastPeer != null) {
                            LastPeerQuickCard(
                                peer = lastPeer!!,
                                onReconnect = { viewModel.reconnectLastPeer() },
                            )
                        }
                    }
                    item {
                        GlassInfoCard(
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
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PulseActionButton(
                                label = "Queue",
                                subtitle = if (ui.queueCount > 0) "${ui.queueCount} ready" else "Add from tabs",
                                gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.weight(1f),
                                onClick = { homeTab = 0 },
                            )
                            PulseActionButton(
                                label = "Receive",
                                subtitle = "Port ${ShareConstants.TCP_PORT}",
                                gradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f),
                                onClick = { homeTab = 1 },
                            )
                        }
                    }
                    item {
                        SectionHeader(
                            title = "Nearby devices",
                            subtitle = "Tap a device — sends everything in your queue",
                        )
                    }
                    if (ui.peers.isEmpty()) {
                        item {
                            Text(
                                "Searching… same Wi‑Fi or hotspot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(ui.peers, key = { it.id }) { peer ->
                            PeerRow(peer = peer, onClick = { viewModel.onPeerTapped(peer) })
                        }
                    }
                    if (sentRecent.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Recent sends",
                                subtitle = "Queue again with one tap",
                            )
                        }
                        item {
                            LazyRow(
                                modifier = Modifier.heightIn(min = 108.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        ReceiveModeCard(
                            onShowQr = { qrOpen = true },
                            onScanQr = { navController.navigate("qr_scan") },
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Recent transfers",
                        subtitle = "Open received files or resend from Send tab",
                    )
                }
                items(recent, key = { "${it.id}-${it.fileName}-${it.timestampEpochMs}" }) { entry ->
                    RecentRow(entry = entry, onOpen = { viewModel.openTransferEntry(entry) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HomeAmbientGradient(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sh",
    )
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(primary, tertiary, Color.Transparent),
                start = Offset(0f, shift * 400f),
                end = Offset(900f, 600f - shift * 200f),
            ),
        ),
    )
}

@Composable
private fun HomeGreetingLine() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$greet — ready to share",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Add files from the bottom tabs, then pick a device or scan QR.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LastPeerQuickCard(peer: SavedPeer, onReconnect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Last device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(peer.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${peer.hostAddress}:${peer.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onReconnect) {
                Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reconnect")
            }
        }
    }
}

@Composable
private fun ReceiveModeCard(onShowQr: () -> Unit, onScanQr: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Receiving", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "This phone listens on port ${ShareConstants.TCP_PORT}. Senders on the same network can connect while the app is open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onShowQr) { Text("Show my QR") }
                TextButton(onClick = onScanQr) { Text("Scan to send") }
            }
        }
    }
}

@Composable
private fun RecentSendChip(
    entry: com.sharefast.domain.model.TransferHistoryEntry,
    onResend: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            Modifier
                .width(132.dp)
                .padding(12.dp),
        ) {
            Text(
                entry.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onResend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Queue")
            }
        }
    }
}

@Composable
private fun GlassInfoCard(
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glass),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("This device", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(deviceName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Rename")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("IP · $ip", style = MaterialTheme.typography.bodyMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Network · $connection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (connection.contains("Wi", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < wifiBars.coerceIn(0, 4)) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = discoveryOn)
                Text(if (discoveryOn) "Discovering & advertising" else "Idle", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onShowQr) { Text("Show QR") }
            }
            if (queueCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Send queue: $queueCount files", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun Color.surfaceLuminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

@Composable
private fun StatusDot(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (active) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "s",
    )
    Box(
        Modifier
            .size(12.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
    )
}

@Composable
private fun PulseActionButton(
    label: String,
    subtitle: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "btn")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "b",
    )
    Card(
        modifier = modifier
            .height(120.dp)
            .scale(breathe)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(gradient))
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun PeerRow(peer: PeerDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(peer.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${peer.hostAddress}:${peer.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RecentRow(entry: com.sharefast.domain.model.TransferHistoryEntry, onOpen: () -> Unit) {
    val canOpen = !entry.storageUri.isNullOrBlank()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpen, onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.fileName,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${if (entry.direction == TransferDirection.SENT) "Sent" else "Received"} · ${entry.peerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canOpen) {
                    Text(
                        "No preview on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (canOpen) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Open file",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
