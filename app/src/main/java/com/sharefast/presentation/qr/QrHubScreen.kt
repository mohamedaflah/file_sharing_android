package com.sharefast.presentation.qr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharefast.core.ShareConstants
import com.sharefast.utils.NetworkUtils
import com.sharefast.utils.QrUtils

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — match HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard = 28.dp
private val RadiusChip = 20.dp
private val RadiusPill = 50.dp
private val PaddingPage = 20.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHubScreen(onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val ip      = remember { NetworkUtils.localIpv4Address(ctx) }
    val payload = remember(ip) { ip?.let { QrUtils.buildConnectPayload(it, ShareConstants.TCP_PORT) } }
    val bmp     = remember(payload) { payload?.let { QrUtils.encodeQrBitmap(it) } }

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        contentVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            QrTopBar(onClose = onClose)
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Ambient background — same as HomeScreen
            QrAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(400)) + slideInVertically(
                    tween(400, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaddingPage),
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Pill tab row (same pattern as HomeScreen) ──────────
                    QrPillTabRow(selected = tab, onSelect = { tab = it })

                    Spacer(Modifier.height(24.dp))

                    // ── Tab content ────────────────────────────────────────
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val fwd = targetState > initialState
                            (fadeIn(tween(260)) + slideInHorizontally(tween(260, easing = EaseOutCubic)) {
                                if (fwd) it / 7 else -it / 7
                            }).togetherWith(
                                fadeOut(tween(200)) + slideOutHorizontally(tween(200)) {
                                    if (fwd) -it / 9 else it / 9
                                },
                            )
                        },
                        label = "qrTabs",
                    ) { t ->
                        if (t == 0) {
                            QrScanScreen(onClose = onClose, showTopBar = false)
                        } else {
                            MyQrContent(payload = payload, bmp = bmp)
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
private fun QrTopBar(onClose: () -> Unit) {
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
                    Icon(Icons.Outlined.QrCode, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "QR Connect",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        "Scan or share your code",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        "Close",
                        tint     = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Pill tab row — identical to HomeScreen pattern
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrPillTabRow(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        "Scan QR"  to Icons.Outlined.QrCodeScanner,
        "My QR"    to Icons.Outlined.QrCode,
    )
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
                    tween(280), label = "tab_bg_$i",
                )
                val fg by animateColorAsState(
                    if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(280), label = "tab_fg_$i",
                )
                val scale by animateFloatAsState(
                    if (isSelected) 1f else 0.94f,
                    spring(Spring.DampingRatioMediumBouncy), label = "tab_s_$i",
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                        Text(
                            label,
                            color      = fg,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// My QR tab content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MyQrContent(
    payload: String?,
    bmp: android.graphics.Bitmap?,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Instruction card ─────────────────────────────────────────────────
        InstructionBanner()

        // ── QR card ──────────────────────────────────────────────────────────
        if (bmp != null && payload != null) {
            QrCodeCard(bmp = bmp, payload = payload)
        } else {
            NoIpCard()
        }
    }
}

@Composable
private fun InstructionBanner() {
    val t  = rememberInfiniteTransition(label = "banner_float")
    val fy by t.animateFloat(
        0f, -5f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
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
            .padding(18.dp),
    ) {
        // Decorative icon
        Icon(
            Icons.Outlined.PhoneAndroid,
            null,
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier
                .size(70.dp)
                .align(Alignment.CenterEnd)
                .offset(10.dp, fy.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Share this QR code",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Let another device on the same Wi-Fi scan it to connect instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QrCodeCard(bmp: android.graphics.Bitmap, payload: String) {
    // Rotating border animation
    val t = rememberInfiniteTransition(label = "qr_border")
    val borderRotation by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "border_rot",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // QR with animated gradient border
        Box(contentAlignment = Alignment.Center) {
            // Outer glow ring
            Box(
                Modifier
                    .size(296.dp)
                    .clip(RoundedCornerShape(RadiusCard + 4.dp))
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                            ),
                            center = Offset(148f, 148f),
                        ),
                    )
                    .rotate(borderRotation),
            )
            // QR card
            Card(
                modifier  = Modifier.size(280.dp),
                shape     = RoundedCornerShape(RadiusCard),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap             = bmp.asImageBitmap(),
                        contentDescription = "My QR code",
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
        }

        // Payload pill
        Box(
            Modifier
                .clip(RoundedCornerShape(RadiusChip))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(RadiusChip),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Wifi,
                    null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    payload,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Status indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            PulseDot(active = true)
            Text(
                "Listening for connections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoIpCard() {
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
                Icons.Outlined.WifiOff,
                null,
                tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp),
            )
            Text(
                "No local network found",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Connect to a Wi-Fi network and try again",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated mesh background — same as HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrAmbientBackground(modifier: Modifier = Modifier) {
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
        drawRect(brush = Brush.radialGradient(listOf(primary,   Color.Transparent), Offset(w * 0.2f + shift * w * 0.3f,  h * 0.15f), w * 0.7f))
        drawRect(brush = Brush.radialGradient(listOf(tertiary,  Color.Transparent), Offset(w * 0.85f - shift * w * 0.25f, h * 0.55f), w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(secondary, Color.Transparent), Offset(w * 0.5f, h * 0.9f + shift * h * 0.05f),   w * 0.5f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseDot(active: Boolean) {
    val t     = rememberInfiniteTransition(label = "dot")
    val scale by t.animateFloat(
        0.85f, if (active) 1.2f else 1f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "dot_s",
    )
    Box(
        Modifier
            .size(10.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
    )
}