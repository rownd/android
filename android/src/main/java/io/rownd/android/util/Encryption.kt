package io.rownd.android.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import io.rownd.android.Rownd
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*


object Encryption {
    private val messageEncoder = Base64MessageEncoder()
    private val ls: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid(), messageEncoder)
    private val context: Context
        get() = Rownd.appHandleWrapper?.app?.get()?.applicationContext ?: throw EncryptionException("No context available. Did you call Rownd.configure()?")

    private fun keyName(keyId: String?): String {
        return "io.rownd.key.${keyId ?: "default"}"
    }

    private fun getKeyFile(keyId: String): EncryptedFile {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        return EncryptedFile.Builder(
            File(context.filesDir, keyId),
            context,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    fun doesKeyExist(keyId: String): Boolean {
        val keyFile = File(context.filesDir, keyName(keyId))

        return keyFile.exists()
    }

    fun storeKey(key: Key, keyId: String) {
        val keyFile = getKeyFile(keyName(keyId))

        keyFile.openFileOutput().apply {
            write(key.asBytes)
            flush()
            close()
        }
    }

    fun storeKey(key: String, keyId: String) {
        storeKey(Key.fromBase64String(key), keyId)
    }

    fun loadKey(keyId: String): Key? {
        val keyFile = getKeyFile(keyName(keyId))

        try {
            val inputStream = keyFile.openFileInput()
            val byteArrayOutputStream = ByteArrayOutputStream()

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }

            val keyBytes = byteArrayOutputStream.toByteArray()

            return Key.fromBytes(keyBytes)
        } catch (error: IOException) {
            return null
        } catch (error: Exception) {
            Log.e("Rownd", "Failed to load encryption key: ${error.message}")
            return null
        }
    }

    fun deleteKey(keyId: String) {
        val keyFile = File(context.filesDir, keyName(keyId))
        keyFile.delete()
    }

    fun generateKey(): Key {
        return ls.cryptoSecretBoxKeygen()
    }

    @Throws(SodiumException::class)
    fun encrypt(plaintext: String, withKey: Key): String {
        val nonce = ls.randomBytesBuf(SecretBox.NONCEBYTES)
        val ciphertext = ls.cryptoSecretBoxEasy(plaintext, nonce, withKey)

        return messageEncoder.encode(nonce + messageEncoder.decode(ciphertext))
    }

    @Throws(SodiumException::class)
    fun encrypt(plaintext: String, keyId: String): String {
        val key = loadKey(keyId) ?: throw EncryptionException("The requested key '$keyId' could not be found")
        return encrypt(plaintext, key)
    }

    @Throws(SodiumException::class)
    fun decrypt(ciphertext: String, withKey: Key): String {
        val noncedCipherByteArray = messageEncoder.decode(ciphertext)
        val nonce = noncedCipherByteArray.copyOfRange(0, SecretBox.NONCEBYTES)
        val cipherTextBytes = noncedCipherByteArray.copyOfRange(SecretBox.NONCEBYTES, noncedCipherByteArray.size)
        return ls.cryptoSecretBoxOpenEasy(messageEncoder.encode(cipherTextBytes), nonce, withKey)
    }

    @Throws(SodiumException::class)
    fun decrypt(ciphertext: String, keyId: String): String {
        val key = loadKey(keyId) ?: throw EncryptionException("The requested key '$keyId' could not be found")
        return decrypt(ciphertext, key)
    }
}

fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))

val Key.asBase64String: String
    get() = this.asBytes.toBase64()

class EncryptionException(message: String) : Exception(message)