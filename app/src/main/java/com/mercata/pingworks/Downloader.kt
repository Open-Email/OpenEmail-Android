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

    suspend fun getDownloadLinksForAttachments(envelopes: List<Envelope>): List<Pair<Envelope, String>> {
        val rv: ArrayList<Pair<Envelope, String>> = arrayListOf()
        /*id: 5fee4e75c92e2f4d57935f3929379dd14fd8cf13e4b117bc5d734c411c994494
        author: anton@ping.works
        size: 4270977
        checksum: algorithm=sha256; value=d7ab0e9da6c594bb680e129ce2dd1612b6890c3a3984281d3e6a58148dcb7ca4
        category: personal
        date: 2024-09-02T13:14:58Z
        subject: Test broadcasting
        subject-id: ea86efac9d6706883eb231f264b409a9b86808196b0ed319eab84bb7f7da3c73
        files: name=DSCF2213.png;type=image/png;modified=2024-06-01T10:08:16Z;size=4270977;id=5fee4e75c92e2f4d57935f3929379dd14fd8cf13e4b117bc5d734c411c994494;part=1/1
        parent-id: ea86efac9d6706883eb231f264b409a9b86808196b0ed319eab84bb7f7da3c73*/
        return  rv
    }

    suspend fun downloadMessagesPayload(
        envelopes: List<Envelope>
    ): List<Pair<Envelope, String>> {
        //val folder = File(context.filesDir, FOLDER_NAME)
        //folder.mkdirs()
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
