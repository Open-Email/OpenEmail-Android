package com.mercata.pingworks.utils

import android.content.Context
import android.util.Log
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_ABYTES
import com.goterl.lazysodium.interfaces.AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES
import com.mercata.pingworks.BUFFER_SIZE
import com.mercata.pingworks.db.attachments.DBAttachment
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.messages.FusedAttachment
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.registration.UserData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.text.Charsets.UTF_8


class Downloader(val context: Context) {

    companion object {
        const val FOLDER_NAME = "messages"
    }

    fun getDownloadedAttachmentsForMessage(messageWithDBAttachments: DBMessageWithDBAttachments): Map<FusedAttachment, AttachmentResult> {
        val folder = File(context.filesDir, FOLDER_NAME)
        val downloaded: Array<File> = folder.listFiles() ?: arrayOf()
        val rv = hashMapOf<FusedAttachment, AttachmentResult>()
        messageWithDBAttachments.getAttachments().forEach { attachment ->
            downloaded.firstOrNull { file -> file.name == attachment.name && file.length() == attachment.fileSize }
                ?.let { file ->
                    rv[attachment] = AttachmentResult(file, 100)
                }
        }
        return rv
    }

    fun deleteAttachmentsForMessages(messages: List<DBMessageWithDBAttachments>) {
        val folder = File(context.filesDir, FOLDER_NAME)
        messages.fold(initial = arrayListOf<DBAttachment>(), operation = { initial, new ->
            initial.apply { addAll(new.attachmentParts) }
        }).forEach { attachment ->
            File(folder, attachment.name).delete()
        }
    }

    data class AttachmentResult(val file: File?, val percentage: Int)

    //emits -1 on error
    fun downloadAttachment(
        currentUser: UserData,
        attachment: FusedAttachment,
    ): Flow<AttachmentResult> = flow {
        val folder = File(context.filesDir, FOLDER_NAME)
        folder.mkdirs()
        if (attachment.parts.size == 1) {
            when (val call = safeApiCall {
                downloadMessage(
                    currentUser,
                    attachment.parts.first().authorAddress,
                    attachment.parts.first().attachmentMessageId
                )
            }) {
                is HttpResult.Error -> {
                    Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
                    emit(AttachmentResult(null, -1))
                }

                is HttpResult.Success -> {
                    call.data?.byteStream()
                        ?.dumpToFile(attachment.parts.first(), null, folder)
                        ?.collect { emit(it) }
                }
            }
        } else {
            emit(
                AttachmentResult(
                    null,
                    -2
                )
            )

            val decryptedParts: List<File?> = attachment.parts.mapIndexed { partIndex, attachment ->
                when (val call = safeApiCall {
                    downloadMessage(
                        currentUser,
                        attachment.authorAddress,
                        attachment.attachmentMessageId
                    )
                }) {
                    is HttpResult.Success -> {
                        call.data?.byteStream()?.use { input ->

                            val encryptedFile =
                                File(folder, "${partIndex}_${attachment.name}.encrypted")
                            if (encryptedFile.exists()) {
                                encryptedFile.delete()
                            }
                            encryptedFile.createNewFile()
                            FileOutputStream(encryptedFile).use { output ->
                                copyStream(input, output)
                            }

                            val decrypted = decrypt_xchacha20poly1305(
                                encryptedFile.readBytes(),
                                attachment.accessKey!!
                            )

                            encryptedFile.delete()

                            val decryptedFile = File(folder, "${partIndex}_${attachment.name}")
                            if (decryptedFile.exists()) {
                                decryptedFile.delete()
                            }
                            decryptedFile.createNewFile()
                            decryptedFile.writeBytes(decrypted)

                            decryptedFile
                        }
                    }

                    is HttpResult.Error -> {
                        Log.e("DOWNLOAD FILE", call.message ?: call.code.toString())
                        null
                    }
                }
            }

            if (decryptedParts.contains(null)) {
                emit(AttachmentResult(null, -1))
                return@flow
            }

            val fusedFile = File(folder, attachment.name)
            if (fusedFile.exists()) {
                fusedFile.delete()
            }
            fusedFile.createNewFile()

            FileOutputStream(fusedFile).use { outputStream ->
                decryptedParts.forEach {
                    FileInputStream(it).use { inputStream1 ->
                        inputStream1.copyTo(outputStream)
                    }
                    it?.delete()
                }
            }
            emit(AttachmentResult(fusedFile, 100))


            //TODO remove
            /*val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fusedFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/x-msvideo")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(uri).use { outputStream ->
                    fusedFile.inputStream().use { inputStream ->
                        val buffer = ByteArray(1024 * 1024) // Buffer size of 1 MB
                        var bytesRead: Int

                        // Read from the source and write to the output stream in chunks
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream?.write(buffer, 0, bytesRead)
                        }
                    }
                }
                Log.d("FileCopy", "Video copied successfully to Movies directory.")
            } ?: run {
                Log.e("FileCopy", "Failed to create URI in MediaStore.")
            }*/
        }
    }.flowOn(Dispatchers.IO)

    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(8 * 1024) // 8 KB buffer
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }

    private fun InputStream.dumpToFile(
        attachment: DBAttachment,
        partIndex: Int?,
        folder: File,
        decrypt: Boolean = true
    ): Flow<AttachmentResult> =
        flow {
            var bytesCopied = 0L
            val onePercent = attachment.fileSize / 100

            val buffer = ByteArray(BUFFER_SIZE)

            var bytesRead: Int

            //TODO check unencrypted broadcast attachments
            val encryptedByteArraySize =
                if (attachment.accessKey == null) {
                    attachment.fileSize.toInt()
                } else {
                    attachment.fileSize.toInt() + XCHACHA20POLY1305_IETF_NPUBBYTES + XCHACHA20POLY1305_IETF_ABYTES
                }
            val encrypted = ByteArray(encryptedByteArraySize)

            while (this@dumpToFile.read(buffer).also { bytesRead = it } != -1) {
                System.arraycopy(
                    buffer.copyOfRange(0, bytesRead),
                    0,
                    encrypted,
                    bytesCopied.toInt(),
                    bytesRead
                )

                bytesCopied += bytesRead

                emit(AttachmentResult(null, (bytesCopied / onePercent).toInt()))
            }

            val decrypted = if (attachment.accessKey == null || !decrypt) {
                encrypted
            } else {
                decrypt_xchacha20poly1305(
                    encrypted,
                    attachment.accessKey
                )
            }

            val fileName =
                if (partIndex == null)
                    attachment.name
                else
                    "${partIndex}_${attachment.name}"

            val file = File(folder, fileName)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            file.writeBytes(decrypted)
            emit(AttachmentResult(file, 100))
        }.flowOn(Dispatchers.IO)

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
