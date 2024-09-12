package com.mercata.pingworks.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.mercata.pingworks.db.messages.DBAttachment
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.registration.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.text.Charsets.UTF_8

class Downloader(val context: Context) {

    companion object {
        const val FOLDER_NAME = "messages"
    }

    init {
        File(context.filesDir, FOLDER_NAME).delete()
    }

    data class AttachmentResult(val uri: Uri?, val percentage: Int, val type: String)

    //throws -1 on error
    fun downloadAttachment(
        currentUser: UserData,
        attachment: DBAttachment,
        coroutineScope: CoroutineScope
    ): StateFlow<AttachmentResult> {
        val folder = File(context.filesDir, FOLDER_NAME)
        folder.mkdirs()
        val rv = MutableStateFlow(AttachmentResult(null, 0, attachment.type))
        coroutineScope.launch {
            when (val call = safeApiCall {
                downloadMessage(
                    currentUser,
                    attachment.authorAddress,
                    attachment.attachmentMessageId
                )
            }) {
                is HttpResult.Error -> {
                    Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
                    rv.value = AttachmentResult(null, -1, attachment.type)
                }

                is HttpResult.Success -> {
                    call.data?.byteStream()?.let { stream ->
                        //val sizeInputStream = SizeInputStream(stream, attachment.size)

                        var bytesCopied = 0L
                        val onePercent = attachment.size / 100
                        val file = File(folder, attachment.attachmentMessageId)
                        file.createNewFile()


                        val buffer = ByteArray(1024) // 1KB buffer
                        val outputStream = FileOutputStream(file)

                        var bytesRead: Int
                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead

                            rv.value = AttachmentResult(
                                null,
                                (bytesCopied / onePercent).toInt(),
                                attachment.type
                            )
                        }
                        outputStream.close()

                        rv.value = AttachmentResult(
                            file.toUri(),
                            100,
                            attachment.type
                        )
                    }
                }
            }
        }
        return rv
    }

    suspend fun downloadMessagesPayload(
        envelopes: List<Envelope>
    ): List<Deferred<Pair<Envelope, String?>>> {
        //val folder = File(context.filesDir, FOLDER_NAME)
        //folder.mkdirs()
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
