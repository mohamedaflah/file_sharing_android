package com.sharefast.presentation.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.sharefast.presentation.permissions.LocalMediaReadGranted
import com.sharefast.utils.openAppSettings
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickScreen(
    kind: MediaKind,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val mediaOk = LocalMediaReadGranted.current
    val items by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(kind, mediaOk) {
        if (mediaOk) viewModel.load(kind)
    }

    val title = if (kind == MediaKind.IMAGES) "Photos" else "Videos"
    val subtitle =
        if (kind == MediaKind.IMAGES) "Pick photos to send — tap thumbnails to select."
        else "Video thumbnails load from the first frame — tap to select."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Button(onClick = { viewModel.addSelectedToQueue(kind) }) {
                    Text("Add ${selected.size} to queue")
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !mediaOk -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Photos & videos access is needed to list your gallery.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { context.openAppSettings() }) {
                            Text("Open app settings")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.load(kind) }) {
                            Text("I already allowed — retry")
                        }
                    }
                }
                loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                items.isEmpty() -> {
                    Text(
                        "No items found. Grant access or add media to your device.",
                        Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            tonalElevation = 0.dp,
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${items.size} items",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                        items(items, key = { it.id }) { item ->
                            val isSel = item.id in selected
                            val imageModel = remember(item.uri, kind) {
                                ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .apply {
                                        if (kind == MediaKind.VIDEOS) {
                                            videoFrameMillis(1_000L)
                                        }
                                    }
                                    .crossfade(true)
                                    .build()
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSel) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { viewModel.toggle(item.id) },
                            ) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = item.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                if (kind == MediaKind.VIDEOS && item.durationMs != null) {
                                    Text(
                                        formatDuration(item.durationMs),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%d:%02d".format(m, r)
}
