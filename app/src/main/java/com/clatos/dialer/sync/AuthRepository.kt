package com.clatos.dialer.sync

import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val crmApi: CrmApi,
    private val sessionStore: SessionStore,
) {
    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = crmApi.login(LoginRequest(username, password))
        sessionStore.save(response.token, response.user.id, response.user.name)
    }

    fun logout() = sessionStore.clear()

    fun isAuthenticated(): Boolean = sessionStore.isAuthenticated.value
}
