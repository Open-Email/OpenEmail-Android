package com.mercata.pingworks

import android.content.Context
import android.util.Log
import com.mercata.pingworks.models.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.text.Charsets.UTF_8

class Downloader(val context: Context) {

    companion object {
        const val FOLDER_NAME = "messages"
    }

    init {
        File(context.filesDir, FOLDER_NAME).delete()
    }

    suspend fun downloadMessagesPayload(
        envelopes: List<Envelope>
    ): ArrayList<Pair<Envelope, String>> {
        val folder = File(context.filesDir, FOLDER_NAME)
        folder.mkdirs()
        val rv: ArrayList<Pair<Envelope, String>> = arrayListOf()
        withContext(Dispatchers.IO) {
            envelopes.map { envelope ->
                launch {
                    when (val call = safeApiCall {
                        downloadMessage(
                            envelope.currentUser,
                            envelope.contact,
                            envelope.contentHeaders.messageID
                        )
                    }) {
                        is HttpResult.Error -> {
                            Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
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
                                        decrypt_xchacha20poly1305(allBytes, envelope.accessKey!!)
                                    }, UTF_8
                                )

                                rv.add(envelope to result)

                            }
                        }
                    }
                }
            }.joinAll()
        }

        return rv
    }
}
