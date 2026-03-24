package com.sharefast.core.network

import java.io.DataInputStream
import java.io.DataOutputStream

private const val MAX_JSON_BYTES = 512 * 1024

fun DataOutputStream.writeJsonPayload(json: String) {
    val bytes = json.toByteArray(Charsets.UTF_8)
    require(bytes.size <= MAX_JSON_BYTES)
    writeInt(bytes.size)
    write(bytes)
    flush()
}

fun DataInputStream.readJsonPayload(): String {
    val len = readInt()
    if (len <= 0 || len > MAX_JSON_BYTES) error("Invalid json length")
    val buf = ByteArray(len)
    readFully(buf)
    return String(buf, Charsets.UTF_8)
}
