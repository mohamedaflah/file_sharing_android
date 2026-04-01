package com.sharefast.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharefast.data.repository.ChatMessage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — match HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val RadiusCard    = 28.dp
private val RadiusChip    = 20.dp
private val RadiusPill    = 50.dp
private val RadiusBubble  = 18.dp
private val RadiusBubbleTip = 4.dp

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val list     by viewModel.messages.collectAsState()
    val error    by viewModel.error.collectAsState()
    val listState = rememberLazyListState()
    var input    by remember { mutableStateOf("") }
    val focus     = remember { FocusRequester() }
    val keyboard  = LocalSoftwareKeyboardController.current

    // Scroll to latest message
    LaunchedEffect(list.size) {
        if (list.isNotEmpty()) listState.animateScrollToItem(list.lastIndex)
    }
    LaunchedEffect(Unit) {
        delay(200)
        focus.requestFocus()
        keyboard?.show()
    }

    // Entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        contentVisible = true
    }

    // Error dialog
    if (error != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            shape   = RoundedCornerShape(RadiusCard),
            icon    = {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            title   = { Text("Message", fontWeight = FontWeight.Bold) },
            text    = { Text(error ?: "") },
            confirmButton = {
                FilledTonalButton(
                    onClick = viewModel::clearError,
                    shape   = RoundedCornerShape(RadiusChip),
                ) { Text("OK") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatTopBar(peerName = viewModel.peerName, onBack = onBack) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // Ambient background
            ChatAmbientBackground(Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = contentVisible,
                enter   = fadeIn(tween(400)),
            ) {
                Column(Modifier.fillMaxSize()) {

                    // ── Message list ─────────────────────────────────────────
                    Box(Modifier.weight(1f)) {
                        if (list.isEmpty()) {
                            ChatEmptyState(
                                peerName = viewModel.peerName,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                state               = listState,
                                modifier            = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding      = PaddingValues(vertical = 12.dp),
                            ) {
                                itemsIndexed(list, key = { _, msg -> msg.id }) { index, msg ->
                                    // Show date separator when day changes
                                    val showDate = index == 0 ||
                                        !isSameDay(list[index - 1].timestampEpochMs, msg.timestampEpochMs)
                                    if (showDate) {
                                        DateSeparator(ms = msg.timestampEpochMs)
                                    }
                                    ChatBubble(
                                        msg   = msg,
                                        index = index,
                                    )
                                }
                            }
                        }
                    }

                    // ── Input bar ────────────────────────────────────────────
                    ChatInputBar(
                        input    = input,
                        onChange = { input = it.take(1000) },
                        onSend   = {
                            if (input.isNotBlank()) {
                                viewModel.send(input)
                                input = ""
                            }
                        },
                        focus    = focus,
                    )
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
private fun ChatTopBar(peerName: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Avatar
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
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
                    Text(
                        peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
                Column {
                    Text(
                        peerName,
                        style         = MaterialTheme.typography.titleLarge,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        maxLines      = 1,
                    )
                    // Online status row
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PulseDot(active = true)
                        Text(
                            "Connected · LAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        "Back",
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
// Chat bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage, index: Int) {
    val isOut = msg.isOutgoing

    // Staggered entrance — only for messages visible at launch (first 20)
    var visible by remember { mutableStateOf(index > 20) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay((index % 20) * 30L + 40L)
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(280)) + slideInHorizontally(
            tween(280, easing = EaseOutCubic),
            initialOffsetX = { if (isOut) it / 5 else -it / 5 },
        ),
    ) {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = if (isOut) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth(0.78f),
                horizontalAlignment = if (isOut) Alignment.End else Alignment.Start,
            ) {
                Box(
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart    = RadiusBubble,
                                topEnd      = RadiusBubble,
                                bottomEnd   = if (isOut) RadiusBubbleTip else RadiusBubble,
                                bottomStart = if (isOut) RadiusBubble else RadiusBubbleTip,
                            ),
                        )
                        .background(
                            if (isOut)
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                                    ),
                                )
                            else
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                ),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            msg.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOut) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestampEpochMs)),
                                style  = MaterialTheme.typography.labelSmall,
                                color  = if (isOut) Color.White.copy(alpha = 0.75f)
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Double tick for outgoing
                            if (isOut) {
                                Icon(
                                    Icons.Outlined.DoneAll,
                                    null,
                                    tint     = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date separator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DateSeparator(ms: Long) {
    val label = remember(ms) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(ms))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(RadiusChip))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Text(
                label,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    input: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    focus: FocusRequester,
) {
    val canSend = input.isNotBlank()
    val sendScale by animateFloatAsState(
        targetValue   = if (canSend) 1f else 0.85f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "send_scale",
    )

    Surface(
        color         = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = onChange,
                modifier      = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                placeholder   = {
                    Text(
                        "Message…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                maxLines      = 5,
                shape         = RoundedCornerShape(24.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                ),
            )

            // Send button
            Box(
                Modifier
                    .size(48.dp)
                    .scale(sendScale)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            )
                        else
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick  = onSend,
                    enabled  = canSend,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        "Send",
                        tint     = if (canSend) Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState(peerName: String, modifier: Modifier = Modifier) {
    val t  = rememberInfiniteTransition(label = "empty")
    val fy by t.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fy",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(40.dp),
        ) {
            // Avatar / icon
            Box(
                Modifier
                    .size(72.dp)
                    .offset(y = fy.dp)
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
                    peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                peerName,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Say hello! Messages stay on your local network — nothing goes to the cloud.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // Decorative "encrypted on LAN" badge
            Row(
                Modifier
                    .clip(RoundedCornerShape(RadiusChip))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
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
private fun ChatAmbientBackground(modifier: Modifier = Modifier) {
    val t     = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
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
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(ms1)) == fmt.format(Date(ms2))
}