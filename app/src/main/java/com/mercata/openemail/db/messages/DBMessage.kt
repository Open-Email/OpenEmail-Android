package com.mercata.openemail.db.messages

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.contacts.toDraftReader
import com.mercata.openemail.db.drafts.DBDraft
import com.mercata.openemail.db.drafts.DBDraftWithReaders
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.UUID

@Entity
/*(
    foreignKeys = [
        ForeignKey(
            entity = DBContact::class,
            parentColumns = ["address"],
            childColumns = ["author_address"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)*/
data class DBMessage(
    @PrimaryKey @ColumnInfo("message_id") val messageId: String,
    @ColumnInfo("author_address", index = true) val authorAddress: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
    @ColumnInfo("is_unread") val isUnread: Boolean,
    @ColumnInfo("marked_to_delete") val markedToDelete: Boolean,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("reader") val readerAddresses: String? // joined to string with "," separator
) {
    @Ignore
    private var userData: PublicUserData? = null

    suspend fun getAuthorPublicData(): PublicUserData? {
        if (userData == null) {
            userData = when (val call = safeApiCall { getProfilePublicData(authorAddress) }) {
                is HttpResult.Error -> null
                is HttpResult.Success -> call.data
            }
        }

        return userData
    }
}

suspend fun DBMessageWithDBAttachments.toDraftWithReaders(
    downloadRepository: DownloadRepository,
    sp: SharedPreferences,
    fileUtils: FileUtils,
    db: AppDatabase
): DBDraftWithReaders? {
    val currentUser = sp.getUserData() ?: return null

    return withContext(Dispatchers.IO) {
        val downloadResults: List<Uri?> = this@toDraftWithReaders.getFusedAttachments().map {
            async {
                downloadRepository
                    .downloadAttachment(currentUser, it)
                    .filter { attachmentResult -> attachmentResult.file != null }
                    .first()
                    .let { attachmentResult -> fileUtils.getUriForFile(attachmentResult.file!!) }
            }
        }.awaitAll()

        val draftId = UUID.randomUUID().toString()
        val draft = DBDraft(
            draftId = draftId,
            attachmentUriList = downloadResults.filterNotNull().joinToString(separator = ",") { it.toString() }.takeIf { it.isNotEmpty() },
            subject = this@toDraftWithReaders.message.subject,
            textBody = this@toDraftWithReaders.message.textBody,
            isBroadcast = this@toDraftWithReaders.message.isBroadcast,
            timestamp = System.currentTimeMillis(),
            readerAddresses = this@toDraftWithReaders.message.readerAddresses
        )
        val readers = this@toDraftWithReaders.message.readerAddresses?.split(",")
            ?.mapNotNull { address ->
                db.userDao().findByAddress(address)?.toDraftReader(draftId)
            } ?: listOf()
        DBDraftWithReaders(draft = draft, readers = readers)
    }
}