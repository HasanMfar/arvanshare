package com.arvanshare.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Prefs {
        val NAME = stringPreferencesKey("name")
        val ENDPOINT = stringPreferencesKey("endpoint")
        val BUCKET = stringPreferencesKey("bucket")
        val ACCESS_KEY = stringPreferencesKey("access_key")
        val SECRET_KEY = stringPreferencesKey("secret_key")
        val COMPLETE = booleanPreferencesKey("setup_complete")
        val ACTIVE_ROOM = stringPreferencesKey("active_room")
        val ACTIVE_ROOM_PASSWORD = stringPreferencesKey("active_room_password")
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val rawProfiles = p[Prefs.PROFILES_JSON] ?: ""
        val parsedProfiles = if (rawProfiles.isNotBlank()) {
            runCatching { json.decodeFromString<List<BucketProfile>>(rawProfiles) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        Settings(
            name = p[Prefs.NAME] ?: "",
            endpoint = p[Prefs.ENDPOINT] ?: "",
            bucket = p[Prefs.BUCKET] ?: "",
            accessKey = p[Prefs.ACCESS_KEY] ?: "",
            secretKey = p[Prefs.SECRET_KEY] ?: "",
            setupComplete = p[Prefs.COMPLETE] ?: false,
            activeRoom = p[Prefs.ACTIVE_ROOM] ?: "",
            activeRoomPassword = p[Prefs.ACTIVE_ROOM_PASSWORD] ?: "",
            profiles = parsedProfiles,
            activeProfileId = p[Prefs.ACTIVE_PROFILE_ID] ?: "",
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
            p[Prefs.ACTIVE_ROOM] = s.activeRoom
            p[Prefs.ACTIVE_ROOM_PASSWORD] = s.activeRoomPassword
            p[Prefs.PROFILES_JSON] = json.encodeToString(s.profiles)
            p[Prefs.ACTIVE_PROFILE_ID] = s.activeProfileId
        }
    }
}
