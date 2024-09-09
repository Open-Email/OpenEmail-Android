@file:OptIn(ExperimentalStdlibApi::class)

package com.mercata.pingworks

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_ABYTES
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES
import com.goterl.lazysodium.interfaces.Box
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
fun decryptAnonymous(cipherText: String, currentUser: UserData): ByteArray {
    val cipher: ByteArray = cipherText.decodeToBase64()
    val message = ByteArray(cipher.size - Box.SEALBYTES)

    val res: Boolean = sodium.cryptoBoxSealOpen(
        message,
        cipher,
        cipher.size.toLong(),
        currentUser.encryptionKeys.pair.publicKey.asBytes,
        currentUser.encryptionKeys.pair.secretKey.asBytes
    )
    if (!res) {
        throw SodiumException("Could not decrypt your message.")
    }
    return message
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


@Throws(SodiumException::class)
fun decrypt_xchacha20poly1305(cipherBytes: ByteArray, accessKey: Key): ByteArray {

    val valid = cipherBytes.size >= XCHACHA20POLY1305_IETF_NPUBBYTES + XCHACHA20POLY1305_IETF_ABYTES

    val nonce = cipherBytes.sliceArray(0 until XCHACHA20POLY1305_IETF_NPUBBYTES)
    val message = cipherBytes.sliceArray(XCHACHA20POLY1305_IETF_NPUBBYTES until cipherBytes.size)
    val decryptedMessage = ByteArray(cipherBytes.size - XCHACHA20POLY1305_IETF_ABYTES)

    val success = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
        decryptedMessage,  // The output buffer where the message will be written
        null,              // Message length, if you don't need to capture it
        null,              // Secret nonce (optional, generally null)
        message,        // Ciphertext (input encrypted data)
        message.size.toLong(),  // Ciphertext length
        null,    // Optional additional authenticated data (AAD)
        0,          // Length of AAD
        nonce,             // Public nonce used during encryption
        accessKey.asBytes          // Secret key used to encrypt
    )

    if (!valid || !success) {
        throw SodiumException("Couldn't decrypt")
    }

    return decryptedMessage
}

fun String.hashedWithSha256(): Pair<String, ByteArray> = toByteArray().hashedWithSha256()

fun ByteArray.hashedWithSha256(): Pair<String, ByteArray> {
    val rv = MessageDigest.getInstance("SHA-256")
        .digest(this)
    return rv.toHexString() to rv
}

@Throws(SodiumException::class)
fun signData(privateKey: Key, data: String): String {
    return sodium.cryptoSignDetached(data, privateKey)
}
