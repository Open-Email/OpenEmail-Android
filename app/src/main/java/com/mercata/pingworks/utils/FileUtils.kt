package com.mercata.pingworks.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toFile
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.models.URLInfo
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant


class FileUtils(val context: Context) {

    fun getBytesFromUri(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val byteBuffer = ByteArrayOutputStream()

        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        var len: Int

        while ((inputStream!!.read(buffer).also { len = it }) != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        val result = byteBuffer.toByteArray()
        inputStream.close()
        return result
    }

    fun getFileChecksum(uri: Uri): Pair<String, ByteArray> {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val sha256 = MessageDigest.getInstance("SHA-256")

        val buffer = ByteArray(1024)
        var bytesRead: Int

        inputStream?.use { input ->
            while (input.read(buffer).also { bytesRead = it } != -1) {
                sha256.update(buffer, 0, bytesRead)
            }
        }

        val hashBytes = sha256.digest()

        return hashBytes.joinToString("") { "%02x".format(it) } to hashBytes
    }

    fun getFilePartChecksum(
        fileAtUri: Uri,
        fromOffset: Long,
        bytesCount: Long
    ): Triple<String, ByteArray, Long>? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(fileAtUri)
        val bufferSize = 1024 * 1024
        var processedBytes: Long = 0

        inputStream?.use { stream ->
            val sha256 = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(bufferSize)

            // Skip to the specified offset
            stream.skip(fromOffset)

            while (true) {
                // Calculate the number of bytes to read, not exceeding the remaining bytes
                var bytesToRead = bufferSize.toLong()
                if (bytesCount > 0) {
                    val remainingBytes = bytesCount.toLong() - processedBytes
                    bytesToRead = minOf(bytesToRead, remainingBytes)
                }

                if (bytesToRead > 0) {
                    val bytesRead = stream.read(buffer, 0, bytesToRead.toInt())
                    if (bytesRead == -1) break  // End of file reached

                    processedBytes += bytesRead.toLong()
                    sha256.update(buffer, 0, bytesRead)

                    if (processedBytes >= bytesCount) break
                } else {
                    break
                }
            }

            // Finalize the hash
            val digest = sha256.digest()
            val hexDigest = digest.joinToString("") { "%02x".format(it) }
            return Triple(hexDigest, digest, processedBytes)
        }
        return null
    }


    fun encryptFilePartXChaCha20Poly1305(
        inputUri: Uri,
        secretKey: ByteArray,
        bytesCount: Long?,
        offset: Long?
    ): ByteArray? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(inputUri)

        inputStream?.use { stream ->
            // Skip to the specified offset
            if (offset != null) {
                stream.skip(offset)
            }

            val dataToEncrypt: ByteArray
            if (bytesCount != null) {
                // Read the specified number of bytes
                dataToEncrypt = ByteArray(bytesCount.toInt())
                stream.read(dataToEncrypt, 0, dataToEncrypt.size)
            } else {
                // Read to the end of the file
                dataToEncrypt = stream.readBytes()
            }

            // Encrypt the data using XChaCha20-Poly1305
            return encrypt_xchacha20poly1305(dataToEncrypt, secretKey)
        }
        return null
    }

    fun getURLInfo(uri: Uri): URLInfo {
        var fileType: String? = null
        var size: Long? = null
        var encodedFilename: String? = null
        var fileLastModificationTime = Instant.now()

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    encodedFilename = cursor
                        .getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .let { cursor.getString(it) }.encodeHeaderValue()

                    val dateModifiedInSeconds =
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_modified"))
                    fileLastModificationTime = Instant.ofEpochMilli(dateModifiedInSeconds * 1000L)
                    size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                }
            }
            fileType = context.contentResolver.getType(uri)
        } else if (uri.scheme == "file") {
            val file = uri.toFile()
            size = file.length()
            fileType = Files.probeContentType(file.toPath())
            encodedFilename = file.name
            fileLastModificationTime = Instant.ofEpochMilli(file.lastModified())
        }


        return URLInfo(
            uri = uri,
            name = encodedFilename ?: "",
            mimeType = fileType ?: "application/octet-stream",
            size = size ?: 0,
            modifiedAt = fileLastModificationTime.toString()
        )
    }


    private fun String.encodeHeaderValue() = URLEncoder.encode(this, Charsets.UTF_8.name()) ?: ""
}