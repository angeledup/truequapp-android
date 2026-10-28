package com.truequapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
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
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_KYC_VERIFIED = booleanPreferencesKey("kyc_verified")
    }

    override val sessionFlow: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            accessToken = prefs[KEY_ACCESS_TOKEN],
            refreshToken = prefs[KEY_REFRESH_TOKEN],
            userId = prefs[KEY_USER_ID],
            userEmail = prefs[KEY_USER_EMAIL],
            isKycVerified = prefs[KEY_KYC_VERIFIED] ?: false
        )
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_KYC_VERIFIED] = isKycVerified
        }
    }

    override suspend fun getAccessToken(): String? {
        return dataStore.data.first()[KEY_ACCESS_TOKEN]
    }

    override suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[KEY_REFRESH_TOKEN]
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_KYC_VERIFIED)
        }
    }
}
