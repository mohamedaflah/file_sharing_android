package com.sharefast.presentation.qr

import android.Manifest
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    onClose: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: QrScanViewModel = hiltViewModel(),
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val handled = remember { AtomicBoolean(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val msg by viewModel.message.collectAsState()
    val phase by viewModel.phase.collectAsState()

    val scale by animateFloatAsState(
        targetValue = if (phase is QrScanPhase.Connecting) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "qr_scale",
    )
    val previewAlpha by animateFloatAsState(
        targetValue = if (phase is QrScanPhase.Connecting) 0.45f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "qr_alpha",
    )

    LaunchedEffect(Unit) {
        viewModel.closeUi.collect {
            onClose()
        }
    }

    LaunchedEffect(msg) {
        if (msg != null) handled.set(false)
    }

    DisposableEffect(context) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    if (msg != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearMessage()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMessage()
                    },
                ) { Text("OK") }
            },
            title = { Text("ShareFast") },
            text = { Text(msg ?: "") },
        )
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Scan QR") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!cameraPermission.status.isGranted) {
                Button(
                    onClick = { cameraPermission.launchPermissionRequest() },
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text("Allow camera")
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = previewAlpha
                        },
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener(
                                {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val options = BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .build()
                                    val scanner = BarcodeScanning.getClient(options)
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        if (handled.get()) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
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
                                    } catch (_: Exception) {
                                    }
                                },
                                ContextCompat.getMainExecutor(ctx),
                            )
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (phase is QrScanPhase.Connecting) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(24.dp)
                            } else {
                                Modifier
                            },
                        )
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                )
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Connecting…",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Starting transfer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
