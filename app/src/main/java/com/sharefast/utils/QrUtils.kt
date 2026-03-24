package com.sharefast.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrUtils {
    private const val PREFIX = "sharefast://"

    fun buildConnectPayload(host: String, port: Int): String = "$PREFIX$host:$port"

    fun parseConnectPayload(raw: String): Pair<String, Int>? {
        val s = raw.trim()
        if (!s.startsWith(PREFIX)) return null
        val rest = s.removePrefix(PREFIX)
        val parts = rest.split(":")
        if (parts.size != 2) return null
        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: return null
        if (host.isBlank() || port <= 0) return null
        return host to port
    }

    fun encodeQrBitmap(content: String, sizePx: Int = 768): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }
}
