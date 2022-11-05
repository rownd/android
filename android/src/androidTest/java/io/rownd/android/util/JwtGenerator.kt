package io.rownd.android.util

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.*

class JwtGenerator {
    val key: OctetKeyPair = OctetKeyPairGenerator(Curve.Ed25519)
        .keyUse(KeyUse.SIGNATURE)
        .keyID("test")
        .generate()

    fun generateTestJwt(
        expires: Date? = Date.from(Instant.now().plusSeconds(120))
    ): String {
        val header = JWSHeader.Builder(JWSAlgorithm.EdDSA)
            .type(JOSEObjectType.JWT)
            .keyID(key.keyID)
            .build()

        val payload = JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .issuer("RowndAndroidSDKTests")
            .audience("RowndAndroidSDKTests")
            .subject("1234567890")
            .expirationTime(expires)
            .build()

        val signedJwt = SignedJWT(header, payload)
        signedJwt.sign(Ed25519Signer(key))

        return signedJwt.serialize()
    }
}