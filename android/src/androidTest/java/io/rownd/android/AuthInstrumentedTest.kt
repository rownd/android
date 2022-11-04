package io.rownd.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.rownd.android.models.RowndConfig
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthInstrumentedTest {

    lateinit var server: MockWebServer
    lateinit var rownd: RowndClient

    @Before
    @Throws(Exception::class)
    fun setUp() {
//        super.setUp()
        server = MockWebServer()
        server.start()

        val config = RowndConfig()
        Rownd.config.apiUrl = server.url("/").toString()
        rownd = RowndClient(DaggerRowndGraph.create(), config)
//        injectInstrumentation(InstrumentationRegistry.getInstrumentation()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun handle_empty_response() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(400)
            .setBody("")
        )

        val resp = rownd.inst.authRepo.refreshTokenAsync().await()

        assertNull(resp)
        assertFalse(rownd.state.value.auth.isAuthenticated)
    }

}