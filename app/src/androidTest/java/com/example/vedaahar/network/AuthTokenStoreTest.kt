package com.example.vedaahar.network

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthTokenStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    // Isolated test preferences: never touch the patient's real JWT/preferences.
    private val name = "auth_token_store_instrumentation_test"
    private val preferences get() = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Before fun setUp() { preferences.edit().clear().commit() }
    @After fun tearDown() { context.deleteSharedPreferences(name) }

    @Test fun pairSurvivesStoreRecreationAndAccessRefreshPreservesRefreshToken() {
        AuthTokenStore(preferences).saveTokens("test-access", "test-refresh")
        val recreated = AuthTokenStore(preferences)
        assertTrue(recreated.hasTokens())
        assertEquals("test-access", recreated.getAccessToken())
        recreated.saveTokens("refreshed-access")
        assertEquals("refreshed-access", AuthTokenStore(preferences).getAccessToken())
        assertEquals("test-refresh", AuthTokenStore(preferences).getRefreshToken())
    }

    @Test fun clearRemovesBothTokens() {
        val store = AuthTokenStore(preferences)
        store.saveTokens("test-access", "test-refresh")
        store.clear()
        assertFalse(AuthTokenStore(preferences).hasTokens())
        assertNull(store.getAccessToken())
        assertNull(store.getRefreshToken())
    }

    @Test fun partialSessionIsNotAuthenticated() {
        val store = AuthTokenStore(preferences)
        store.saveTokens("test-access")
        assertFalse(store.hasTokens())
    }

    @Test fun blankTokensAreRejectedWithoutReplacingExistingSession() {
        val store = AuthTokenStore(preferences)
        store.saveTokens("test-access", "test-refresh")
        try {
            store.saveTokens("", "bad-refresh")
            fail("Blank access token must be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals("test-access", store.getAccessToken())
            assertEquals("test-refresh", store.getRefreshToken())
        }
    }
}
