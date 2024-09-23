package com.mercata.pingworks.models

import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.CHECKSUM_ALGORITHM
import com.mercata.pingworks.CHECKSUM_HEADERS
import com.mercata.pingworks.HEADER_CONTENT_AUTHOR
import com.mercata.pingworks.HEADER_CONTENT_CATEGORY
import com.mercata.pingworks.HEADER_CONTENT_CHECKSUM
import com.mercata.pingworks.HEADER_CONTENT_DATE
import com.mercata.pingworks.HEADER_CONTENT_FILES
import com.mercata.pingworks.HEADER_CONTENT_MESSAGE_ID
import com.mercata.pingworks.HEADER_CONTENT_PARENT_ID
import com.mercata.pingworks.HEADER_CONTENT_READERS
import com.mercata.pingworks.HEADER_CONTENT_SIZE
import com.mercata.pingworks.HEADER_CONTENT_SUBJECT
import com.mercata.pingworks.HEADER_CONTENT_SUBJECT_ID
import com.mercata.pingworks.HEADER_MESSAGE_ACCESS
import com.mercata.pingworks.HEADER_MESSAGE_ENCRYPTION
import com.mercata.pingworks.HEADER_MESSAGE_ENVELOPE_CHECKSUM
import com.mercata.pingworks.HEADER_MESSAGE_ENVELOPE_SIGNATURE
import com.mercata.pingworks.HEADER_MESSAGE_HEADERS
import com.mercata.pingworks.HEADER_MESSAGE_ID
import com.mercata.pingworks.HEADER_MESSAGE_STREAM
import com.mercata.pingworks.MAX_CHUNK_SIZE
import com.mercata.pingworks.MAX_HEADERS_SIZE
import com.mercata.pingworks.MIN_CHUNK_SIZE
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.SYMMETRIC_CIPHER
import com.mercata.pingworks.SYMMETRIC_FILE_CIPHER
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.exceptions.AlgorithmMissMatch
import com.mercata.pingworks.exceptions.BadChecksum
import com.mercata.pingworks.exceptions.BadChunkSize
import com.mercata.pingworks.exceptions.BadMessageId
import com.mercata.pingworks.exceptions.EnvelopeAuthenticity
import com.mercata.pingworks.exceptions.FingerprintMismatch
import com.mercata.pingworks.exceptions.SignatureMismatch
import com.mercata.pingworks.exceptions.TooLargeEnvelope
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.connectionLink
import com.mercata.pingworks.utils.decodeFromBase64
import com.mercata.pingworks.utils.decryptAnonymous
import com.mercata.pingworks.utils.decrypt_xchacha20poly1305
import com.mercata.pingworks.utils.hashedWithSha256
import com.mercata.pingworks.utils.verifySignature
import okhttp3.Headers
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant
import kotlin.text.Charsets.UTF_8

class Envelope(
    val messageId: String,
    val currentUser: UserData,
    val contact: DBContact,
    headers: Headers? = null
) {

    private val envelopeHeadersMap = headers?.associate { it.first to it.second } ?: mapOf()
    private val streamId: String?
    private var accessLinks: String?
    var accessKey: ByteArray? = null
        private set
    private var contentHeadersBytes: ByteArray
    private val headersOrder: String
    private val headersChecksum: String
    private val headersSignature: String
    private val payloadCipher: String?
    private val payloadCipherInfo: PayloadSeal?
    val contentHeaders: ContentHeaders

    init {
        if (envelopeHeadersMap.entries.any { it.key.length + it.value.length > MAX_HEADERS_SIZE }) {
            throw TooLargeEnvelope(envelopeHeadersMap.toString())
        }

        payloadCipher = envelopeHeadersMap[HEADER_MESSAGE_ENCRYPTION]
        payloadCipherInfo = if (payloadCipher == null) null else cipherInfoFromHeader(payloadCipher)

        accessLinks = envelopeHeadersMap[HEADER_MESSAGE_ACCESS]
        accessLinks?.run { parseAccessLink() }

        envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_CHECKSUM]!!.let { headerVal ->
            val checksumMap = parseHeaderAttributes(headerVal)
            val algorithm = checksumMap["algorithm"] ?: throw BadChecksum(checksumMap.toString())
            val sum = checksumMap["value"] ?: throw BadChecksum(checksumMap.toString())
            val order = checksumMap["order"] ?: throw BadChecksum(checksumMap.toString())
            if (algorithm.lowercase() != CHECKSUM_ALGORITHM) {
                throw BadChecksum(checksumMap.toString())
            }
            headersOrder = order
            headersChecksum = sum
        }

        contentHeaders = envelopeHeadersMap[HEADER_MESSAGE_HEADERS]!!.let { headerVal ->
            val contentHeaderMap = parseHeaderAttributes(headerVal)
            contentHeaderMap["algorithm"]?.let { algorithm ->
                if (algorithm != SYMMETRIC_CIPHER) {
                    throw AlgorithmMissMatch(algorithm)
                }
            }
            contentHeadersBytes = contentHeaderMap["value"]!!.decodeFromBase64()
            openContentHeaders()
        }

        envelopeHeadersMap[HEADER_MESSAGE_ID]!!.let { headerVal ->
            if (messageId != headerVal) {
                throw BadMessageId(messageId + "\n" + headerVal)
            }
        }

        streamId = envelopeHeadersMap[HEADER_MESSAGE_STREAM]

        headersSignature =
            envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_SIGNATURE]!!.let { headerVal ->
                val sigMap = parseHeaderAttributes(headerVal)
                val algorithm = sigMap["algorithm"]
                val data = sigMap["value"]
                if (algorithm?.lowercase() != SIGNING_ALGORITHM) {
                    throw AlgorithmMissMatch(algorithm.toString())
                }
                data!!
            }
    }

    @Throws(FingerprintMismatch::class)
    private fun parseAccessLink() {
        val connectionLink = contact.address.connectionLink()
        val readerLinks = accessLinks?.split(",")?.map { it.trim() } ?: listOf()

        for (readerLink in readerLinks) {
            val readerMap = parseHeaderAttributes(readerLink)

            val accessKeyFp = readerMap["fingerprint"] ?: continue
            val value = readerMap["value"] ?: continue
            val link = readerMap["link"] ?: continue
            readerMap["id"] ?: continue
            if (link != connectionLink) continue


            if (!currentUser.signingKeys.pair.publicKey.asBytes.hashedWithSha256().first.startsWith(
                    accessKeyFp
                )
            ) {
                throw FingerprintMismatch(accessKeyFp)
            }

            val decrypted = decryptAnonymous(value, currentUser)
            this.accessKey = decrypted
        }
    }

    private fun openContentHeaders(): ContentHeaders {
        if (isBroadcast()) {
            return contentFromHeaders(String(contentHeadersBytes, charset = UTF_8))
        } else {
            val result =
                decrypt_xchacha20poly1305(contentHeadersBytes, accessKey!!)
            return contentFromHeaders(String(result, UTF_8))
        }
    }

    private fun contentFromHeaders(headerText: String): ContentHeaders {
        val headersMap: Map<String, String> =
            headerText
                .split("\n")
                .map { it.trim() }
                .filterNot { it.isBlank() }
                .associate { header ->
                    val parts = header
                        .split(":", limit = 2)
                        .map { it.trim() }
                        .filterNot { it.isBlank() }

                    parts.first() to parts.last()
                }

        val parsedFiles = headersMap[HEADER_CONTENT_FILES]?.let { parseFilesHeader(it) }
            ?: (listOf<MessageFilePartInfo>() to listOf())

        return ContentHeaders(
            messageID = headersMap[HEADER_CONTENT_MESSAGE_ID]!!,
            date = Instant.parse(headersMap[HEADER_CONTENT_DATE]!!),
            subject = headersMap[HEADER_CONTENT_SUBJECT]!!,
            subjectId = headersMap[HEADER_CONTENT_SUBJECT_ID],
            parentId = headersMap[HEADER_CONTENT_PARENT_ID]?.trim()?.replace("\u0000", ""),
            files = parsedFiles.second,
            filesHeader = headersMap[HEADER_CONTENT_FILES],
            fileParts = parsedFiles.first,
            category = MessageCategory.entries.firstOrNull {
                it.name == (headersMap[HEADER_CONTENT_CATEGORY] ?: "")
            } ?: MessageCategory.personal,
            size = headersMap[HEADER_CONTENT_SIZE]!!.toLong(),
            checksum = headersMap[HEADER_CONTENT_CHECKSUM]!!.let { checksum ->
                val checksumMap = parseHeaderAttributes(checksum)
                val sum = checksumMap["value"]
                if (checksumMap["algorithm"]?.lowercase() != CHECKSUM_ALGORITHM) {
                    throw AlgorithmMissMatch(checksumMap["algorithm"] ?: "")
                }
                sum!!
            },
            authorAddress = headersMap[HEADER_CONTENT_AUTHOR]!!,
            readersAddresses = headersMap[HEADER_CONTENT_READERS]?.split(",")?.map { it.trim() }
                ?.filterNot { it.isBlank() } ?: listOf(),
        )
    }

    private fun parseFilesHeader(filesHeader: String): Pair<List<MessageFilePartInfo>, List<MessageFileInfo>> {
        val fileStrings = filesHeader.split(",")
        val fileParts = mutableListOf<MessageFilePartInfo>()


        for (fileString in fileStrings) {
            val urlInfoDict = mutableMapOf<String, String>()
            var messageId: String? = null
            var part: Long = 0
            var totalParts: Long = 0
            var modifiedAt: String = ""

            val keyValuePairs = fileString.split(";").map { it.trim() }
            for (pair in keyValuePairs) {
                val keyValue = pair.split("=", limit = 2).map { it.trim() }
                if (keyValue.size == 2) {
                    val key = keyValue[0]
                    val value = keyValue[1]

                    when (key) {
                        "name" -> urlInfoDict["name"] = value
                        "type" -> urlInfoDict["type"] = value
                        "size" -> urlInfoDict["size"] = value
                        "id" -> messageId = value
                        "part" -> {
                            val parts = value.split("/").mapNotNull { it.toLongOrNull() }
                            if (parts.size == 2) {
                                if (parts[0] > 0) {
                                    part = parts[0] - 1
                                }
                                totalParts = parts[1]
                            }
                        }

                        "modified" -> {
                            modifiedAt = value
                        }
                    }
                }
            }

            if (messageId != null && urlInfoDict["name"] != null && urlInfoDict["type"] != null && urlInfoDict["size"] != null) {
                val name = urlInfoDict["name"]!!
                val mimeType = urlInfoDict["type"]!!
                val size = urlInfoDict["size"]!!.toLongOrNull() ?: 0L
                val urlInfo = URLInfo(
                    uri = null,
                    name = name,
                    mimeType = mimeType,
                    size = size,
                    modifiedAt = modifiedAt
                )
                val fileInfo = MessageFilePartInfo(
                    urlInfo = urlInfo,
                    messageId = messageId,
                    part = part,
                    size = size,
                    totalParts = totalParts
                )
                fileParts.add(fileInfo)
            }
        }

        val fileInfos = groupMessageFilePartsIntoFileInfo(fileParts)
        return Pair(fileParts, fileInfos)
    }

    private fun groupMessageFilePartsIntoFileInfo(parts: List<MessageFilePartInfo>): List<MessageFileInfo> {
        val partsDictionary = mutableMapOf<String, MutableList<MessageFilePartInfo>>()

        for (part in parts) {
            val name = part.urlInfo.name
            if (partsDictionary[name] == null) {
                partsDictionary[name] = mutableListOf(part)
            } else {
                partsDictionary[name]?.add(part)
            }
        }

        val fileInfoArray = mutableListOf<MessageFileInfo>()
        for ((_, groupedParts) in partsDictionary) {
            val sortedParts = groupedParts.sortedBy { it.part }
            val firstPart = sortedParts.first()
            val complete = sortedParts.size.toLong() == firstPart.totalParts
            val messageIdsParts = sortedParts.map { it.messageId }

            val fileInfo = MessageFileInfo(
                name = firstPart.urlInfo.name,
                mimeType = firstPart.urlInfo.mimeType,
                size = firstPart.urlInfo.size,
                modifiedAt = firstPart.urlInfo.modifiedAt,
                messageIds = messageIdsParts,
                complete = complete
            )
            fileInfoArray.add(fileInfo)
        }

        return fileInfoArray
    }

    fun isBroadcast(): Boolean {
        val accessLinks = accessLinks
        return accessLinks.isNullOrEmpty()
    }

    fun isRootMessage(): Boolean = contentHeaders.parentId == null

    private fun cipherInfoFromHeader(header: String): PayloadSeal {
        val attributes = parseHeaderAttributes(header)
        val algorithm = attributes["algorithm"]
        val chunkSize = attributes["chunk-size"]?.toIntOrNull()

        val isStream = chunkSize != null && algorithm == SYMMETRIC_FILE_CIPHER
        if (chunkSize != null && (chunkSize < MIN_CHUNK_SIZE || chunkSize >= MAX_CHUNK_SIZE)) {
            throw BadChunkSize(chunkSize.toString())
        }

        return PayloadSeal(
            algorithm = algorithm ?: "",
            stream = isStream,
            chunkSize = chunkSize ?: 0,
            originalHeaderValue = header
        )
    }

    private fun parseHeaderAttributes(header: String): Map<String, String> =
        header.split(";").associate { attribute ->
            val kv = attribute.trim().split("=").filterNot { it.isBlank() }
            kv.first().trim().lowercase() to kv.last().trim()
        }


    @Throws(EnvelopeAuthenticity::class, SignatureMismatch::class)
    fun assertEnvelopeAuthenticity() {
        val headerData = mutableListOf<Byte>()

        headersOrder.lowercase().split(":").map { it.trim() }.filterNot { it.isBlank() }
            .forEach { headerKey ->
                if (CHECKSUM_HEADERS.contains(headerKey)) {
                    val value = envelopeHeadersMap[headerKey]
                    if (value != null) {
                        headerData.addAll(value.toByteArray().toList())
                    }
                }
            }

        // Verify headers checksum
        val (headersSum, headersSumBytes) = headerData.toByteArray().hashedWithSha256()

        if (headersChecksum != headersSum) {
            throw EnvelopeAuthenticity(headersChecksum)
        }

        // Verify checksum signature
        headersSignature.takeIf { it.isNotEmpty() }?.let { signature ->
            val isSignatureValid = verifySignature(
                publicKey = Key.fromBase64String(contact.publicSigningKey),
                signature = signature,
                originData = headersSumBytes
            )

            if (!isSignatureValid) {
                throw SignatureMismatch(signature)
            }
        }
    }
}
