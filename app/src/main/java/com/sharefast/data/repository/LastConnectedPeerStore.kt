package com.sharefast.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharefast.domain.model.PeerDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lastPeerDataStore: DataStore<Preferences> by preferencesDataStore("last_peer")

data class SavedPeer(
    val displayName: String,
    val hostAddress: String,
    val port: Int,
)

@Singleton
class LastConnectedPeerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds = context.lastPeerDataStore

    private val KEY_NAME = stringPreferencesKey("lp_name")
    private val KEY_HOST = stringPreferencesKey("lp_host")
    private val KEY_PORT = intPreferencesKey("lp_port")

    val savedPeer: Flow<SavedPeer?> = ds.data.map { prefs ->
        val host = prefs[KEY_HOST] ?: return@map null
        val name = prefs[KEY_NAME] ?: "Device"
        val port = prefs[KEY_PORT] ?: return@map null
        SavedPeer(name, host, port)
    }

    suspend fun saveFromPeer(peer: PeerDevice) {
        ds.edit {
            it[KEY_NAME] = peer.displayName
            it[KEY_HOST] = peer.hostAddress
            it[KEY_PORT] = peer.port
        }
    }

    suspend fun save(host: String, port: Int, displayName: String) {
        ds.edit {
            it[KEY_NAME] = displayName
            it[KEY_HOST] = host
            it[KEY_PORT] = port
        }
    }
}
