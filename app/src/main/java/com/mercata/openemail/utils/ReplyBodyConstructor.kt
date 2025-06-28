package com.mercata.openemail.utils

import android.content.Context
import com.mercata.openemail.DEFAULT_DATE_TIME_FORMAT
import com.mercata.openemail.R
import com.mercata.openemail.db.messages.DBMessage
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ReplyBodyConstructor(val context: Context) {
    suspend fun getReplyBody(replyMessage: DBMessage): String {
        val time: String? = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(replyMessage.timestamp), ZoneId.systemDefault()
        ).format(DEFAULT_DATE_TIME_FORMAT)
        return String.format(
            context.getString(R.string.reply_header),
            time,
            replyMessage.getAuthorPublicData()?.fullName ?: "",
            replyMessage.textBody,
            replyMessage.subject
        )
    }
}