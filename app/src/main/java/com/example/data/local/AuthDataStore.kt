package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_auth_prefs")

data class SavedAuthSession(
    val authType: String = "XTREAM", // "XTREAM" or "M3U"
    val playlistName: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val isLoggedIn: Boolean = false,
    val rememberCredentials: Boolean = true,
    val lastLoginTimestamp: Long = 0L
)

class AuthDataStore(private val context: Context) {

    companion object {
        val KEY_AUTH_TYPE = stringPreferencesKey("auth_type")
        val KEY_PLAYLIST_NAME = stringPreferencesKey("playlist_name")
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_M3U_URL = stringPreferencesKey("m3u_url")
        val KEY_EPG_URL = stringPreferencesKey("epg_url")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_REMEMBER = booleanPreferencesKey("remember_credentials")
        val KEY_LAST_LOGIN = longPreferencesKey("last_login_timestamp")
    }

    val authSession: Flow<SavedAuthSession> = context.dataStore.data.map { prefs ->
        SavedAuthSession(
            authType = prefs[KEY_AUTH_TYPE] ?: "XTREAM",
            playlistName = prefs[KEY_PLAYLIST_NAME] ?: "",
            serverUrl = prefs[KEY_SERVER_URL] ?: "",
            username = prefs[KEY_USERNAME] ?: "",
            password = prefs[KEY_PASSWORD] ?: "",
            m3uUrl = prefs[KEY_M3U_URL] ?: "",
            epgUrl = prefs[KEY_EPG_URL] ?: "",
            isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false,
            rememberCredentials = prefs[KEY_REMEMBER] ?: true,
            lastLoginTimestamp = prefs[KEY_LAST_LOGIN] ?: 0L
        )
    }

    suspend fun saveXtreamCredentials(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        remember: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TYPE] = "XTREAM"
            prefs[KEY_PLAYLIST_NAME] = name
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_USERNAME] = username
            prefs[KEY_PASSWORD] = if (remember) password else ""
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_REMEMBER] = remember
            prefs[KEY_LAST_LOGIN] = System.currentTimeMillis()
        }
    }

    suspend fun saveM3uCredentials(
        name: String,
        m3uUrl: String,
        epgUrl: String = "",
        remember: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TYPE] = "M3U"
            prefs[KEY_PLAYLIST_NAME] = name
            prefs[KEY_M3U_URL] = m3uUrl
            prefs[KEY_EPG_URL] = epgUrl
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_REMEMBER] = remember
            prefs[KEY_LAST_LOGIN] = System.currentTimeMillis()
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            val remember = prefs[KEY_REMEMBER] ?: true
            if (!remember) {
                prefs.remove(KEY_PASSWORD)
                prefs.remove(KEY_USERNAME)
                prefs.remove(KEY_SERVER_URL)
                prefs.remove(KEY_M3U_URL)
                prefs.remove(KEY_EPG_URL)
                prefs.remove(KEY_PLAYLIST_NAME)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
