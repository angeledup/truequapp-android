package com.truequapp.core.network

import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.network.interceptor.AuthInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeUserSessionDataStore(
    private var accessToken: String? = null
) : UserSessionDataStore {
    override val sessionFlow: Flow<UserSession> = flowOf(UserSession(accessToken = accessToken))
    override suspend fun saveTokens(accessToken: String, refreshToken: String) { this.accessToken = accessToken }
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {}
    override suspend fun getAccessToken(): String? = accessToken
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() { accessToken = null }
}

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `when token exists in datastore, auth header is injected`() {
        val fakeStore = FakeUserSessionDataStore(accessToken = "valid_jwt_token_123")
        val interceptor = AuthInterceptor(fakeStore)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer valid_jwt_token_123", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `when No-Authentication header is passed, auth header is omitted`() {
        val fakeStore = FakeUserSessionDataStore(accessToken = "valid_jwt_token_123")
        val interceptor = AuthInterceptor(fakeStore)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/public"))
            .addHeader("No-Authentication", "true")
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }
}
