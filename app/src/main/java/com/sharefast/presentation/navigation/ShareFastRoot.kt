package com.sharefast.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharefast.presentation.TransferNavExtras
import com.sharefast.presentation.chat.chatRoute
import com.sharefast.presentation.home.HomeScreen
import com.sharefast.presentation.chat.ChatScreen
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.presentation.permissions.LocalPermissionsFlowCompleted
import com.sharefast.presentation.queue.QueueHudOverlay
import com.sharefast.presentation.qr.QrScanScreen
import com.sharefast.presentation.qr.QrHubScreen
import com.sharefast.presentation.sensor.ShakeToSendEffect
import com.sharefast.presentation.tabs.AppsScreen
import com.sharefast.presentation.tabs.DocumentsScreen
import com.sharefast.presentation.tabs.MediaKind
import com.sharefast.presentation.tabs.MediaPickScreen
import com.sharefast.utils.OpenUriHelper
import com.sharefast.utils.AppPermissions
import com.sharefast.utils.Feedback
import com.sharefast.utils.hasMediaReadPermission
import kotlinx.coroutines.delay

private data class TabSpec(
    val route: String,
    val label: String,
    val iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    val iconUnselected: androidx.compose.ui.graphics.vector.ImageVector,
)

private val tabs = listOf(
    TabSpec("home", "Home", Icons.Rounded.Bolt, Icons.Rounded.Home),
    TabSpec("apps", "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    TabSpec("images", "Images", Icons.Rounded.Image, Icons.Outlined.Image),
    TabSpec("videos", "Videos", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    TabSpec("documents", "Docs", Icons.Rounded.Description, Icons.Rounded.Smartphone),
)

private val enter = fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 14 } +
    scaleIn(initialScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
private val exit = fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 14 } +
    scaleOut(targetScale = 0.96f, animationSpec = tween(200))

@Composable
fun ShareFastRoot(
    modifier: Modifier = Modifier,
    amoledBlack: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    onThemeCycle: () -> Unit,
    transferNavExtras: TransferNavExtras = TransferNavExtras(),
    onConsumedTransferNav: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val transferToastVm: TransferToastViewModel = hiltViewModel()
    val incomingRequestVm: IncomingTransferRequestViewModel = hiltViewModel()
    val transferToast by transferToastVm.toast.collectAsState()
    val incomingRequest by incomingRequestVm.request.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = current in tabs.map { it.route }
    var receiveDialogOpen by remember { mutableStateOf(false) }

    var corePermissionPromptIssued by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result ignored — LAN stack starts after prompt is shown */ }

    LaunchedEffect(corePermissionPromptIssued) {
        if (!corePermissionPromptIssued) {
            permissionLauncher.launch(AppPermissions.coreLaunchPermissions())
            corePermissionPromptIssued = true
        }
    }
    LaunchedEffect(transferToast?.id) {
        if (transferToast != null) {
            delay(4_200)
            transferToastVm.dismiss()
        }
    }
    LaunchedEffect(incomingRequest?.id) {
        if (incomingRequest != null) {
            Feedback.vibrateSuccess(context)
            Feedback.playQrScannedChime()
            if (incomingRequest?.type == com.sharefast.services.transfer.IncomingRequestType.TEXT) {
                delay(4_500)
                incomingRequestVm.dismiss()
            }
        }
    }

    CompositionLocalProvider(
        LocalPermissionsFlowCompleted provides corePermissionPromptIssued,
        LocalMediaReadGranted provides context.hasMediaReadPermission(),
    ) {
        Scaffold(
            modifier = modifier,
            floatingActionButton = {
                if (showBottom) {
                    FloatingActionButton(
                        onClick = { navController.navigate("qr_hub") { launchSingleTop = true } },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 108.dp, end = 4.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.QrCode2,
                            contentDescription = "QR",
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottom,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(180)),
                ) {
                    GlassBottomTabs(
                        current = current,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            ShakeToSendEffect(
                enabled = showBottom && current != "qr_scan",
                onShake = {
                    navController.navigate("images") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(
                        "home",
                        enterTransition = { enter },
                        exitTransition = { exit },
                    ) {
                        HomeScreen(
                            navController = navController,
                            amoledBlack = amoledBlack,
                            onAmoledChange = onAmoledChange,
                            onThemeCycle = onThemeCycle,
                            transferNavExtras = transferNavExtras,
                            onConsumedTransferNav = onConsumedTransferNav,
                        )
                    }
                    composable("apps", enterTransition = { enter }, exitTransition = { exit }) { AppsScreen() }
                    composable("images", enterTransition = { enter }, exitTransition = { exit }) {
                        MediaPickScreen(MediaKind.IMAGES)
                    }
                    composable("videos", enterTransition = { enter }, exitTransition = { exit }) {
                        MediaPickScreen(MediaKind.VIDEOS)
                    }
                    composable("documents", enterTransition = { enter }, exitTransition = { exit }) {
                        DocumentsScreen()
                    }
                    composable(
                        "qr_scan",
                        enterTransition = { fadeIn(tween(240)) },
                        exitTransition = { fadeOut(tween(200)) },
                    ) {
                        QrScanScreen(onClose = { navController.popBackStack() })
                    }
                    composable("qr_hub", enterTransition = { enter }, exitTransition = { exit }) {
                        QrHubScreen(onClose = { navController.popBackStack() })
                    }
                    composable("chat/{peerKey}/{peerName}/{host}/{port}") {
                        ChatScreen(onBack = { navController.popBackStack() })
                    }
                }
                QueueHudOverlay(navController = navController)
                incomingRequest?.let { req ->
                    if (req.type == com.sharefast.services.transfer.IncomingRequestType.TEXT) {
                        IncomingMessageToast(
                            request = req,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 14.dp, end = 14.dp, bottom = 92.dp),
                            onDismiss = { incomingRequestVm.dismiss() },
                            onClick = {
                                val key = req.peerKey
                                val host = req.peerHost
                                val port = req.peerPort
                                if (!key.isNullOrBlank() && !host.isNullOrBlank() && port != null) {
                                    navController.navigate(
                                        chatRoute(
                                            peerKey = key,
                                            peerName = req.deviceName,
                                            host = host,
                                            port = port,
                                        ),
                                    )
                                }
                                incomingRequestVm.dismiss()
                            },
                        )
                    } else {
                        IncomingTransferRequestCard(
                            request = req,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 14.dp, end = 14.dp, bottom = 92.dp),
                            onAccept = { incomingRequestVm.accept(req.id) },
                            onDecline = { incomingRequestVm.decline(req.id) },
                        )
                    }
                }
                transferToast?.let { toast ->
                    TransferSuccessToast(
                        ui = toast,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp, start = 14.dp, end = 14.dp),
                        onDismiss = { transferToastVm.dismiss() },
                        onClick = {
                            if (toast.direction == com.sharefast.domain.model.TransferDirection.RECEIVED) {
                                val openable = toast.files.filter { !it.storageUri.isNullOrBlank() }
                                if (toast.fileCount <= 1 && openable.size == 1) {
                                    val entry = openable.first()
                                    OpenUriHelper.tryOpenWithView(
                                        context = context,
                                        uri = android.net.Uri.parse(entry.storageUri),
                                        fileNameHint = entry.fileName,
                                    )
                                    transferToastVm.dismiss()
                                } else {
                                    receiveDialogOpen = true
                                }
                            } else {
                                transferToastVm.dismiss()
                            }
                        },
                    )
                }
            }
        }
    }
    if (receiveDialogOpen) {
        val items = transferToast?.files?.filter { !it.storageUri.isNullOrBlank() }.orEmpty()
        Dialog(onDismissRequest = { receiveDialogOpen = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Received files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Name, size and quick open for each file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        items.forEach { entry ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            prettyBytes(entry.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            OpenUriHelper.tryOpenWithView(
                                                context = context,
                                                uri = android.net.Uri.parse(entry.storageUri),
                                                fileNameHint = entry.fileName,
                                            )
                                            receiveDialogOpen = false
                                            transferToastVm.dismiss()
                                        },
                                    )
                                    {
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { receiveDialogOpen = false },
                        modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
                    ) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun GlassBottomTabs(
    current: String?,
    onNavigate: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 22.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp),
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                tabs.forEach { tab ->
                    BottomTabItem(tab, current == tab.route) { onNavigate(tab.route) }
                }
            }
        }
    }
}

@Composable
private fun BottomTabItem(tab: TabSpec, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
            contentDescription = tab.label,
            modifier = Modifier.size(if (selected) 24.dp else 22.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IncomingMessageToast(
    request: com.sharefast.services.transfer.IncomingTransferRequest,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Message from ${request.deviceName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    request.message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Swipe to close",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun IncomingTransferRequestCard(
    request: com.sharefast.services.transfer.IncomingTransferRequest,
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    var detailsOpen by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (request.type == com.sharefast.services.transfer.IncomingRequestType.TEXT) {
                    "Incoming message request"
                } else {
                    "Incoming transfer request"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (request.type == com.sharefast.services.transfer.IncomingRequestType.TEXT) {
                    "${request.deviceName} wants to send a message"
                } else {
                    "${request.deviceName} wants to send ${request.files.size} files"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (request.type == com.sharefast.services.transfer.IncomingRequestType.TEXT) {
                Text(
                    request.message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) { Text("Accept") }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                ) { Text("Decline", color = Color(0xFFE53935)) }
                if (request.type == com.sharefast.services.transfer.IncomingRequestType.FILES) {
                    TextButton(onClick = { detailsOpen = true }) { Text("Details") }
                }
            }
        }
    }
    if (detailsOpen) {
        Dialog(onDismissRequest = { detailsOpen = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Transfer details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Sender: ${request.deviceName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    request.files.take(10).forEach { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                f.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                prettyBytes(f.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (request.files.size > 10) {
                        Text(
                            "+ ${request.files.size - 10} more files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { detailsOpen = false },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Close") }
                }
            }
        }
    }
}

private fun prettyBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024 && idx < units.lastIndex) {
        value /= 1024
        idx++
    }
    return "%.1f %s".format(value, units[idx])
}

@Composable
private fun TransferSuccessToast(
    ui: TransferToastUi,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val title = if (ui.direction == com.sharefast.domain.model.TransferDirection.RECEIVED) {
        if (ui.fileCount > 1) "${ui.fileCount} files received from ${ui.peerName}" else "File received from ${ui.peerName}"
    } else {
        if (ui.fileCount > 1) "${ui.fileCount} files sent to ${ui.peerName}" else "File sent to ${ui.peerName}"
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + slideInHorizontally(initialOffsetX = { -it / 3 }),
        exit = fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            ),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (ui.direction == com.sharefast.domain.model.TransferDirection.RECEIVED) {
                    FilledTonalButton(onClick = onClick) { Text("Open") }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}
