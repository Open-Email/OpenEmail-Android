@file:OptIn(ExperimentalStdlibApi::class)

package com.mercata.pingworks.utils

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_ABYTES
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_KEYBYTES
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.Hash
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.NONCE_HEADER_ALGORITHM_KEY
import com.mercata.pingworks.NONCE_HEADER_PUBKEY_KEY
import com.mercata.pingworks.NONCE_HEADER_SIGNATURE_KEY
import com.mercata.pingworks.NONCE_HEADER_VALUE_HOST
import com.mercata.pingworks.NONCE_HEADER_VALUE_KEY
import com.mercata.pingworks.NONCE_SCHEME
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.headerFieldSeparator
import com.mercata.pingworks.headerKeyValueSeparator
import com.mercata.pingworks.registration.UserData
import okio.Utf8
import org.koin.java.KoinJavaComponent.inject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class EncryptionKeys(val pair: KeyPair, val id: String)
data class SigningKeys(val pair: KeyPair)
typealias Nonce = ByteArray

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
fun String.decodeFromBase64(): ByteArray = Base64.getDecoder().decode(this)

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
        (this.address.getMailHost() + value).signData(
            privateKey = this.signingKeys.pair.secretKey,
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
    return encryptAnonymous(address, currentUser.encryptionKeys.pair.publicKey)
}

@Throws(SodiumException::class)
fun encryptAnonymous(address: Address, publicEncryptionKey: Key): String {
    return sodium.cryptoBoxSealEasy(address, publicEncryptionKey)
}

@Throws(SodiumException::class)
fun encryptAnonymous(data: ByteArray, publicEncryptionKey: Key): ByteArray {
    val keyBytes: ByteArray = publicEncryptionKey.asBytes
    val cipher = ByteArray(Box.SEALBYTES + data.size)

    if (!sodium.cryptoBoxSeal(cipher, data, data.size.toLong(), keyBytes)) {
        throw SodiumException("Could not encrypt message.")
    }
    return cipher
}

@Throws(SodiumException::class)
fun decryptAnonymous(cipherText: String, currentUser: UserData): ByteArray {
    val cipher: ByteArray = cipherText.decodeFromBase64()
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
        signature.decodeFromBase64(),
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


fun generateRandomBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return bytes
}

fun UserData.newMessageId(): String {
    val random = generateRandomString(24)
    val rawId = "$random${this.address.getHost()}${this.address.getLocal()}"
    return rawId.hashedWithSha256().first
}

fun encrypt_xchacha20poly1305(
    message: ByteArray,
    secretKey: ByteArray,
): ByteArray? {

    if (secretKey.size != XCHACHA20POLY1305_IETF_KEYBYTES) {
        return null
    }

    val nonce = sodium.nonce(XCHACHA20POLY1305_IETF_NPUBBYTES)

    val authenticatedCipherText = ByteArray(message.size + XCHACHA20POLY1305_IETF_ABYTES) //382

    val success = sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
        authenticatedCipherText,         // Output ciphertext buffer
        null,                            // Output ciphertext length (not needed in LazySodium)
        message,                         // Message to encrypt
        message.size.toLong(),           // Message length
        null,                  // Additional authenticated data (AAD)
        0,  // Length of the additional data
        null,                            // Secret nonce (optional, usually null)
        nonce,                           // Nonce generated
        secretKey                        // Secret key used for encryption
    )

    if (!success) {
        return null
    }

    return nonce.plus(authenticatedCipherText)
}


@Throws(SodiumException::class)
fun decrypt_xchacha20poly1305(cipherBytes: ByteArray, accessKey: ByteArray): ByteArray {

    val valid = cipherBytes.size >= XCHACHA20POLY1305_IETF_NPUBBYTES + XCHACHA20POLY1305_IETF_ABYTES

    if (!valid) {
        throw SodiumException("invalid cipher")
    }

    val nonce = cipherBytes.sliceArray(0 until XCHACHA20POLY1305_IETF_NPUBBYTES)
    val message = cipherBytes.sliceArray(XCHACHA20POLY1305_IETF_NPUBBYTES until cipherBytes.size)
    val decryptedMessage = ByteArray(cipherBytes.size - XCHACHA20POLY1305_IETF_ABYTES)
    // Length of decrypted message reference
    val decryptedLength = LongArray(1)

    val success = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
        decryptedMessage,  // The output buffer where the message will be written
        decryptedLength,              // Message length, if you don't need to capture it
        null,              // Secret nonce (optional, generally null)
        message,        // Ciphertext (input encrypted data)
        message.size.toLong(),  // Ciphertext length
        null,    // Optional additional authenticated data (AAD)
        0,          // Length of AAD
        nonce,             // Public nonce used during encryption
        accessKey          // Secret key used to encrypt
    )

    if (!success) {
        throw SodiumException("Couldn't decrypt")
    }

    return decryptedMessage.sliceArray(0 until decryptedLength[0].toInt())
}

fun String.hashedWithSha256(): Pair<String, ByteArray> = toByteArray().hashedWithSha256()

fun ByteArray.hashedWithSha256(): Pair<String, ByteArray> {
    val hash = ByteArray(Hash.SHA256_BYTES)
    sodium.cryptoHashSha256(hash, this, this.size.toLong())

    val hexChecksum = sodium.toHexStr(hash).lowercase()
    return Pair(hexChecksum, hash)
}

@Throws(SodiumException::class)
fun String.signData(privateKey: Key): String {
    return sodium.cryptoSignDetached(this, privateKey)
}

@Throws(SodiumException::class)
fun ByteArray.signDataBytes(privateKey: Key): ByteArray {
    val skBytes: ByteArray = privateKey.asBytes
    val signatureBytes = ByteArray(Sign.BYTES)

    if (!sodium.cryptoSignDetached(signatureBytes, this, this.size.toLong(), skBytes)) {
        throw SodiumException("Could not create a signature for your message in detached mode.")
    }

    return signatureBytes
}
