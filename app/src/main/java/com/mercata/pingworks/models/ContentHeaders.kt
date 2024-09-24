package com.mercata.pingworks.models

import com.mercata.pingworks.CHECKSUM_ALGORITHM
import com.mercata.pingworks.HEADER_CONTENT_AUTHOR
import com.mercata.pingworks.HEADER_CONTENT_CATEGORY
import com.mercata.pingworks.HEADER_CONTENT_CHECKSUM
import com.mercata.pingworks.HEADER_CONTENT_DATE
import com.mercata.pingworks.HEADER_CONTENT_MESSAGE_ID
import com.mercata.pingworks.HEADER_CONTENT_PARENT_ID
import com.mercata.pingworks.HEADER_CONTENT_READERS
import com.mercata.pingworks.HEADER_CONTENT_SIZE
import com.mercata.pingworks.HEADER_CONTENT_SUBJECT
import com.mercata.pingworks.HEADER_CONTENT_SUBJECT_ID
import com.mercata.pingworks.utils.Address
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ContentHeaders(
    val messageID: String,
    val date: Instant,
    val subject: String,
    val subjectId: String?,
    val parentId: String?,
    val files: List<MessageFileInfo>? = null,
    val filesHeader: String? = null,
    val fileParts: List<MessageFilePartInfo>? = null,
    val category: MessageCategory,
    val size: Long,
    val checksum: String,
    val authorAddress: Address,
    val readersAddresses: List<Address>,
) {
    val contentHeadersText: String
    private val contentHeadersMap: HashMap<String, String> = hashMapOf(
        HEADER_CONTENT_MESSAGE_ID to messageID,
        HEADER_CONTENT_AUTHOR to authorAddress,
        HEADER_CONTENT_SIZE to size.toString(),
        HEADER_CONTENT_CHECKSUM to "algorithm=$CHECKSUM_ALGORITHM; value=${checksum}",
        HEADER_CONTENT_CATEGORY to MessageCategory.personal.toString(),
        HEADER_CONTENT_DATE to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
            .withZone(ZoneId.of("UTC")).format(date),
        HEADER_CONTENT_SUBJECT to subject,
        HEADER_CONTENT_READERS to readersAddresses.joinToString(separator = ", "),
    )

    init {
        parentId?.let {
            contentHeadersMap[HEADER_CONTENT_SUBJECT_ID] = parentId
            contentHeadersMap[HEADER_CONTENT_PARENT_ID] = parentId
        } ?: run {
            contentHeadersMap[HEADER_CONTENT_SUBJECT_ID] = messageID
        }

        contentHeadersText =
            contentHeadersMap.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }
}
