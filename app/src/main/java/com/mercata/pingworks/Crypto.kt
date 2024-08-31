@file:OptIn(ExperimentalStdlibApi::class)

package com.mercata.pingworks

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.Base64MessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.models.Address
import com.mercata.pingworks.models.getMailHost
import com.mercata.pingworks.registration.UserData
import org.koin.java.KoinJavaComponent.inject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

data class EncryptionKeys(val pair: KeyPair, val id: String)
data class SigningKeys(val pair: KeyPair)

val sodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8, Base64MessageEncoder())

fun generateEncryptionKeys(): EncryptionKeys {
    val pair = sodium.cryptoBoxKeypair()
    return EncryptionKeys(
        pair = pair,
        id = generateRandomString(4)
    )
}

fun ByteArray.encodeToBase64(): String = Base64.getEncoder().encodeToString(this)
//fun String.decodeToBase64(): ByteArray = Base64.getDecoder().decode(this)

fun generateSigningKeys(): SigningKeys {
    return SigningKeys(pair = sodium.cryptoSignKeypair())
}

fun generateRandomString(length: Int = 32): String {
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return Array(length) { alphabet.random() }.joinToString("")
}

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

fun Address.generateLink(): String {
    val sp: SharedPreferences by inject(SharedPreferences::class.java)
    val addresses = listOf(sp.getUserAddress()!!, this).sorted().joinToString(separator = "")
    return addresses.hashedWithSha256()
}

fun String.hashedWithSha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .toHexString()

@Throws(SodiumException::class)
fun signData(privateKey: Key, data: String): String {
    //val signature = ByteArray(size = Sign.ED25519_BYTES)
    return  sodium.cryptoSignDetached(data, privateKey)
    //sodium.cryptoSignDetached(signature, data, data.size.toLong(), privateKey.asBytes)
    //return signature.encodeToBase64()
}
