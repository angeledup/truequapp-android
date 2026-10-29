package com.truequapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserSessionDataStore {
    val sessionFlow: Flow<UserSession>
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String = "",
        photoUrl: String? = null,
        authProvider: String = "EMAIL"
    )
    suspend fun updateTruequeCoins(available: Double, escrow: Double? = null) {}
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getTruequeCoins(): Double = 100.0
    suspend fun clearSession()
}

@Singleton
class UserSessionDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSessionDataStore {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_PHOTO = stringPreferencesKey("user_photo")
        private val KEY_KYC_VERIFIED = booleanPreferencesKey("kyc_verified")
        private val KEY_TRUEQUE_COINS = doublePreferencesKey("trueque_coins")
        private val KEY_ESCROW_COINS = doublePreferencesKey("escrow_coins")
        private val KEY_AUTH_PROVIDER = stringPreferencesKey("auth_provider")

        const val DEFAULT_INITIAL_COINS = 100.0
    }

    override val sessionFlow: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            accessToken = prefs[KEY_ACCESS_TOKEN],
            refreshToken = prefs[KEY_REFRESH_TOKEN],
            userId = prefs[KEY_USER_ID] ?: "usr-demo",
            userName = prefs[KEY_USER_NAME] ?: "Usuario Verificado",
            userEmail = prefs[KEY_USER_EMAIL] ?: "usuario@truequapp.com",
            userPhotoUrl = prefs[KEY_USER_PHOTO],
            isKycVerified = prefs[KEY_KYC_VERIFIED] ?: true,
            truequeCoins = prefs[KEY_TRUEQUE_COINS] ?: DEFAULT_INITIAL_COINS,
            escrowCoins = prefs[KEY_ESCROW_COINS] ?: 0.0,
            authProvider = prefs[KEY_AUTH_PROVIDER] ?: "EMAIL"
        )
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String,
        photoUrl: String?,
        authProvider: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            if (photoUrl != null) {
                prefs[KEY_USER_PHOTO] = photoUrl
            }
            prefs[KEY_KYC_VERIFIED] = isKycVerified
            prefs[KEY_AUTH_PROVIDER] = authProvider
            if (!prefs.contains(KEY_TRUEQUE_COINS)) {
                prefs[KEY_TRUEQUE_COINS] = DEFAULT_INITIAL_COINS
            }
        }
    }

    override suspend fun updateTruequeCoins(available: Double, escrow: Double?) {
        dataStore.edit { prefs ->
            prefs[KEY_TRUEQUE_COINS] = available
            if (escrow != null) {
                prefs[KEY_ESCROW_COINS] = escrow
            }
        }
    }

    override suspend fun getAccessToken(): String? {
        return dataStore.data.first()[KEY_ACCESS_TOKEN]
    }

    override suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[KEY_REFRESH_TOKEN]
    }

    override suspend fun getTruequeCoins(): Double {
        return dataStore.data.first()[KEY_TRUEQUE_COINS] ?: DEFAULT_INITIAL_COINS
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_PHOTO)
            prefs.remove(KEY_KYC_VERIFIED)
            prefs.remove(KEY_AUTH_PROVIDER)
            prefs[KEY_TRUEQUE_COINS] = DEFAULT_INITIAL_COINS
            prefs[KEY_ESCROW_COINS] = 0.0
        }
    }
}
