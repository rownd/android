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
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun handle_empty_response() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("")
        )

        val resp = rownd.inst.authRepo.refreshTokenAsync().await()

        assertNull(resp)
        assertFalse(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun handle_invalid_token_resp() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(400)
            .setBody("{\"statusCode\":400,\"error\":\"Bad Request\",\"message\":\"Invalid refresh token: Refresh token has been consumed\"}")
        )

        val storeField = rownd.inst.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.inst.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().minusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        val resp = rownd.inst.authRepo.refreshTokenAsync().await()

        assertNull(resp)
        assertFalse(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun refresh_valid_token() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        val storeField = rownd.inst.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.inst.stateRepo) as Store<GlobalState, StateAction>).dispatch(
            StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(
                    expires = Date.from(Instant.now().minusSeconds(120))
                ),
                refreshToken = jwtGenerator.generateTestJwt()
            ))
        )

        val resp = rownd.inst.authRepo.refreshTokenAsync().await()

        assertNotNull(resp)
        assertTrue(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun auto_refresh_expired_token_multi_call() = runTest {
        // We enqueue three different calls in case the refresh token API gets
        // called multiple times. If it does, that's typically bad and will
        // cause the test assertions to fail.
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "${jwtGenerator.generateTestJwt()}",
                    "refresh_token": "${jwtGenerator.generateTestJwt()}"
                }
            """.trimIndent())
        )

        // Initial state prior to test
        val storeField = rownd.inst.stateRepo.javaClass.getDeclaredField("store")
        storeField.isAccessible = true
        (storeField.get(rownd.inst.stateRepo) as Store<GlobalState, StateAction>).dispatch(
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
            rownd.inst.authRepo.getAccessToken()
        }, async {
            rownd.inst.authRepo.getAccessToken()
        }, async {
            rownd.inst.authRepo.getAccessToken()
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

}