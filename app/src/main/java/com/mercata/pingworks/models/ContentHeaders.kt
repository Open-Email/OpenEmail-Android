package com.mercata.pingworks.models

import com.mercata.pingworks.utils.Address
import java.time.ZonedDateTime

data class ContentHeaders(
    val messageID: String,
    val date: ZonedDateTime,
    val subject: String,
    val subjectId: String,
    val parentId: String?,
    val files: List<MessageFileInfo>,
    val filesHeader: String?,
    val fileParts: List<MessageFilePartInfo>,
    val category: MessageCategory,
    val size: Long,
    val checksum: String,
    val authorAddress: Address,
    val readersAddresses: List<Address>,
    val contentHeadersText: String,
) {

}
