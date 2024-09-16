package com.mercata.pingworks.utils

import android.content.Context
import android.util.Log
import com.mercata.pingworks.db.messages.DBAttachment
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.registration.UserData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.text.Charsets.UTF_8


class Downloader(val context: Context) {

    companion object {
        const val FOLDER_NAME = "messages"
    }

    fun getDownloadedAttachmentsForMessage(messageWithDBAttachments: DBMessageWithDBAttachments): Map<DBAttachment, AttachmentResult> {
        val folder = File(context.filesDir, FOLDER_NAME)
        val downloaded: Array<File> = folder.listFiles() ?: arrayOf()
        val rv = hashMapOf<DBAttachment, AttachmentResult>()
        messageWithDBAttachments.attachments.forEach { attachment ->
            downloaded.firstOrNull { file -> file.name == attachment.name }?.let { file ->
                rv[attachment] = AttachmentResult(file, 100)
            }
        }
        return rv
    }

    fun deleteAttachmentsForMessages(messages: List<DBMessageWithDBAttachments>) {
        val folder = File(context.filesDir, FOLDER_NAME)
        messages.fold(initial = arrayListOf<DBAttachment>(), operation = { initial, new ->
            initial.apply { addAll(new.attachments) }
        }).forEach { attachment ->
            File(folder, attachment.name).delete()
        }
    }

    data class AttachmentResult(val file: File?, val percentage: Int)

    //emits -1 on error
    fun downloadAttachment(
        currentUser: UserData,
        attachment: DBAttachment,
    ): Flow<AttachmentResult> = flow {
        val folder = File(context.filesDir, FOLDER_NAME)
        folder.mkdirs()
        when (val call = safeApiCall {
            downloadMessage(
                currentUser,
                attachment.authorAddress,
                attachment.attachmentMessageId
            )
        }) {
            is HttpResult.Error -> {
                Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
                emit(AttachmentResult(null, -1))
            }

            is HttpResult.Success -> {
                call.data?.byteStream()?.let { stream ->

                    var bytesCopied = 0L
                    val onePercent = attachment.size / 100
                    val file = File(folder, attachment.name)
                    if (file.exists()) {
                        file.delete()
                    }
                    file.createNewFile()

                    val buffer = ByteArray(1024) // 1KB buffer

                    var bytesRead: Int
                    val outputStream = ByteArrayOutputStream()
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        emit(AttachmentResult(null, (bytesCopied / onePercent).toInt()))
                    }

                    val allBytes = outputStream.toByteArray()

                    file.writeBytes(
                        if (attachment.accessKey == null) {
                            allBytes
                        } else {
                            decrypt_xchacha20poly1305(
                                allBytes,
                                attachment.accessKey
                            )
                        }
                    )

                    outputStream.close()
                    emit(AttachmentResult(file, 100))
                }
            }
        }
    }

    suspend fun downloadMessagesPayload(
        envelopes: List<Envelope>
    ): List<Deferred<Pair<Envelope, String?>>> {
        return withContext(Dispatchers.IO) {
            envelopes.map { envelope ->
                async {
                    when (val call = safeApiCall {
                        downloadMessage(
                            envelope.currentUser,
                            envelope.contact,
                            envelope.contentHeaders.messageID
                        )
                    }) {
                        is HttpResult.Error -> {
                            Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
                            envelope to null
                        }

                        is HttpResult.Success -> {
                            call.data?.byteStream()?.let { stream ->
                                val buffer = ByteArray(1024) // 1KB buffer
                                val outputStream = ByteArrayOutputStream()

                                var bytesRead: Int
                                while (stream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }

                                val allBytes = outputStream.toByteArray()

                                val result = String(
                                    if (envelope.accessKey == null) {
                                        allBytes
                                    } else {
                                        decrypt_xchacha20poly1305(
                                            allBytes,
                                            envelope.accessKey!!
                                        )
                                    }, UTF_8
                                )

                                outputStream.close()

                                envelope to result
                            } ?: (envelope to null)
                        }
                    }
                }
            }
        }
    }
}
