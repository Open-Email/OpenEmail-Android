@file:OptIn(ExperimentalStdlibApi::class)

package com.mercata.pingworks

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.registration.UserData
import org.koin.java.KoinJavaComponent.inject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

data class EncryptionKeys(val pair: KeyPair, val id: String)
data class SigningKeys(val pair: KeyPair)

val sodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8, Base64MessageEncoder())

@Throws(SodiumException::class)
fun generateEncryptionKeys(): EncryptionKeys {
    val pair = sodium.cryptoBoxKeypair()
    return EncryptionKeys(
        pair = pair,
        id = generateRandomString(4)
    )
}

fun ByteArray.encodeToBase64(): String = Base64.getEncoder().encodeToString(this)
fun String.decodeToBase64(): ByteArray = Base64.getDecoder().decode(this)

@Throws(SodiumException::class)
fun generateSigningKeys(): SigningKeys {
    return SigningKeys(pair = sodium.cryptoSignKeypair())
}

fun generateRandomString(length: Int = 32): String {
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return Array(length) { alphabet.random() }.joinToString("")
}

@Throws(SodiumException::class)
fun UserData.sign(): String {
    val value = generateRandomString()
    val signature: String? = try {
        signData(
            privateKey = this.signingKeys.pair.secretKey,
            data = (this.address.getMailHost() + value)
        )
    } catch (e: SodiumException) {
        null
    }

    val headers = mutableListOf<String>()

    headers.add("$NONCE_HEADER_VALUE_KEY$headerKeyValueSeparator$value")
    headers.add("$NONCE_HEADER_VALUE_HOST$headerKeyValueSeparator${this.address.getMailHost()}")
    headers.add("$NONCE_HEADER_ALGORITHM_KEY$headerKeyValueSeparator$SIGNING_ALGORITHM")
    headers.add("$NONCE_HEADER_SIGNATURE_KEY$headerKeyValueSeparator$signature")
    headers.add("$NONCE_HEADER_PUBKEY_KEY$headerKeyValueSeparator${this.signingKeys.pair.publicKey.asBytes.encodeToBase64()}")
    return headers.joinToString(prefix = "$NONCE_SCHEME ", separator = headerFieldSeparator)
}

@Throws(SodiumException::class)
fun encryptAnonymous(address: Address, currentUser: UserData): String {
    return sodium.cryptoBoxSealEasy(address, currentUser.encryptionKeys.pair.publicKey)
}

@Throws(SodiumException::class)
fun decryptAnonymous(cipherText: String, currentUser: UserData): String {
    return sodium.cryptoBoxSealOpenEasy(cipherText, currentUser.encryptionKeys.pair)
}

@Throws(SodiumException::class)
fun verifySignature(publicKey: Key, signature: String, originData: ByteArray): Boolean {
    return sodium.cryptoSignVerifyDetached(
        signature.decodeToBase64(),
        originData,
        originData.size,
        publicKey.asBytes
    )
}

fun Address.generateLink(): String {
    val sp: SharedPreferences by inject(SharedPreferences::class.java)
    val addresses = listOf(sp.getUserAddress()!!, this).sorted().joinToString(separator = "")
    return addresses.hashedWithSha256().first
}

/*static func decrypt_xchacha20poly1305(cipherData: [UInt8], secretKey: [UInt8]) throws -> [UInt8]{
        let sodium = Sodium()

        guard let decrypted = sodium.aead.xchacha20poly1305ietf.decrypt(nonceAndAuthenticatedCipherText: cipherData, secretKey: secretKey) else {
            throw CryptoError.decryptionError
        }
        return decrypted
    }*/

@Throws(SodiumException::class)
fun decrypt_xchacha20poly1305(cipher: String, accessKey: Key): String {
    //val nonceBytes = AEAD.XCHACHA20POLY1305_IETF_ABYTES
    return sodium.cryptoSecretBoxOpenEasy(cipher, cipher.toByteArray(), accessKey)
}

fun String.hashedWithSha256(): Pair<String, ByteArray> = toByteArray().hashedWithSha256()

fun ByteArray.hashedWithSha256(): Pair<String, ByteArray> {
    val rv = MessageDigest.getInstance("SHA-256")
        .digest(this)
    return rv.toHexString() to rv
}

@Throws(SodiumException::class)
fun signData(privateKey: Key, data: String): String {
    //val signature = ByteArray(size = Sign.ED25519_BYTES)
    return sodium.cryptoSignDetached(data, privateKey)
    //sodium.cryptoSignDetached(signature, data, data.size.toLong(), privateKey.asBytes)
    //return signature.encodeToBase64()
}
