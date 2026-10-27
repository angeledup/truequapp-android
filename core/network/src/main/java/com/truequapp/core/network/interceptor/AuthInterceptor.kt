package com.truequapp.core.network.interceptor

import com.truequapp.core.datastore.UserSessionDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that injects the JWT Bearer authentication token into outbound requests.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionDataStore: UserSessionDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header if header "No-Authentication" is present or public endpoint
        if (originalRequest.header("No-Authentication") != null) {
            val requestWithoutHeader = originalRequest.newBuilder()
                .removeHeader("No-Authentication")
                .build()
            return chain.proceed(requestWithoutHeader)
        }

        val token = runBlocking { sessionDataStore.getAccessToken() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
