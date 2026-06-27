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
 * Keystore-backed encrypted storage for the auth token, the per-tenant CRM
 * server URL, and minimal session info. Recordings and call logs live in Room;
 * this only holds credentials/session.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences = createPrefs(context)

    private val _isAuthenticated = MutableStateFlow(prefs.getString(KEY_TOKEN, null) != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    suspend fun currentToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Per-tenant CRM base URL (e.g. https://acme.crm.com/). */
    fun currentBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun agentId(): Long = prefs.getLong(KEY_AGENT_ID, -1L)

    fun agentName(): String = prefs.getString(KEY_AGENT_NAME, "").orEmpty()

    fun save(token: String, agentId: Long, agentName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_AGENT_ID, agentId)
            .putString(KEY_AGENT_NAME, agentName)
            .apply()
        _isAuthenticated.value = true
    }

    fun lastContactSync(): String? = prefs.getString(KEY_LAST_CONTACT_SYNC, null)

    fun setLastContactSync(iso: String) {
        prefs.edit().putString(KEY_LAST_CONTACT_SYNC, iso).apply()
    }

    /** Clears the token/session but preserves the server URL so re-login on the
     *  same device keeps the tenant. */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_AGENT_ID)
            .remove(KEY_AGENT_NAME)
            .apply()
        _isAuthenticated.value = false
    }

    /** Full wipe (explicit logout on a shared device). */
    fun clearAll() {
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
    }

    private companion object {
        const val PREFS_NAME = "clatos_session"
        const val KEY_TOKEN = "token"
        const val KEY_BASE_URL = "base_url"
        const val KEY_AGENT_ID = "agent_id"
        const val KEY_AGENT_NAME = "agent_name"
        const val KEY_LAST_CONTACT_SYNC = "last_contact_sync"

        /**
         * Builds the EncryptedSharedPreferences. The underlying Keystore key can
         * be invalidated (e.g. lockscreen/biometric changes), which makes
         * create() throw — recover by deleting the corrupt store and recreating
         * rather than crashing on launch.
         */
        fun createPrefs(context: Context): SharedPreferences {
            fun build(): SharedPreferences {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }
            return try {
                build()
            } catch (e: Exception) {
                // Corrupt/invalidated keystore entry — reset and rebuild (user
                // will need to log in again, but the app won't crash on start).
                context.deleteSharedPreferences(PREFS_NAME)
                build()
            }
        }
    }
}
