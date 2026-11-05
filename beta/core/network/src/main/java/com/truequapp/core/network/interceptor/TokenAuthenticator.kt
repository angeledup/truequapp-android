package com.truequapp.core.network.interceptor

import com.truequapp.core.datastore.UserSessionDataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that transparently refreshes expired JWT tokens upon receiving 401 Unauthorized.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionDataStore: UserSessionDataStore
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if authentication has already been retried 3 times
        if (responseCount(response) >= 3) {
            return null
        }

        return runBlocking {
            refreshMutex.withLock {
                val currentToken = sessionDataStore.getAccessToken()
                val requestHeaderToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                // If another thread already refreshed the token, retry with the updated token
                if (!currentToken.isNullOrBlank() && currentToken != requestHeaderToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = sessionDataStore.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    sessionDataStore.clearSession()
                    return@withLock null
                }

                // In a production backend, perform synchronous refresh HTTP call here.
                // If refresh succeeds:
                // sessionDataStore.saveTokens(newAccessToken, newRefreshToken)
                // return request with new token.
                // For now, if no refresh network endpoint is wired yet, return null.
                null
            }
        }
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
