package io.rownd.android

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goterl.lazysodium.utils.Key
import io.rownd.android.util.Encryption
import io.rownd.android.util.asBase64String
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

class RowndTest : Application()

@RunWith(AndroidJUnit4::class)
class EncryptionInstrumentedTest {

    private val KEY_ID = "test-key"

    lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentationContext = instrumentation.context

        val app = Instrumentation.newApplication(Application::class.java, instrumentationContext)
        Rownd.configure(app, "")
    }

    @After
    fun teardown() {
        // Clean up old keys
        Encryption.deleteKey(KEY_ID)
    }

    @Test
    fun key_generate() {
        val key = Encryption.generateKey()
        println(key.asBase64String)
        assertNotNull(key)
    }

    @Test
    fun encrypt_data() {
        val key = Key.fromBase64String("4f4a6IInDuSga0wyQQQpMSrDHIZ/ryoc9w6s5xVF/VQ=")

        val plainText = "This super secret string will never be known."
        val cipherText = Encryption.encrypt(plainText, key)

        println(cipherText)

        assertNotNull(cipherText)
    }

    @Test
    fun decrypt_data() {
        val key = Key.fromBase64String("4f4a6IInDuSga0wyQQQpMSrDHIZ/ryoc9w6s5xVF/VQ=")
        val expectedPlainText = "This super secret string will never be known."
        val cipherText = "Di0IyYbC141WIPKzFnlsQc0BIi1AWKSpLf6Th9TcDDJiidPfkVazXtFibnsqJyKFaQf7SaF68yihnqJXidodfKqKzjM2MnbHbh+O8wpxFO3gO6OhVg=="

        val computedPlainText = Encryption.decrypt(cipherText, key)

        assertEquals(computedPlainText, expectedPlainText)
    }

    @Test
    fun encrypt_then_decrypt() {
        val key = Encryption.generateKey()

        val plainText = "This super secret string will never be known."
        val cipherText = Encryption.encrypt(plainText, key)

        println(cipherText)

        val computedPlainText = Encryption.decrypt(cipherText, key)

        assertEquals(computedPlainText, plainText)
    }

    @Test
    fun store_encryption_key() {
        val key = Encryption.generateKey()
        Encryption.storeKey(key, KEY_ID)

        assert(true)    // key stored successfully
    }

    @Test
    fun load_encryption_key() {
        val key = Encryption.generateKey()
        Encryption.storeKey(key, KEY_ID)

        val storedKey = Encryption.loadKey(KEY_ID)

        assertEquals(key.asBase64String, storedKey?.asBase64String)
    }

    @Test
    fun load_nonexistant_key() {
        val storedKey = Encryption.loadKey(KEY_ID)

        assertNull(storedKey)
    }
}