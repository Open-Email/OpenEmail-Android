package com.mercata.openemail.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.mercata.openemail.CHECKSUM_ALGORITHM
import com.mercata.openemail.DEFAULT_SERVER_DATE_FORMAT
import com.mercata.openemail.HEADER_MESSAGE_ACCESS
import com.mercata.openemail.HEADER_MESSAGE_ENCRYPTION
import com.mercata.openemail.HEADER_MESSAGE_ENVELOPE_CHECKSUM
import com.mercata.openemail.HEADER_MESSAGE_ENVELOPE_SIGNATURE
import com.mercata.openemail.HEADER_MESSAGE_HEADERS
import com.mercata.openemail.HEADER_MESSAGE_ID
import com.mercata.openemail.SIGNING_ALGORITHM
import com.mercata.openemail.SYMMETRIC_CIPHER
import com.mercata.openemail.db.pending.readers.DBPendingReaderPublicData
import com.mercata.openemail.models.ContentHeaders
import com.mercata.openemail.registration.UserData
import java.security.MessageDigest
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

fun Uri.getNameFromURI(context: Context): String? {
    return when (scheme) {
        "content" -> {
            context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    return if (nameIndex >= 0) cursor.getString(nameIndex) else null
                }
                null
            }
        }

        "file" -> {
            this.path?.substringAfterLast('/')
        }

        else -> {
            null
        }
    }
}

fun Uri.getMimeType(context: Context): String? {
    return when (scheme) {
        "content" -> context.contentResolver.getType(this)
        "file" -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(this.toString())
        )

        else -> null
    }
}

fun List<DBPendingReaderPublicData>.generateAccessLinks(accessKey: ByteArray): String {
    return this.joinToString(", ") { profile ->
        val link = profile.address.connectionLink()
        val accessKeyFingerprint = profile.publicSigningKey.decodeFromBase64().hashedWithSha256()
        val accessKeyEncrypted =
            encryptAnonymous(accessKey, profile.publicEncryptionKey.decodeFromBase64())
        "link=${link}; fingerprint=${accessKeyFingerprint.first}; value=${accessKeyEncrypted.encodeToBase64()}; id=${profile.encryptionKeyId}"
    }
}

fun ContentHeaders.seal(
    accessKey: ByteArray,
    messageId: String,
    accessLinks: String,
    currentUser: UserData,
    isBroadcast: Boolean
): Map<String, String> {

    val contentHeaderBytes =
        if (isBroadcast) this.contentHeadersText.toByteArray() else encrypt_xchacha20poly1305(
            this.contentHeadersText.toByteArray(),
            accessKey
        )!!
    val envelopeHeadersMap = hashMapOf(
        HEADER_MESSAGE_ID to messageId
    )

    if (isBroadcast) {
        envelopeHeadersMap.putAll(
            listOf(
                HEADER_MESSAGE_HEADERS to "value=${contentHeaderBytes.encodeToBase64()}"
            )
        )
    } else {
        envelopeHeadersMap.putAll(
            listOf(
                HEADER_MESSAGE_ID to messageId,
                HEADER_MESSAGE_ENCRYPTION to "algorithm=$SYMMETRIC_CIPHER",
                HEADER_MESSAGE_ACCESS to accessLinks,
                HEADER_MESSAGE_HEADERS to "algorithm=$SYMMETRIC_CIPHER; value=${contentHeaderBytes.encodeToBase64()}"
            )
        )
    }

    val entries = envelopeHeadersMap.entries.asSequence().sortedBy { it.key }

    val hashSum = entries.map { it.value }.joinToString("").hashedWithSha256()
    envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_CHECKSUM] =
        "algorithm=$CHECKSUM_ALGORITHM; order=${entries.joinToString(":") { it.key }}; value=${hashSum.first}"
    val signedChecksum =
        hashSum.second.signDataBytes(currentUser.signingKeys.pair.secretKey).encodeToBase64()
    envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_SIGNATURE] =
        "algorithm=$SIGNING_ALGORITHM; value=${signedChecksum}; id=${currentUser.encryptionKeys.id}"

    return envelopeHeadersMap
}

fun Instant.toServerFormatString(): String {
    return DateTimeFormatter.ofPattern(DEFAULT_SERVER_DATE_FORMAT)
        .withZone(ZoneId.of("UTC")).format(this)
}

fun UserData.connectionLinkFor(remoteAddress: Address): String {
    val linkJoin = listOf(this.address, remoteAddress)
        .map { it.lowercase().trim() }
        .sorted()
        .joinToString("")

    // Hash the combined string using SHA-256
    val digest = MessageDigest.getInstance("SHA-256").digest(linkJoin.toByteArray())

    // Convert the digest to a hexadecimal string
    return digest.joinToString("") { "%02x".format(it) }
}