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

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDED, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    suspend fun currentToken(): String? = prefs.getString(KEY_TOKEN, null)

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

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDED, complete).apply()
        _onboardingComplete.value = complete
    }

    /** Clears the token/session but preserves the onboarding flag so a re-login
     *  on the same device skips first-run setup. */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_AGENT_ID)
            .remove(KEY_AGENT_NAME)
            .apply()
        _isAuthenticated.value = false
    }

    /** Full wipe (e.g. explicit logout on a shared device). */
    fun clearAll() {
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
        _onboardingComplete.value = false
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_AGENT_ID = "agent_id"
        const val KEY_AGENT_NAME = "agent_name"
        const val KEY_ONBOARDED = "onboarding_complete"
    }
}
