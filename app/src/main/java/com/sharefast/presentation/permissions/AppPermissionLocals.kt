package com.sharefast.presentation.permissions

import androidx.compose.runtime.compositionLocalOf

/** True after user has responded to the core permission sheet (granted or denied). */
val LocalPermissionsFlowCompleted = compositionLocalOf { false }

/** Media read (photos/videos) granted — required for gallery tabs. */
val LocalMediaReadGranted = compositionLocalOf { false }
