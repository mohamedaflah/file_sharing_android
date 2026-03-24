package com.sharefast.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun connectionLabel(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(network) ?: return "Unknown"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "Wi‑Fi Aware"
            else -> "Local"
        }
    }

    /**
     * Best-effort IPv4 of the active interface (Wi‑Fi / hotspot client / LAN).
     */
    fun localIpv4Address(context: Context): String? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val link = cm.getLinkProperties(cm.activeNetwork ?: return fallbackScan())
                ?: return fallbackScan()
            for (addr in link.linkAddresses) {
                val ip = addr.address
                if (ip is Inet4Address && !ip.isLoopbackAddress) {
                    return ip.hostAddress
                }
            }
        } catch (_: Exception) { }
        return fallbackScan()
    }

    private fun fallbackScan(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    fun wifiManager(context: Context): WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** 0–4 bars when on Wi‑Fi; 0 if unavailable. */
    fun wifiSignalBars(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return 0
        val caps = cm.getNetworkCapabilities(network) ?: return 0
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return 0
        return try {
            val wm = wifiManager(context)
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            val rssi = info.rssi
            WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
        } catch (_: Exception) {
            0
        }
    }
}
