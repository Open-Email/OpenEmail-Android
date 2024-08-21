package com.mercata.pingworks

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.registration.UserData
import java.nio.charset.StandardCharsets
import java.util.Base64

data class PrivateKey(val key: Key) {
    override fun toString(): String = key.encodeToBase64()
}

data class PublicKey(val key: Key) {
    override fun toString(): String = key.encodeToBase64()
}

data class EncryptionKeys(val privateKey: PrivateKey, val publicKey: PublicKey, val id: String)
data class SigningKeys(val privateKey: PrivateKey, val publicKey: PublicKey)

val sodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

fun generateEncryptionKeys(): EncryptionKeys {
    val pair = sodium.cryptoBoxKeypair()
    return EncryptionKeys(
        privateKey = PrivateKey(pair.secretKey),
        publicKey = PublicKey(pair.publicKey),
        id = generateRandomString(4)
    )
}

fun Key.encodeToBase64(): String = Base64.getEncoder().encodeToString(this.asBytes)

fun generateSigningKeys(): SigningKeys {
    val pair = sodium.cryptoSignKeypair()
    return SigningKeys(
        privateKey = PrivateKey(pair.secretKey),
        publicKey = PublicKey(pair.publicKey),
    )
}

fun generateRandomString(length: Int = 32): String {
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return Array(length) { alphabet.random() }.joinToString("")
}

fun sign(user: UserData): String {
    val value = generateRandomString()
    val signature: String? = try {
        signData(
            privateKey = user.signingKeys.privateKey.key,
            data = (user.getHost() + value).toByteArray()
        )
    } catch (e: SodiumException) {
        null
    }

    val headers = mutableListOf<String>()

    headers.add("$NONCE_HEADER_VALUE_KEY$headerKeyValueSeparator$value")
    headers.add("$NONCE_HEADER_VALUE_HOST$headerKeyValueSeparator${user.getHost()}")
    headers.add("$NONCE_HEADER_ALGORITHM_KEY$headerKeyValueSeparator$SIGNING_ALGORITHM")
    headers.add("$NONCE_HEADER_SIGNATURE_KEY$headerKeyValueSeparator$signature")
    headers.add("$NONCE_HEADER_PUBKEY_KEY$headerKeyValueSeparator${user.signingKeys.publicKey}")

    return headers.joinToString(prefix = "$NONCE_SCHEME ", separator = headerFieldSeparator)
}

@Throws(SodiumException::class)
fun signData(privateKey: Key, data: ByteArray): String {
    val signature = ByteArray(size = Sign.ED25519_BYTES)
    sodium.cryptoSignDetached(signature, data, data.size.toLong(), privateKey.asBytes)
    return Base64.getEncoder().encodeToString(signature)
}
