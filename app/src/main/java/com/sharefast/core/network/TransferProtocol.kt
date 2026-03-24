package com.sharefast.core.network

import com.sharefast.core.ShareConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class WireMessage(
    @SerialName("v") val version: Int = ShareConstants.PROTOCOL_VERSION,
    @SerialName("cmd") val command: String,
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("name") val deviceName: String? = null,
    @SerialName("files") val files: List<FileDescriptorWire>? = null,
    @SerialName("error") val error: String? = null,
)

@Serializable
data class FileDescriptorWire(
    @SerialName("n") val name: String,
    @SerialName("s") val size: Long,
)

fun WireMessage.toWireJson(): String = json.encodeToString(this)

fun parseWireMessage(payload: String): WireMessage = json.decodeFromString(payload.trim())
