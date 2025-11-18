package io.rownd.android.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import io.rownd.android.Rownd
import java.io.*
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


object Encryption {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "io.rownd.android.keystore.v1"
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION_STRING = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
    private const val GCM_IV_LENGTH = 12 // GCM recommended IV size
    private const val AES_KEY_SIZE = 256

    private val messageEncoder = Base64MessageEncoder()
    private val ls: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid(), messageEncoder)
    private val context: Context
        get() = Rownd.appHandleWrapper?.app?.get()?.applicationContext ?: throw EncryptionException("No context available. Did you call Rownd.configure()?")

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setKeySize(AES_KEY_SIZE)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun keyName(keyId: String?): String {
        return "io.rownd.key.${keyId ?: "default"}"
    }

    fun doesKeyExist(keyId: String): Boolean {
        val keyFile = File(context.filesDir, keyName(keyId))
        return keyFile.exists()
    }

    fun storeKey(key: Key, keyId: String) {
        val file = File(context.filesDir, keyName(keyId))

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION_STRING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // The IV is written to the start of the file
        val iv = cipher.iv
        FileOutputStream(file).use { fileOut ->
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use {
                it.write(key.asBytes)
            }
        }
    }

    fun storeKey(key: String, keyId: String) {
        storeKey(Key.fromBase64String(key), keyId)
    }

    fun loadKey(keyId: String): Key? {
        val file = File(context.filesDir, keyName(keyId))
        if (!file.exists()) {
            return null
        }

        try {
            FileInputStream(file).use { fileIn ->
                // Read the IV from the start of the file
                val iv = ByteArray(GCM_IV_LENGTH)
                fileIn.read(iv)

                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION_STRING)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                CipherInputStream(fileIn, cipher).use { cipherIn ->
                    val keyBytes = cipherIn.readBytes()
                    return Key.fromBytes(keyBytes)
                }
            }
        } catch (error: IOException) {
            Log.e("Rownd", "Failed to load encryption key (IO): ${error.message}", error)
            return null
        } catch (error: Exception) {
            Log.e("Rownd", "Failed to load encryption key: ${error.message}", error)
            // It's possible the key is corrupt or something changed, delete it
            file.delete()
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