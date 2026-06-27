package com.clatos.dialer.core.network

import com.clatos.dialer.core.datastore.SessionStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites every request to the per-tenant CRM base URL stored at login, so a
 * single app build can serve many customers (each with their own PHPMaker
 * server). Retrofit's compile-time baseUrl is only a placeholder. Supports base
 * URLs that include a path prefix (e.g. https://host/crm/).
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = sessionStore.currentBaseUrl()
        if (base.isNullOrBlank()) return chain.proceed(request)

        val cleanBase = base.trimEnd('/')
        val query = request.url.encodedQuery
        val full = cleanBase + request.url.encodedPath + if (query != null) "?$query" else ""
        val newUrl = full.toHttpUrlOrNull() ?: return chain.proceed(request)

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
