package com.sharefast.presentation.qr

import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.sharefast.core.ShareConstants
import com.sharefast.utils.NetworkUtils
import com.sharefast.utils.QrUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHubScreen(
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) } // 0 scanner default
    val ip = NetworkUtils.localIpv4Address(androidx.compose.ui.platform.LocalContext.current)
    val payload = remember(ip) { ip?.let { QrUtils.buildConnectPayload(it, ShareConstants.TCP_PORT) } }
    val bmp = remember(payload) { payload?.let { QrUtils.encodeQrBitmap(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Connect") },
                actions = { TextButton(onClick = onClose) { Text("Close") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Scanner") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My QR") })
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { if (forward) it / 8 else -it / 8 })
                        .togetherWith(
                            fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { if (forward) -it / 10 else it / 10 },
                        )
                },
                label = "qrTabs",
            ) { t ->
                if (t == 0) {
                    QrScanScreen(onClose = onClose, showTopBar = false)
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Let another device scan this QR", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.padding(top = 8.dp))
                        Card(shape = RoundedCornerShape(20.dp)) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "My QR",
                                    modifier = Modifier.size(260.dp).clip(RoundedCornerShape(18.dp)),
                                )
                            } else {
                                Text("No local IP found", modifier = Modifier.padding(24.dp))
                            }
                        }
                        if (payload != null) {
                            Text(
                                payload,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

