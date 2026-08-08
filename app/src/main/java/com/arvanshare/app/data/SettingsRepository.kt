package com.arvanshare.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Prefs {
        val NAME = stringPreferencesKey("name")
        val ENDPOINT = stringPreferencesKey("endpoint")
        val BUCKET = stringPreferencesKey("bucket")
        val ACCESS_KEY = stringPreferencesKey("access_key")
        val SECRET_KEY = stringPreferencesKey("secret_key")
        val COMPLETE = booleanPreferencesKey("setup_complete")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            name = p[Prefs.NAME] ?: "",
            endpoint = p[Prefs.ENDPOINT] ?: "",
            bucket = p[Prefs.BUCKET] ?: "",
            accessKey = p[Prefs.ACCESS_KEY] ?: "",
            secretKey = p[Prefs.SECRET_KEY] ?: "",
            setupComplete = p[Prefs.COMPLETE] ?: false,
        )
    }

    suspend fun save(s: Settings) {
        context.dataStore.edit { p ->
            p[Prefs.NAME] = s.name
            p[Prefs.ENDPOINT] = s.endpoint
            p[Prefs.BUCKET] = s.bucket
            p[Prefs.ACCESS_KEY] = s.accessKey
            p[Prefs.SECRET_KEY] = s.secretKey
            p[Prefs.COMPLETE] = s.isComplete()
        }
    }
}
