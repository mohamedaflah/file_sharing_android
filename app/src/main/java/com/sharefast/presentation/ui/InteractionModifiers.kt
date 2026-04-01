package com.sharefast.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

// ─────────────────────────────────────────────────────────────────────────────
// pressScale — subtle scale-down on press, spring back on release
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Subtle press feedback: scale down while pressed, spring back on release.
 * Optionally adds a slight tilt (rotation) for a more tactile feel.
 *
 * @param enabled       Whether interaction is enabled.
 * @param pressedScale  Scale factor while pressed (default 0.96).
 * @param springStiff   Spring stiffness for the return animation.
 * @param springDamp    Spring damping ratio for the return animation.
 * @param onClick       Click callback.
 */
fun Modifier.pressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    springStiff: Float = Spring.StiffnessMedium,
    springDamp: Float = Spring.DampingRatioMediumBouncy,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (enabled && pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = springStiff, dampingRatio = springDamp),
        label         = "pressScale",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication        = androidx.compose.material3.ripple(),
            enabled           = enabled,
            role              = Role.Button,
            onClick           = onClick,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// pressScaleTilt — pressScale + subtle Z-rotation on press
// Great for cards and action tiles that need extra tactile depth.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Like [pressScale] but adds a slight tilt rotation on the Z axis while
 * pressed, giving cards a "picking up" feel.
 *
 * @param tiltDegrees  Max rotation in degrees while pressed (default 1.2f).
 */
fun Modifier.pressScaleTilt(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    tiltDegrees: Float = 1.2f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (enabled && pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "psTilt_scale",
    )
    val rotation by animateFloatAsState(
        targetValue   = if (enabled && pressed) tiltDegrees else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "psTilt_rot",
    )
    this
        .graphicsLayer {
            scaleX        = scale
            scaleY        = scale
            rotationZ     = rotation
        }
        .clickable(
            interactionSource = interaction,
            indication        = androidx.compose.material3.ripple(),
            enabled           = enabled,
            role              = Role.Button,
            onClick           = onClick,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// pressElevate — scale UP slightly on press (for FAB-like affordances)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Inverse of [pressScale] — scales the composable slightly UP on press.
 * Useful for icon buttons or chips where you want a "lift" feeling.
 *
 * @param liftScale  Scale factor while pressed (default 1.08).
 */
fun Modifier.pressElevate(
    enabled: Boolean = true,
    liftScale: Float = 1.08f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (enabled && pressed) liftScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "pressElevate",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication        = androidx.compose.material3.ripple(),
            enabled           = enabled,
            role              = Role.Button,
            onClick           = onClick,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// pressBounce — spring back with overshoot (playful cards / chips)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A bouncier version of [pressScale] that overshoots slightly on release,
 * giving a playful elastic snap-back. Best on smaller elements like chips.
 */
fun Modifier.pressBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.90f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (enabled && pressed) pressedScale else 1f,
        animationSpec = spring(
            stiffness    = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy,   // more overshoot
        ),
        label = "pressBounce",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication        = androidx.compose.material3.ripple(),
            enabled           = enabled,
            role              = Role.Button,
            onClick           = onClick,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// pressAlpha — dim on press (for image thumbnails / media tiles)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dims the composable's alpha while pressed — ideal for image thumbnails
 * where you can't overlay a tinted ripple easily.
 *
 * @param pressedAlpha  Alpha while pressed (default 0.72f).
 */
fun Modifier.pressAlpha(
    enabled: Boolean = true,
    pressedAlpha: Float = 0.72f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue   = if (enabled && pressed) pressedAlpha else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label         = "pressAlpha",
    )
    this
        .graphicsLayer { this.alpha = alpha }
        .clickable(
            interactionSource = interaction,
            indication        = null,          // no ripple — alpha is the feedback
            enabled           = enabled,
            role              = Role.Button,
            onClick           = onClick,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// noRippleClickable — clickable with zero indication (for custom animations)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A bare clickable that suppresses both ripple and indication.
 * Use this when you're providing your own animated press feedback
 * and don't want the system ripple to compete.
 */
fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = interaction,
        indication        = null,
        enabled           = enabled,
        role              = role,
        onClick           = onClick,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Convenience extension — expose pressed state without click
// Useful for driving custom animations from outside the Modifier chain.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a [MutableInteractionSource] whose pressed state can be observed
 * to drive custom animations. Pair with [noRippleClickable] to attach the
 * same source as the click handler's interaction source.
 */
@Composable
fun rememberPressInteraction(): MutableInteractionSource =
    remember { MutableInteractionSource() }