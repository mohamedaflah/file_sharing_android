package com.sharefast.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appearanceDataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(
    name = "appearance",
)

private val KEY_ACCENT = intPreferencesKey("accent_index")
private val KEY_GREETING = stringPreferencesKey("greeting_name")

@Singleton
class AppearancePreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val ds = context.appearanceDataStore

    val accentIndex: Flow<Int> = ds.data.map { prefs -> prefs[KEY_ACCENT] ?: 0 }

    val greetingName: Flow<String> = ds.data.map { prefs -> prefs[KEY_GREETING].orEmpty() }

    suspend fun setAccentIndex(index: Int) {
        ds.edit { it[KEY_ACCENT] = index.coerceIn(0, MAX_ACCENT_INDEX) }
    }

    suspend fun setGreetingName(name: String) {
        ds.edit { it[KEY_GREETING] = name.trim().take(32) }
    }
}

private const val MAX_ACCENT_INDEX = 5
