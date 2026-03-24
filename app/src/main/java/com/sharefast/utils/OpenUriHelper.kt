package com.sharefast.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens [uri] with [Intent.ACTION_VIEW] using several MIME / data strategies.
 * [resolveActivity] is unreliable on API 30+; we prefer trying [startActivity] in order.
 */
object OpenUriHelper {

    fun tryOpenWithView(context: Context, uri: Uri, fileNameHint: String): Boolean {
        val cr = context.contentResolver
        val fromResolver = runCatching { cr.getType(uri) }.getOrNull()
        val fromName = mimeForFileName(fileNameHint)

        val typeAttempts = buildList {
            fromResolver?.let { add(it) }
            if (fromName != "application/octet-stream") add(fromName)
            add("*/*")
        }.distinct()

        for (mime in typeAttempts) {
            if (tryStart(context, viewIntent(uri, mime))) return true
        }
        if (tryStart(context, dataOnlyIntent(uri))) return true

        val chooserTarget = viewIntent(uri, "*/*")
        val chooser = Intent.createChooser(chooserTarget, "Open file").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uri)
        }
        return tryStart(context, chooser)
    }

    private fun viewIntent(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uri)
        }

    private fun dataOnlyIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uri)
        }

    private fun tryStart(context: Context, intent: Intent): Boolean =
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
}
