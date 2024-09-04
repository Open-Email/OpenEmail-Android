package com.mercata.pingworks

import android.content.Context
import android.util.Log
import com.mercata.pingworks.models.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class Downloader(val context: Context) {

    companion object {
        const val FOLDER_NAME = "messages"
    }

    init {
        File(context.filesDir, FOLDER_NAME).delete()
    }

    suspend fun downloadFilesAndGetFolder(
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
                                val file = File(folder, envelope.contentHeaders.messageID)
                                file.createNewFile()

                                val out = FileOutputStream(file)
                                stream.copyTo(out)
                                out.close()
                                rv.add(envelope to file.readText())
                            }
                        }
                    }
                }
            }.joinAll()
        }

        return rv
    }
}
