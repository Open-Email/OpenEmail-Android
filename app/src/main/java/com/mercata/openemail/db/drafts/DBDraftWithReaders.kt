package com.mercata.openemail.db.drafts

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.drafts.draft_reader.DBDraftReader
import com.mercata.openemail.db.drafts.draft_reader.toPublicUserData
import com.mercata.openemail.models.PublicUserData

data class DBDraftWithReaders(
    @Embedded val draft: DBDraft,
    @Relation(
        parentColumn = "draft_id",
        entityColumn = "draft_id"
    )
    val readers: List<DBDraftReader>
) : HomeItem {
    override suspend fun getContacts(): List<PublicUserData> = readers.map { it.toPublicUserData() }

    override suspend fun getTitle(): String = getContacts().firstOrNull()?.fullName ?: ""

    override fun getAuthorAddressValue(): String? = draft.readerAddresses?.split(",")?.firstOrNull()

    override fun getSubject(): String = draft.subject

    override fun getTextBody(): String = draft.textBody

    override fun getMessageId(): String = draft.draftId

    override fun getAttachmentsAmount(): Int? = draft.attachmentUriList?.split(",")?.size

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long = draft.timestamp
}
