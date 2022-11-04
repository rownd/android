package io.rownd.android

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SignInLinksInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.rownd.android.test", appContext.packageName)
    }

    @Test
    fun parseVariousUris() {
        val httpUri = Uri.parse("http://test.rownd.link/foo")
        val httpsUri = Uri.parse("https://test.rownd.link/bar")
        val noSchemeUri = Uri.parse("test.rownd.link/baz")

        assertEquals("http", httpUri.scheme)
        assertEquals("https", httpsUri.scheme)
        assertEquals(null, noSchemeUri.scheme)
    }
}