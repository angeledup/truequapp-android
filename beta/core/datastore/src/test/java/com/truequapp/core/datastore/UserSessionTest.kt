package com.truequapp.core.datastore

import com.truequapp.core.datastore.model.UserSession
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserSessionTest {

    @Test
    fun `user session is authenticated when access token is present`() {
        val session = UserSession(accessToken = "token_xyz")
        assertTrue(session.isAuthenticated)
    }

    @Test
    fun `user session is not authenticated when access token is null or blank`() {
        val sessionNull = UserSession(accessToken = null)
        val sessionBlank = UserSession(accessToken = "   ")
        assertFalse(sessionNull.isAuthenticated)
        assertFalse(sessionBlank.isAuthenticated)
    }
}
