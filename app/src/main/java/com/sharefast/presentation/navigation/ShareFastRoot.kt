package com.sharefast.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharefast.presentation.TransferNavExtras
import com.sharefast.presentation.home.HomeScreen
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.presentation.permissions.LocalPermissionsFlowCompleted
import com.sharefast.presentation.queue.QueueHudOverlay
import com.sharefast.presentation.qr.QrScanScreen
import com.sharefast.presentation.tabs.AppsScreen
import com.sharefast.presentation.tabs.DocumentsScreen
import com.sharefast.presentation.tabs.MediaKind
import com.sharefast.presentation.tabs.MediaPickScreen
import com.sharefast.utils.AppPermissions
import com.sharefast.utils.hasMediaReadPermission

private data class TabSpec(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    TabSpec("home", "Home", Icons.Outlined.Home),
    TabSpec("apps", "Apps", Icons.Outlined.Apps),
    TabSpec("images", "Images", Icons.Outlined.Image),
    TabSpec("videos", "Videos", Icons.Outlined.VideoLibrary),
    TabSpec("documents", "Docs", Icons.Outlined.Description),
)

private val enter = fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 14 }
private val exit = fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 14 }

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
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = current in tabs.map { it.route }

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

    CompositionLocalProvider(
        LocalPermissionsFlowCompleted provides corePermissionPromptIssued,
        LocalMediaReadGranted provides context.hasMediaReadPermission(),
    ) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottom,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(180)),
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 3.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = current == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
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
            }
            QueueHudOverlay(navController = navController)
            }
        }
    }
}
