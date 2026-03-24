package com.sharefast.services.discovery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class UdpAnnouncePacket(
    @SerialName("t") val type: String = "SF_ANN",
    @SerialName("id") val id: String,
    @SerialName("n") val name: String,
    @SerialName("p") val port: Int,
    @SerialName("v") val version: Int = 1,
)

fun UdpAnnouncePacket.toUdpBytes(): ByteArray = json.encodeToString(this).encodeToByteArray()

fun decodeUdpPacket(bytes: ByteArray, length: Int): UdpAnnouncePacket? {
    if (length <= 0 || length > 4096) return null
    return try {
        json.decodeFromString<UdpAnnouncePacket>(String(bytes, 0, length, Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }
}
