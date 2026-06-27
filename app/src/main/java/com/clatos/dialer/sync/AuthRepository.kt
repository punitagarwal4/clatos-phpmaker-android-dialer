package com.clatos.dialer.sync

import com.clatos.dialer.core.database.dao.CallLogDao
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val crmApi: CrmApi,
    private val sessionStore: SessionStore,
    private val callLogDao: CallLogDao,
) {
    fun currentBaseUrl(): String? = sessionStore.currentBaseUrl()

    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> = runCatching {
        // Persist the tenant URL FIRST so the host interceptor targets it for the
        // login call itself.
        sessionStore.setBaseUrl(normalizeUrl(serverUrl))
        val response = crmApi.login(LoginRequest(username, password))
        sessionStore.save(response.token, response.user.id, response.user.name)
    }

    /** Ensures a usable base URL: adds https:// if no scheme, and a trailing slash. */
    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) url = "$url/"
        return url
    }

    /**
     * Validates the stored token against the CRM (GET /api/me). On an auth
     * failure the session is cleared so the app gates back to login. Network
     * errors are tolerated (offline) — the existing session is kept.
     */
    suspend fun validateSession(): Boolean {
        if (sessionStore.agentId() < 0) return false
        return runCatching { crmApi.me() }
            .fold(
                onSuccess = { true },
                onFailure = { error ->
                    if (error.isAuthError()) {
                        sessionStore.clearSession()
                        false
                    } else {
                        // Offline / transient: keep the session as-is.
                        true
                    }
                },
            )
    }

    /** Number of call logs not yet uploaded — used to warn before logout. */
    suspend fun pendingSyncCount(): Int = callLogDao.pending().size

    fun agentName(): String = sessionStore.agentName()

    /** Explicit logout: full wipe of the session (US-1.4). */
    fun logout() = sessionStore.clearAll()

    fun isAuthenticated(): Boolean = sessionStore.isAuthenticated.value

    private fun Throwable.isAuthError(): Boolean =
        (this as? retrofit2.HttpException)?.code() in setOf(401, 403)
}
