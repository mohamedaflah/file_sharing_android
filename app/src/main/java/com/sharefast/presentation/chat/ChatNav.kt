package com.sharefast.presentation.chat

import android.net.Uri
import com.sharefast.domain.model.PeerDevice

fun chatRoute(peer: PeerDevice): String {
    val key = peer.id
    return "chat/${Uri.encode(key)}/${Uri.encode(peer.displayName)}/${Uri.encode(peer.hostAddress)}/${peer.port}"
}

fun chatRoute(
    peerKey: String,
    peerName: String,
    host: String,
    port: Int,
): String = "chat/${Uri.encode(peerKey)}/${Uri.encode(peerName)}/${Uri.encode(host)}/$port"

