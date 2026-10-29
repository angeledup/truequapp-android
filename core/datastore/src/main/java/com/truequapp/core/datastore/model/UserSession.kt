package com.truequapp.core.datastore.model

/**
 * Represents the authenticated user session in memory / DataStore.
 * Canonical single source of truth for user profile, authentication, and TruequeCoins balance.
 */
data class UserSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isKycVerified: Boolean = false,
    val truequeCoins: Double = 100.0,
    val escrowCoins: Double = 0.0,
    val authProvider: String = "EMAIL" // "GOOGLE", "PHONE", "EMAIL"
) {
    val totalCoins: Double
        get() = truequeCoins + escrowCoins

    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}
