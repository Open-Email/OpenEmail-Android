package com.mercata.pingworks.db.drafts

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.drafts.draft_reader.DBDraftReader
import com.mercata.pingworks.db.drafts.draft_reader.toPublicUserData
import com.mercata.pingworks.models.PublicUserData

data class DBDraftWithReaders(
    @Embedded val draft: DBDraft,
    @Relation(
        parentColumn = "draft_id",
        entityColumn = "draft_id"
    )
    val readers: List<DBDraftReader>
) : HomeItem {
    override fun getContacts(): List<PublicUserData> = readers.map { it.toPublicUserData() }

    override fun getSubject(): String = draft.subject

    override fun getTextBody(): String = draft.textBody

    override fun getMessageId(): String = draft.draftId

    override fun getAttachmentsAmount(): Int? = draft.attachmentUriList?.split(",")?.size

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long = draft.timestamp
}
