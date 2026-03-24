package com.sharefast.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharefast.domain.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore("device")

private val KEY_NAME = stringPreferencesKey("display_name")
private val KEY_ID = stringPreferencesKey("device_id")

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceRepository {

    override val deviceDisplayName: Flow<String> = context.deviceDataStore.data.map { prefs ->
        prefs[KEY_NAME] ?: android.os.Build.MODEL.ifBlank { "ShareFast device" }
    }

    override suspend fun setDeviceDisplayName(name: String) {
        context.deviceDataStore.edit { it[KEY_NAME] = name.trim().ifBlank { android.os.Build.MODEL } }
    }

    override suspend fun localDeviceId(): String {
        context.deviceDataStore.edit { prefs ->
            if (prefs[KEY_ID] == null) prefs[KEY_ID] = UUID.randomUUID().toString()
        }
        return context.deviceDataStore.data.first()[KEY_ID]!!
    }
}
