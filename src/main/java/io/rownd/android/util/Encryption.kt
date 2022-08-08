package io.rownd.android.util

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import java.util.*


object Encryption {
    private var messageEncoder = Base64MessageEncoder()
    private var ls: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid(), messageEncoder)

    fun generateKey(): Key {
        return ls.cryptoSecretBoxKeygen()
    }

    @Throws(SodiumException::class)
    fun encrypt(plaintext: String, withKey: Key): String {
        val nonce = ls.randomBytesBuf(SecretBox.NONCEBYTES)
        val ciphertext = ls.cryptoSecretBoxEasy(plaintext, nonce, withKey)

        return messageEncoder.encode(nonce + messageEncoder.decode(ciphertext))
    }

    fun decrypt(ciphertext: String, withKey: Key): String {
        val noncedCipherByteArray = messageEncoder.decode(ciphertext)
        val nonce = noncedCipherByteArray.copyOfRange(0, SecretBox.NONCEBYTES)
        val cipherTextBytes = noncedCipherByteArray.copyOfRange(SecretBox.NONCEBYTES, noncedCipherByteArray.size)
        return ls.cryptoSecretBoxOpenEasy(messageEncoder.encode(cipherTextBytes), nonce, withKey)
    }
}

fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))

val Key.asBase64String: String
    get() = this.asBytes.toBase64()