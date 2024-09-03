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
import com.mercata.pingworks.decryptAnonymous
import com.mercata.pingworks.decrypt_xchacha20poly1305
import com.mercata.pingworks.exceptions.AlgorithmMissMatch
import com.mercata.pingworks.exceptions.BadChecksum
import com.mercata.pingworks.exceptions.BadChunkSize
import com.mercata.pingworks.exceptions.BadMessageId
import com.mercata.pingworks.exceptions.EnvelopeAuthenticity
import com.mercata.pingworks.exceptions.FingerprintMismatch
import com.mercata.pingworks.exceptions.SignatureMismatch
import com.mercata.pingworks.exceptions.TooLargeEnvelope
import com.mercata.pingworks.generateLink
import com.mercata.pingworks.hashedWithSha256
import com.mercata.pingworks.parseDate
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.verifySignature
import okhttp3.Headers
import java.time.ZonedDateTime

class Envelope(
    messageId: String,
    private val currentUser: UserData,
    val contact: DBContact,
    headers: Headers
) {

    private val envelopeHeadersMap = headers.associate { it.first to it.second }
    private val streamId: String?
    private val accessLinks: String?
    private var accessKey: Key? = null
    private val contentHeader: String
    private val headersOrder: String
    private val headersChecksum: String
    private val headersSignature: String
    private val payloadCipher: String?
    private val payloadCipherInfo: PayloadSeal?
    private var contentHeaders: ContentHeaders? = null

    /*  date=Tue, 03 Sep 2024 12:54:31 GMT
        message-checksum=algorithm=sha256; order=message-headers:message-id; value=f64715b249e3d33bb4e19e0b4e25ab779810469cbdc15ecd17b41bd5dd35e626
        message-headers=value=aWQ6IDcwZjM1ODk4MGIyMzBlMzg1NmFiZjQxNWI4ZDUxOGUxMmY4YzExZjY1MDgyNzFiNzI3YjBjMzZiNGE2YmMzYjAKYXV0aG9yOiBhbnRvbi5ha2ltY2hlbmtvQHBpbmcud29ya3MKc2l6ZTogMTIKY2hlY2tzdW06IGFsZ29yaXRobT1zaGEyNTY7IHZhbHVlPWI0NzVjOTUyY2VhYzZmMjIyNGI0NjdhMzFiNmNjMmU3ZTRmMTFiMmEwM2M0NWQwNDhlZjZmZjg2ZmNmY2U0ZjMKY2F0ZWdvcnk6IHBlcnNvbmFsCmRhdGU6IDIwMjQtMDgtMTNUMTU6NTY6NTRaCnN1YmplY3Q6IEJyb2FkY2FzdGluZyB0aXRsZQpzdWJqZWN0LWlkOiA3MGYzNTg5ODBiMjMwZTM4NTZhYmY0MTViOGQ1MThlMTJmOGMxMWY2NTA4MjcxYjcyN2IwYzM2YjRhNmJjM2Iw
        message-id=70f358980b230e3856abf415b8d518e12f8c11f6508271b727b0c36b4a6bc3b0
        message-signature=algorithm=ed25519; value=mmrdx/XJV8Ifm7M2am3BtIgRBRoYoMo2pshjjvwgPyP39XJDL1XlwAZVG71byu5ynodpMOODCYOdoCO9TNpbDw==; id=Upxi
        report-to={"endpoints":[{"url":"https:\/\/a.nel.cloudflare.com\/report\/v4?s=i5M9AVevYhTYM6njpnqxF2I80MdVCAHtrMxTvQ8TRL6TL6VeZUUh9fNqOpvcwNlomzegcFyjlXOBHm15DfvKlyAQ4V5NDaybm1ZpqoWbR0cl1pbX3nwRcizSi6s2he5ZQEE%3D"}],"group":"cf-nel","max_age":604800}
        nel={"success_fraction":0,"report_to":"cf-nel","max_age":604800}
        vary=Accept-Encoding
        server=cloudflare
        cf-ray=8bd5e389cdd095a9-TBS
        alt-svc=h3=":443"; ma=86400*/

    init {
        if (envelopeHeadersMap.entries.any { it.key.length + it.value.length > MAX_HEADERS_SIZE }) {
            throw TooLargeEnvelope(envelopeHeadersMap.toString())
        }

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

        envelopeHeadersMap[HEADER_MESSAGE_HEADERS]!!.let { headerVal ->
            val contentHeaderMap = parseHeaderAttributes(headerVal)
            contentHeaderMap["algorithm"]?.let { algorithm ->
                if (algorithm != SYMMETRIC_CIPHER) {
                    throw AlgorithmMissMatch(algorithm)
                }
            }
            contentHeader = contentHeaderMap["value"]!!
        }

        envelopeHeadersMap[HEADER_MESSAGE_ID]!!.let { headerVal ->
            if (messageId != headerVal) {
                throw BadMessageId(messageId + "\n" + headerVal)
            }
        }

        streamId = envelopeHeadersMap[HEADER_MESSAGE_STREAM]

        headersSignature = envelopeHeadersMap[HEADER_MESSAGE_ENVELOPE_SIGNATURE]!!.let { headerVal ->
            val sigMap = parseHeaderAttributes(headerVal)
            val algorithm = sigMap["algorithm"]
            val data = sigMap["value"]
            if (algorithm?.lowercase() != SIGNING_ALGORITHM) {
                throw AlgorithmMissMatch(algorithm.toString())
            }
            data!!
        }

        payloadCipher = envelopeHeadersMap[HEADER_MESSAGE_ENCRYPTION]

        payloadCipherInfo = if (payloadCipher == null) null else cipherInfoFromHeader(payloadCipher)

        accessLinks = envelopeHeadersMap[HEADER_MESSAGE_ACCESS]
        accessLinks?.run { parseAccessLink() }
    }

    @Throws(FingerprintMismatch::class)
    private fun parseAccessLink() {
        val connectionLink = currentUser.address.generateLink()

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
            this.accessKey = Key.fromBase64String(decryptAnonymous(value, currentUser))
        }
    }

    fun openContentHeaders() {
        if (isBroadcast()) {
            this.contentHeaders = contentFromHeaders(contentHeader)
        } else {
            contentHeaders = contentFromHeaders(
                decrypt_xchacha20poly1305(contentHeader, accessKey!!)
            )
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
                        .split(":")
                        .map { it.trim() }
                        .filterNot { it.isBlank() }

                    parts.first() to parts.last()
                }


        val parsedFiles = parseFilesHeader(headersMap[HEADER_CONTENT_FILES]!!)
        return ContentHeaders(
            messageID = headersMap[HEADER_CONTENT_MESSAGE_ID]!!,
            date = headersMap[HEADER_CONTENT_DATE]!!.parseDate(),
            subject = headersMap[HEADER_CONTENT_SUBJECT]!!,
            subjectId = headersMap[HEADER_CONTENT_SUBJECT_ID]!!,
            parentId = headersMap[HEADER_CONTENT_PARENT_ID],
            files = parsedFiles.second,
            filesHeader = headersMap[HEADER_CONTENT_FILES]!!,
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
            contentHeadersText = headerText
        )
    }

    fun parseFilesHeader(filesHeader: String): Pair<List<MessageFilePartInfo>, List<MessageFileInfo>> {
        val fileStrings = filesHeader.split(",")
        val fileParts = mutableListOf<MessageFilePartInfo>()

        for (fileString in fileStrings) {
            val urlInfoDict = mutableMapOf<String, String>()
            var messageId: String? = null
            var part: Long = 0
            var totalParts: Long = 0
            var modifiedAt: ZonedDateTime = ZonedDateTime.now()

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
                            value.parseDate()
                        }
                    }
                }
            }

            if (messageId != null && urlInfoDict["name"] != null && urlInfoDict["type"] != null && urlInfoDict["size"] != null) {
                val name = urlInfoDict["name"]!!
                val mimeType = urlInfoDict["type"]!!
                val size = urlInfoDict["size"]!!.toLongOrNull() ?: 0L
                val urlInfo = URLInfo(
                    url = null,
                    name = name,
                    mimeType = mimeType,
                    size = size,
                    modifiedAt = modifiedAt
                )
                val fileInfo = MessageFilePartInfo(
                    urlInfo = urlInfo,
                    messageId = messageId,
                    part = part,
                    size = part,
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

    fun isBroadcast(): Boolean = accessLinks.isNullOrBlank()

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
        header.split(";").associate {
            val kv = it.trim().split("=")
            kv.first().trim().lowercase() to kv.last().trim()
        }

    @Throws(EnvelopeAuthenticity::class, SignatureMismatch::class)
    fun assertEnvelopeAuthenticity() {
        var data = ""

        headersOrder.lowercase().split(":").map { it.trim() }.forEach { headerKey ->
            if (CHECKSUM_HEADERS.contains(headerKey)) {

                data += headerKey
            }
        }

        val headerSum = data.hashedWithSha256() //headerSum.first == headerCheckSum == "376c8e1cca087ccfb390bac289de54a55231714b603ba0e1b89708bcf3f66449"
        if (headersChecksum != headerSum.first) {
            throw EnvelopeAuthenticity(headerSum.first)
        }

        if (!verifySignature(
                signature = headersSignature.toByteArray(),
                message = headerSum.second,
                publicKey = currentUser.signingKeys.pair.publicKey
            )
        ) {
            throw SignatureMismatch(headersSignature ?: "")
        }
    }

}
