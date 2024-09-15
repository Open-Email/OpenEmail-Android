package com.mercata.pingworks.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toFile
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

    fun getSha256SumFromUri(uri: Uri): Pair<String, ByteArray> {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val digest = MessageDigest.getInstance("SHA-256")

        val buffer = ByteArray(1024)
        var bytesRead: Int

        inputStream?.use { input ->
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val hashBytes = digest.digest()

        return hashBytes.joinToString("") { "%02x".format(it) } to hashBytes
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
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
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