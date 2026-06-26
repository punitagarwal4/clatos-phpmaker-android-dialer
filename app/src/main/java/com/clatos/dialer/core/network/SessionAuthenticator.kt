package com.clatos.dialer.core.network

import com.clatos.dialer.core.datastore.SessionStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reacts to 401 responses. The CRM uses a bearer token without a refresh
 * endpoint (to confirm), so on 401 we clear the session and give up; the
 * authenticated StateFlow flips and the app navigates back to login (US-1.3).
 *
 * If/when the API exposes token refresh, perform it here and return the
 * retried request instead of null.
 */
@Singleton
class SessionAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite loops: if we already retried, stop.
        if (responseCount(response) >= 2) return null
        sessionStore.clearSession()
        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
