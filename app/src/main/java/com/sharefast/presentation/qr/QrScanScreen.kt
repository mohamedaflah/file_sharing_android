package com.sharefast.presentation.qr

import android.Manifest
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sharefast.utils.QrUtils
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard  = 28.dp
private val RadiusChip  = 20.dp
private val ScanBoxSize = 260.dp
private val CornerLen   = 36.dp
private val CornerThick = 4.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    onClose: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: QrScanViewModel = hiltViewModel(),
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val handled          = remember { AtomicBoolean(false) }
    val context          = LocalContext.current
    val lifecycleOwner   = LocalLifecycleOwner.current
    val msg   by viewModel.message.collectAsState()
    val phase by viewModel.phase.collectAsState()

    // Camera preview shrinks & fades while connecting
    val scale by animateFloatAsState(
        targetValue   = if (phase is QrScanPhase.Connecting) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "qr_scale",
    )
    val previewAlpha by animateFloatAsState(
        targetValue   = if (phase is QrScanPhase.Connecting) 0.35f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "qr_alpha",
    )

    LaunchedEffect(Unit) { viewModel.closeUi.collect { onClose() } }
    LaunchedEffect(msg)  { if (msg != null) handled.set(false) }
    DisposableEffect(context) {
        onDispose { runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() } }
    }

    // ── Error dialog ─────────────────────────────────────────────────────────
    if (msg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            shape   = RoundedCornerShape(RadiusCard),
            icon    = {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            title   = {
                Text("Invalid QR Code", fontWeight = FontWeight.Bold)
            },
            text    = { Text(msg ?: "") },
            confirmButton = {
                FilledTonalButton(
                    onClick = { viewModel.clearMessage() },
                    shape   = RoundedCornerShape(RadiusChip),
                ) { Text("Try again") }
            },
        )
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (showTopBar) {
                ScanTopBar(onClose = onClose)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!cameraPermission.status.isGranted) {
                // ── Permission gate ──────────────────────────────────────────
                CameraPermissionGate(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                )
            } else {
                // ── Camera preview ───────────────────────────────────────────
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha  = previewAlpha
                        },
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView        = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener(
                                {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val scanner  = BarcodeScanning.getClient(
                                        BarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .build(),
                                    )
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        if (handled.get()) { imageProxy.close(); return@setAnalyzer }
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees,
                                            )
                                            scanner.process(image)
                                                .addOnSuccessListener { codes ->
                                                    val raw = codes.firstOrNull()?.rawValue
                                                    if (raw != null && handled.compareAndSet(false, true)) {
                                                        val parsed = QrUtils.parseConnectPayload(raw)
                                                        if (parsed != null) {
                                                            val (h, p) = parsed
                                                            viewModel.onQrDecoded(h, p)
                                                        } else {
                                                            handled.set(false)
                                                            ContextCompat.getMainExecutor(ctx).execute {
                                                                viewModel.notifyInvalidQr()
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis,
                                        )
                                    } catch (_: Exception) {}
                                },
                                ContextCompat.getMainExecutor(ctx),
                            )
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // ── Scan overlay (corners + laser + hint) ────────────────────
                ScanOverlay(isConnecting = phase is QrScanPhase.Connecting)
            }

            // ── Connecting overlay ───────────────────────────────────────────
            AnimatedVisibility(
                visible  = phase is QrScanPhase.Connecting,
                enter    = fadeIn(tween(300)) + scaleIn(
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                ),
                exit     = fadeOut(tween(200)) + scaleOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                ConnectingCard()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanTopBar(onClose: () -> Unit) {
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
                                listOf(Color(0xFF6366F1), Color(0xFF06B6D4)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Scan QR",
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        "Point at a ShareFast QR code",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        },
        navigationIcon = {},
        actions = {
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        "Close",
                        tint     = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.55f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Scan overlay — corners, laser line, dim surround, hint pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanOverlay(isConnecting: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")

    // Laser sweep
    val laserY by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "laser_y",
    )

    // Corner glow pulse
    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.7f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "corner_a",
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Dark surround drawn via Canvas
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val boxPx   = ScanBoxSize.toPx()
            val cx      = size.width  / 2f
            val cy      = size.height / 2f
            val left    = cx - boxPx / 2f
            val top_    = cy - boxPx / 2f
            val right   = cx + boxPx / 2f
            val bottom_ = cy + boxPx / 2f

            // Four dark rectangles around the scan box
            drawRect(Color.Black.copy(alpha = 0.65f), Offset(0f, 0f), Size(size.width, top_))
            drawRect(Color.Black.copy(alpha = 0.65f), Offset(0f, bottom_), Size(size.width, size.height - bottom_))
            drawRect(Color.Black.copy(alpha = 0.65f), Offset(0f, top_), Size(left, boxPx))
            drawRect(Color.Black.copy(alpha = 0.65f), Offset(right, top_), Size(size.width - right, boxPx))

            // Rounded rect outline (subtle white)
            drawRoundRect(
                color       = Color.White.copy(alpha = 0.12f),
                topLeft     = Offset(left, top_),
                size        = Size(boxPx, boxPx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style       = Stroke(width = 1.5f.dp.toPx()),
            )

            // Laser line
            if (!isConnecting) {
                val laserTop = top_ + laserY * boxPx
                drawRect(
                    brush   = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF6366F1).copy(alpha = 0.9f),
                            Color(0xFF06B6D4).copy(alpha = 0.9f),
                            Color.Transparent,
                        ),
                        startX = left,
                        endX   = right,
                    ),
                    topLeft = Offset(left, laserTop - 1.dp.toPx()),
                    size    = Size(boxPx, 2.5f.dp.toPx()),
                )
            }
        }

        // Corner brackets drawn as a Composable (so alpha animates smoothly)
        ScanCornerBrackets(
            size         = ScanBoxSize,
            cornerLen    = CornerLen,
            cornerThick  = CornerThick,
            cornerAlpha  = cornerAlpha,
        )

        // Hint pill at bottom of scan box
        Box(
            Modifier
                .offset(y = ScanBoxSize / 2 + 24.dp)
                .clip(RoundedCornerShape(RadiusChip))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(RadiusChip),
                )
                .padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.WifiTethering,
                    null,
                    tint     = Color(0xFF06B6D4),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Point at a ShareFast QR code",
                    style     = MaterialTheme.typography.labelMedium,
                    color     = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated corner brackets
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanCornerBrackets(
    size: Dp,
    cornerLen: Dp,
    cornerThick: Dp,
    cornerAlpha: Float,
) {
    val primary  = Color(0xFF6366F1)
    val secondary = Color(0xFF06B6D4)
    androidx.compose.foundation.Canvas(
        Modifier
            .size(size)
            .alpha(cornerAlpha),
    ) {
        val s  = this.size
        val cl = cornerLen.toPx()
        val ct = cornerThick.toPx()
        val r  = 10.dp.toPx()

        // Helper: draw one L-shaped corner
        fun drawCorner(ox: Float, oy: Float, dx: Float, dy: Float) {
            drawLine(primary, Offset(ox + dx * r, oy), Offset(ox + dx * cl, oy), ct, StrokeCap.Round)
            drawLine(secondary, Offset(ox, oy + dy * r), Offset(ox, oy + dy * cl), ct, StrokeCap.Round)
        }

        drawCorner(0f,    0f,    1f,  1f)   // top-left
        drawCorner(s.width, 0f,  -1f,  1f)  // top-right
        drawCorner(0f,    s.height, 1f, -1f)  // bottom-left
        drawCorner(s.width, s.height, -1f, -1f) // bottom-right
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Connecting card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectingCard() {
    // Gradient ring behind spinner
    val t         = rememberInfiniteTransition(label = "spin_ring")
    val ringRot by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ring_rot",
    )

    Card(
        shape  = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Spinner with decorative glow ring
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                androidx.compose.foundation.Canvas(Modifier.size(80.dp).rotate(ringRot)) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF06B6D4), Color.Transparent),
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter  = false,
                        style      = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                CircularProgressIndicator(
                    modifier     = Modifier.size(52.dp),
                    strokeWidth  = 4.dp,
                    color        = MaterialTheme.colorScheme.primary,
                    trackColor   = MaterialTheme.colorScheme.surfaceVariant,
                )
                // Icon in centre
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.WifiTethering,
                        null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Connecting…",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Establishing transfer session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Animated dots indicator
            ConnectingDots()
        }
    }
}

@Composable
private fun ConnectingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0, 200, 400).forEach { delayMs ->
            val alpha by t.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(
                    tween(600, delayMillis = delayMs, easing = EaseInOutSine),
                    RepeatMode.Reverse,
                ),
                label = "dot_$delayMs",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera permission gate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionGate(onRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient background
        QrScanAmbientBackground(Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(40.dp),
        ) {
            // Icon
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF06B6D4))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            Text(
                "Camera access needed",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "ShareFast needs the camera to scan QR codes and connect to nearby devices instantly.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick   = onRequest,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape     = RoundedCornerShape(RadiusChip),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allow Camera", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background (reused from HomeScreen pattern)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrScanAmbientBackground(modifier: Modifier = Modifier) {
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