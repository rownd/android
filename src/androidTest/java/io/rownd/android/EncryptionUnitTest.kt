package io.rownd.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goterl.lazysodium.utils.Key
import io.rownd.android.util.Encryption
import io.rownd.android.util.asBase64String
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionUnitTest {
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
}