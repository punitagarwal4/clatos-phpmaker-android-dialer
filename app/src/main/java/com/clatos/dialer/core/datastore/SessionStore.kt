package com.clatos.dialer.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for the auth token and minimal session info.
 * Recordings and call logs live in Room; this only holds credentials/session.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "clatos_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _isAuthenticated = MutableStateFlow(prefs.getString(KEY_TOKEN, null) != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    suspend fun currentToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun agentId(): Long = prefs.getLong(KEY_AGENT_ID, -1L)

    fun save(token: String, agentId: Long, agentName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_AGENT_ID, agentId)
            .putString(KEY_AGENT_NAME, agentName)
            .apply()
        _isAuthenticated.value = true
    }

    fun clear() {
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_AGENT_ID = "agent_id"
        const val KEY_AGENT_NAME = "agent_name"
    }
}
