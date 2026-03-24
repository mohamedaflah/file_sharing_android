package com.sharefast.presentation

/** One-shot navigation from transfer notifications into Home. */
data class TransferNavExtras(
    val scrollToRecent: Boolean = false,
    val openUri: String? = null,
    val openFileNameHint: String? = null,
)
