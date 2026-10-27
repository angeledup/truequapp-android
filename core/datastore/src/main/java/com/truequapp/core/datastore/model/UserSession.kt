package com.truequapp.core.datastore.model

/**
 * Represents the authenticated user session in memory / DataStore.
 */
data class UserSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val userEmail: String? = null,
    val isKycVerified: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}
