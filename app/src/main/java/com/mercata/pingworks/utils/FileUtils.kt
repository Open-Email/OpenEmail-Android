package com.mercata.pingworks.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.mercata.pingworks.models.URLInfo
import java.io.ByteArrayOutputStream
import java.io.File
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

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // authority should match the one defined in the manifest
            file
        )
    }

    fun getFileFromUri(uri: Uri): File? {
        if (uri.authority == "${context.packageName}.fileprovider") {
            // Assuming the file is stored in the app's internal storage
            val fileName = uri.lastPathSegment
            return fileName?.let { File(context.filesDir, it) }
        }
        return null
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
        chunkBytes: ByteArray
    ): Pair<String, ByteArray> {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(chunkBytes)
        val bs = md.digest()
        return bs.joinToString("") { "%02x".format(it) } to bs
    }


    fun encryptFilePartXChaCha20Poly1305(
        inputUri: Uri,
        secretKey: ByteArray,
        bytesCount: Long,
        offset: Long
    ): ByteArray? {
        context.contentResolver.openInputStream(inputUri)?.use { stream ->
            // Skip to the specified offset
            stream.skip(offset)
            val encrypted = ByteArray(bytesCount.toInt())

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)


            var offsetRead = 0

            var byteRead: Int

                                                         //TODO check <=
            while ((stream.read(buffer).also { byteRead = it }) < bytesCount.toInt()) {
                encrypt_xchacha20poly1305(buffer, secretKey)?.let { src ->
                    System.arraycopy(src, 0, encrypted, offsetRead, src.size)
                }
                offsetRead += byteRead
            }

            return encrypted
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

                    val dateModifiedInSeconds = try {
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_modified"))
                    } catch (e: IllegalArgumentException) {
                        null
                    }

                    dateModifiedInSeconds?.let {
                        fileLastModificationTime = Instant.ofEpochMilli(it * 1000L)
                    }

                    size = try {
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    } catch (e: IllegalArgumentException) {
                        null
                    }
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
            size = size ?: 0L,
            modifiedAt = fileLastModificationTime
        )
    }


    private fun String.encodeHeaderValue() = URLEncoder.encode(this, Charsets.UTF_8.name()) ?: ""
}