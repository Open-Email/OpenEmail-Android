package com.mercata.pingworks.db.archive

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.archive.archive_readers.DBArchivedReader
import com.mercata.pingworks.db.archive.archive_readers.toPublicUserData
import com.mercata.pingworks.models.PublicUserData

data class DBArchiveWithReaders(
    @Embedded val archive: DBArchivedMessage,
    @Relation(
        parentColumn = "archive_id",
        entityColumn = "archive_id"
    )
    val readers: List<DBArchivedReader>
) : HomeItem {
    override fun getContacts(): List<PublicUserData> = readers.map { it.toPublicUserData() }

    override fun getTitle(): String = getContacts().firstOrNull()?.fullName ?: ""

    override fun getAddressValue(): String? = archive.readerAddresses?.split(",")?.firstOrNull()

    override fun getSubtitle(): String = archive.subject

    override fun getTextBody(): String = archive.textBody

    override fun getMessageId(): String = archive.archiveId

    override fun getAttachmentsAmount(): Int? = archive.attachmentUrls?.split(",")?.size

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long = archive.timestamp
}