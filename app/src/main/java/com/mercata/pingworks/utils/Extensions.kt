package com.mercata.pingworks.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.CHECKSUM_ALGORITHM
import com.mercata.pingworks.HEADER_MESSAGE_ACCESS
import com.mercata.pingworks.HEADER_MESSAGE_ENCRYPTION
import com.mercata.pingworks.HEADER_MESSAGE_ENVELOPE_CHECKSUM
import com.mercata.pingworks.HEADER_MESSAGE_ENVELOPE_SIGNATURE
import com.mercata.pingworks.HEADER_MESSAGE_HEADERS
import com.mercata.pingworks.HEADER_MESSAGE_ID
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.SYMMETRIC_CIPHER
import com.mercata.pingworks.models.ContentHeaders
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun Address.getHost(): String = this.substringAfter("@")
fun Address.getLocal(): String = this.substringBefore("@")
fun String.parseSimpleDate(): ZonedDateTime =
    ZonedDateTime.parse(this, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z"))

fun String.parseServerDate(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.parse(this), ZoneId.systemDefault())

fun Uri.getNameFromURI(context: Context): String {
    val c: Cursor = context.contentResolver.query(this, null, null, null, null)!!
    c.moveToFirst()
    val result =
        c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) }
            ?: ""
    c.close()
    return result
}

fun List<PublicUserData>.toAccessLinks(accessKey: ByteArray): String {
    return this.joinToString(", ") { profile ->
        val link = profile.address.generateLink()
        val accessKeyFingerprint = profile.publicSigningKey.hashedWithSha256()
        val accessKeyEncrypted =
            encryptAnonymous(accessKey, Key.fromBase64String(profile.publicEncryptionKey))
        "link=${link}; fingerprint=${accessKeyFingerprint.first}; value=${accessKeyEncrypted.encodeToBase64()}; id=${profile.encryptionKeyId}"
    }
}

fun ContentHeaders.generateContentMap(
    accessKey: ByteArray,
    messageId: String,
    accessLinks: String,
    currentUser: UserData
): Map<String, String> {

    val contentHeaderBytes = encrypt_xchacha20poly1305(
        this.contentHeadersText.toByteArray(),
        accessKey
    )!!
    val envelopeHeadersMap = hashMapOf(
        HEADER_MESSAGE_ID to messageId,
        HEADER_MESSAGE_ENCRYPTION to "algorithm=$SYMMETRIC_CIPHER",
        HEADER_MESSAGE_ACCESS to accessLinks,
        HEADER_MESSAGE_HEADERS to "algorithm=$SYMMETRIC_CIPHER; value=${contentHeaderBytes.encodeToBase64()}"
    )

    val entries = envelopeHeadersMap.entries.asSequence().sortedBy { it.key }

    val hashSum = entries.map { it.value }.joinToString("").hashedWithSha256()
    envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_CHECKSUM] =
        "algorithm=$CHECKSUM_ALGORITHM; order=${entries.joinToString(":") { it.key }}; value=${hashSum.first}"
    val signedChecksum = hashSum.first.signData(currentUser.signingKeys.pair.secretKey)
    envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_SIGNATURE] =
        "algorithm=$SIGNING_ALGORITHM; value=${signedChecksum}; id=${currentUser.encryptionKeys.id}"

    return envelopeHeadersMap
}