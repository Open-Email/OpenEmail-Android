package com.mercata.openemail.models

import com.mercata.openemail.CHECKSUM_ALGORITHM
import com.mercata.openemail.HEADER_CONTENT_AUTHOR
import com.mercata.openemail.HEADER_CONTENT_CATEGORY
import com.mercata.openemail.HEADER_CONTENT_CHECKSUM
import com.mercata.openemail.HEADER_CONTENT_DATE
import com.mercata.openemail.HEADER_CONTENT_FILES
import com.mercata.openemail.HEADER_CONTENT_MESSAGE_ID
import com.mercata.openemail.HEADER_CONTENT_PARENT_ID
import com.mercata.openemail.HEADER_CONTENT_READERS
import com.mercata.openemail.HEADER_CONTENT_SIZE
import com.mercata.openemail.HEADER_CONTENT_SUBJECT
import com.mercata.openemail.HEADER_CONTENT_SUBJECT_ID
import com.mercata.openemail.utils.Address
import com.mercata.openemail.utils.toServerFormatString
import java.time.Instant

data class ContentHeaders(
    val messageID: String,
    val subjectId: String?,
    val date: Instant,
    val subject: String,
    val parentId: String?,
    val fileParts: List<MessageFilePartInfo>? = null,
    val category: MessageCategory,
    val size: Long,
    val checksum: String,
    val authorAddress: Address,
    val readersAddresses: List<Address>?,
) {
    private val filesHeader: String?
    val contentHeadersText: String
    private val contentHeadersMap: HashMap<String, String> = hashMapOf(
        HEADER_CONTENT_MESSAGE_ID to messageID,
        HEADER_CONTENT_AUTHOR to authorAddress,
        HEADER_CONTENT_SIZE to size.toString(),
        HEADER_CONTENT_CHECKSUM to "algorithm=$CHECKSUM_ALGORITHM; value=${checksum}",
        HEADER_CONTENT_CATEGORY to MessageCategory.personal.toString(),
        HEADER_CONTENT_DATE to date.toServerFormatString(),
        HEADER_CONTENT_SUBJECT to subject,

        )

    init {
        readersAddresses?.let {
            contentHeadersMap[HEADER_CONTENT_READERS] =
                it.filterNot { address -> address == authorAddress }.joinToString(separator = ", ")
        }

        parentId?.let {
            contentHeadersMap[HEADER_CONTENT_SUBJECT_ID] = it
            contentHeadersMap[HEADER_CONTENT_PARENT_ID] = it
        }

        subjectId?.let {
            contentHeadersMap[HEADER_CONTENT_SUBJECT_ID] = it
        }

        filesHeader = fileParts?.joinToString(", ") { serializeMessageFileInfo(it) }
            ?.takeIf { it.isNotBlank() }?.also {
            contentHeadersMap[HEADER_CONTENT_FILES] = it
        }

        contentHeadersText =
            contentHeadersMap.entries.joinToString("\n") { "${it.key}: ${it.value}" }

    }


    private fun serializeMessageFileInfo(info: MessageFilePartInfo): String {

        return listOf(
            "name=${info.urlInfo.name}",
            "type=${info.urlInfo.mimeType}",
            "modified=${info.urlInfo.modifiedAt.toServerFormatString()}",
            "size=${info.urlInfo.size}",
            "id=${info.messageId}",
            "part=${info.part}/${info.totalParts}"
        ).joinToString(separator = ";")
    }

}
