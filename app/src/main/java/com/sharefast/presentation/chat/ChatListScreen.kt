package com.sharefast.presentation.chat

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharefast.data.repository.ChatThread
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard  = 28.dp
private val RadiusChip  = 20.dp
private val PaddingPage = 20.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val threads by viewModel.threads.collectAsState()
    val listState = rememberLazyListState()

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        contentVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatListTopBar(threadCount = threads.size) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Ambient background
            ChatListAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(500)) + slideInVertically(
                    tween(500, easing = EaseOutCubic),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                if (threads.isEmpty()) {
                    ChatListEmptyState()
                } else {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(horizontal = PaddingPage),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }

                        // Hero banner
                        item { ChatListHeroBanner(count = threads.size) }

                        // Thread rows
                        itemsIndexed(threads, key = { _, t -> t.peerKey }) { index, t ->
                            StaggeredThreadRow(
                                thread    = t,
                                index     = index,
                                onClick   = {
                                    val key  = Uri.encode(t.peerKey)
                                    val name = Uri.encode(t.peerName)
                                    val host = Uri.encode(t.peerKey.substringBefore(":"))
                                    val port = t.peerKey.substringAfter(":", "17342")
                                        .toIntOrNull() ?: 17342
                                    navController.navigate("chat/$key/$name/$host/$port")
                                },
                            )
                        }

                        item { Spacer(Modifier.height(32.dp)) }
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
private fun ChatListTopBar(threadCount: Int) {
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
                    Icon(
                        Icons.Outlined.Forum,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Chats",
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    if (threadCount > 0) {
                        Text(
                            "$threadCount conversation${if (threadCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatListHeroBanner(count: Int) {
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
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f),
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        Icon(
            Icons.Outlined.Forum,
            null,
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(10.dp, fy.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier            = Modifier.padding(end = 60.dp),
        ) {
            Text(
                "Recent Chats",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Messages stay on your local network — nothing goes to the cloud.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            // LAN badge
            Row(
                Modifier
                    .clip(RoundedCornerShape(RadiusChip))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PulseDot(active = true)
                Text(
                    "$count active · LAN only",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Thread row — staggered entrance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredThreadRow(
    thread: ChatThread,
    index: Int,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L + 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(300)) + slideInHorizontally(
            tween(300, easing = EaseOutCubic),
            initialOffsetX = { -it / 6 },
        ),
    ) {
        ThreadRow(thread = thread, onClick = onClick)
    }
}

@Composable
private fun ThreadRow(thread: ChatThread, onClick: () -> Unit) {
    val initial = thread.peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    // Unread badge color pulse
    val hasUnread = false
    val badgePulse by rememberInfiniteTransition(label = "badge").animateFloat(
        0.8f, if (hasUnread) 1.1f else 0.8f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "badge_pulse",
    )

    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar with gradient
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initial,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                // Online dot
                Box(
                    Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
            }

            // Text content
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        thread.peerName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f),
                    )
                    // Timestamp
                    if (thread.timestampEpochMs > 0L) {
                        Text(
                            formatThreadTime(thread.timestampEpochMs),
                            style  = MaterialTheme.typography.labelSmall,
                            color  = if (hasUnread)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        thread.lastBody,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        style      = MaterialTheme.typography.bodySmall,
                        color      = if (hasUnread)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                        modifier   = Modifier.weight(1f),
                    )
                    // Unread badge
                    if (hasUnread) {
                        Box(
                            Modifier
                                .scale(badgePulse)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "0",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 10.sp,
                            )
                        }
                    } else {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatListEmptyState() {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .offset(y = fy.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Forum,
                    null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp),
                )
            }
            Text(
                "No conversations yet",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Tap the chat icon on a nearby device card on the Home screen to start messaging over LAN.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // LAN badge
            Row(
                Modifier
                    .clip(RoundedCornerShape(RadiusChip))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Wifi,
                    null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Local network · no cloud",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatListAmbientBackground(modifier: Modifier = Modifier) {
    val t     = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val tertiary  = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(brush = Brush.radialGradient(listOf(primary,   Color.Transparent), Offset(w * 0.15f + shift * w * 0.3f,  h * 0.12f), w * 0.75f))
        drawRect(brush = Brush.radialGradient(listOf(tertiary,  Color.Transparent), Offset(w * 0.88f - shift * w * 0.25f, h * 0.60f), w * 0.65f))
        drawRect(brush = Brush.radialGradient(listOf(secondary, Color.Transparent), Offset(w * 0.5f, h * 0.92f + shift * h * 0.04f),  w * 0.55f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Micro-components
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
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatThreadTime(ms: Long): String {
    val now    = System.currentTimeMillis()
    val diff   = now - ms
    val oneDayMs = 86_400_000L
    return when {
        diff < 60_000L       -> "Just now"
        diff < 3_600_000L    -> "${diff / 60_000}m"
        diff < oneDayMs      -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        diff < oneDayMs * 7  -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(ms))
        else                 -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
    }
}