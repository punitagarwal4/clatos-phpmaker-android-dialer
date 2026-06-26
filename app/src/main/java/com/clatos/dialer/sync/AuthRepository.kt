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
    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = crmApi.login(LoginRequest(username, password))
        sessionStore.save(response.token, response.user.id, response.user.name)
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
        (this as? retrofit2.HttpException)?.code() == 401
}
