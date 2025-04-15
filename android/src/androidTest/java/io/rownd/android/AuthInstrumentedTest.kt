package io.rownd.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Component
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.rownd.android.di.component.RowndGraph
import io.rownd.android.di.module.ApiModule
import io.rownd.android.di.module.AuthRepoModule
import io.rownd.android.di.module.FakeNetworkModule
import io.rownd.android.di.module.KtorMockEngineConfig
import io.rownd.android.di.module.RowndConfigProvider
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.util.InvalidRefreshTokenException
import io.rownd.android.util.JwtGenerator
import io.rownd.android.util.NoRefreshTokenPresentException
import io.rownd.android.util.ServerException
import junit.framework.Assert.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.*
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        KtorMockEngineConfig::class,
        FakeNetworkModule::class,
        RowndConfigProvider::class,
        ApiModule::class,
        AuthRepoModule::class,
    ]
)
internal interface TestRowndGraph : RowndGraph {
    fun fakeNetworkModule(): FakeNetworkModule?
    fun inject(config: MockEngineConfig)
}

@RunWith(AndroidJUnit4::class)
class AuthInstrumentedTest {
    lateinit var rownd: RowndClient
    var jwtGenerator = JwtGenerator()
    var httpEngineConfig = MockEngineConfig()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Have to have at least one request handler before instantiating engine
        httpEngineConfig.addHandler {  request ->
            respond(
                content = ByteReadChannel("""{"ip":"127.0.0.1"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val graph = DaggerTestRowndGraph.builder()
            .fakeNetworkModule(FakeNetworkModule(httpEngineConfig))
            .build()


        rownd = RowndClient(graph)
        rownd.config.defaultRequestTimeout = 100L

        // Clear mock handlers before each test
        httpEngineConfig.requestHandlers.clear()
    }

    @Test
    fun throw_if_no_tokens() = runTest {
        try {
            rownd.authRepo.refreshTokenAsync().await()
            fail("Did not throw exception")
        } catch (ex: Exception) {
            if (ex is NoRefreshTokenPresentException) {
                return@runTest
            } else {
                throw ex
            }
        }
    }

    @Test
    fun handle_empty_response() = runTest {
        // Handle all the retries from the API client
        for (i in 0..5) {
            httpEngineConfig.addHandler {
                respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        try {
            // Add a refresh token to the state first
            rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
                accessToken = jwtGenerator.generateTestJwt(),
                refreshToken = jwtGenerator.generateTestJwt()
            )))

            rownd.authRepo.refreshTokenAsync().await()
            fail("Did not throw exception after multiple failures")
        } catch (ex: ServerException) {
            // the test passes
            return@runTest
        }
    }

    @Test
    fun handle_network_failures() = runTest {
        // Handle all the retries from the API client
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val originalAccessToken = jwtGenerator.generateTestJwt()
        val originalRefreshToken = jwtGenerator.generateTestJwt()

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = originalAccessToken,
            refreshToken = originalRefreshToken
        )))

        val resp = rownd.authRepo.refreshTokenAsync().await()

        assertNotNull(resp)
        assertTrue(rownd.state.value.auth.isAuthenticated)
        assertNotSame(rownd.state.value.auth.accessToken, originalAccessToken)
        assertNotSame(rownd.state.value.auth.refreshToken, originalRefreshToken)
    }

    @Test
    fun handle_invalid_token_resp() = runTest {
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "statusCode": 400,
                        "error": "Bad Request",
                        "message": "Invalid refresh token: Refresh token has been consumed"
                    }
                """.trimIndent()),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().minusSeconds(120))
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertTrue(rownd.state.value.auth.isAuthenticated)

        try {
            rownd.authRepo.refreshTokenAsync().await()
            fail("Refresh flow should've thrown")
        } catch (ex: InvalidRefreshTokenException) {
            return@runTest
        }
    }

    @Test
    fun refresh_valid_token() = runTest {
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                    """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().minusSeconds(120))
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        val resp = rownd.authRepo.refreshTokenAsync().await()

        assertNotNull(resp)
        assertTrue(rownd.state.value.auth.isAuthenticated)
    }

    @Test
    fun auto_refresh_expired_token_multi_call() = runTest {
        // We enqueue three different calls in case the refresh token API gets
        // called multiple times. If it does, that's typically bad and will
        // cause the test assertions to fail.
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                    """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                    """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                    """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // Initial state prior to test
        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().minusSeconds(120))
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertTrue(rownd.state.value.auth.isAuthenticated)
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
            httpEngineConfig.addHandler {
                respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().minusSeconds(120))
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        try {
            val resp = rownd.authRepo.refreshTokenAsync().await()
            fail("Refresh flow should've thrown")
        } catch (ex: Throwable) {
            assertTrue(rownd.state.value.auth.isAuthenticated)
        }
    }

    @Test
    fun detect_expired_access_token() = runTest {
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val initialAccessToken = jwtGenerator.generateTestJwt(
            expires = Date.from(Instant.now().plusSeconds(55))
        )

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = initialAccessToken,
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertFalse(rownd.state.value.auth.isAccessTokenValid)

        val resp = rownd.authRepo.getAccessToken()

        assertTrue(resp is String)
        assertNotSame(initialAccessToken, resp)
    }

    @Test
    fun detect_not_expired_access_token() = runTest {
        httpEngineConfig.addHandler {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "${jwtGenerator.generateTestJwt()}",
                        "refresh_token": "${jwtGenerator.generateTestJwt()}"
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val initialAccessToken = jwtGenerator.generateTestJwt(
            expires = Date.from(Instant.now().plusSeconds(65))
        )

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = initialAccessToken,
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertTrue(rownd.state.value.auth.isAccessTokenValid)

        val resp = rownd.authRepo.getAccessToken()

        assertTrue(resp is String)
        assertEquals(initialAccessToken, resp)
    }

    @Test
    fun detect_is_access_token_expired() = runTest {

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().plusSeconds(65)) // Just about to expire when 60sec margin include
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertTrue(rownd.state.value.auth.isAccessTokenValid)

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().plusSeconds(55)) // Just expired when 60sec margin include
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertFalse(rownd.state.value.auth.isAccessTokenValid)

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().plusSeconds(3600)) // Access Token is valid for another hour
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertTrue(rownd.state.value.auth.isAccessTokenValid)

        rownd.stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(
            accessToken = jwtGenerator.generateTestJwt(
                expires = Date.from(Instant.now().minusSeconds(3600)) // Access Token expired an hour ago
            ),
            refreshToken = jwtGenerator.generateTestJwt()
        )))

        assertFalse(rownd.state.value.auth.isAccessTokenValid)
    }

}