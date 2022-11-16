package io.rownd.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.GlobalState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.util.JwtGenerator
import junit.framework.Assert.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthInstrumentedTest {

    lateinit var server: MockWebServer
    lateinit var rownd: RowndClient
    var jwtGenerator = JwtGenerator()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        server = MockWebServer()
        server.start()

        val config = RowndConfig()
        Rownd.config.apiUrl = server.url("/").toString()
        rownd = RowndClient(DaggerRowndGraph.create(), config)
        config.apiUrl = server.url("").toString()
        config.defaultRequestTimeout = 1000L
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun handle_empty_response() = runTest {
        // Handle all the retries from the API client
        for (i in 0..5) {
            server.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("")
            )
        }

        try {
            val resp = rownd.authRepo.refreshTokenAsync().await()
            fail("Did not throw exception after multiple failures")
        } catch (ex: Exception) {
            // the test passes
            return@runTest
        }
    }

    @Test
    fun handle_network_failures() = runTest {
        // Handle all the retries from the API client
        server.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("")
        )

//        server.enqueue(MockResponse()
//            .setSocketPolicy(SocketPolicy.NO_RESPONSE)
//        )

        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("")
            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY)
        )

        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        val resp = rownd.authRepo.refreshTokenAsync().await()

        assertNotNull(resp)
        assertTrue(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun handle_invalid_token_resp() = runTest {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(400)
            .setBody("{\"statusCode\":400,\"error\":\"Bad Request\",\"message\":\"Invalid refresh token: Refresh token has been consumed\"}")
        )

        val storeField = rownd.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().minusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        assertTrue(rownd.state.value.auth.isAuthenticated)

        val resp = rownd.authRepo.refreshTokenAsync().await()

        assertNull(resp)
        assertFalse(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun refresh_valid_token() = runTest {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        val storeField = rownd.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().minusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        val resp = rownd.authRepo.refreshTokenAsync().await()

        assertNotNull(resp)
        assertTrue(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun auto_refresh_expired_token_multi_call() = runTest {
        // We enqueue three different calls in case the refresh token API gets
        // called multiple times. If it does, that's typically bad and will
        // cause the test assertions to fail.
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        // Initial state prior to test
        val storeField = rownd.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().minusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        assertFalse(rownd.state.value.auth.isAccessTokenValid)

        // We get the access token multiple times asynchronously
        // to ensure that the refresh token flow is only being
        // called once per expired access token.
        val tokens = awaitAll(async {
            rownd.authRepo.getAccessToken()
        }, async {
            rownd.authRepo.getAccessToken()
        }, async {
            rownd.authRepo.getAccessToken()
        })

        assertEquals(tokens.size, 3)

        val firstToken = tokens[0]
        for (token in tokens) {
            assertNotNull(token)
            assertEquals(token, firstToken)
        }

        assertTrue(rownd.state.value.auth.isAccessTokenValid)
        assertTrue(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun prevent_signout_on_server_errors() = runTest {
        // Handle all the retries from the API client
        for (i in 0..5) {
            server.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("")
            )
        }

        val storeField = rownd.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().plusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        try {
            val resp = rownd.authRepo.refreshTokenAsync().await()
            fail("Refresh flow should've thrown")
        } catch (ex: Throwable) {
            assertTrue(rownd.state.value.auth.isAuthenticated)
        }
    }

}