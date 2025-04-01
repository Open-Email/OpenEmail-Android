package com.mercata.openemail.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.mercata.openemail.BUFFER_SIZE
import com.mercata.openemail.BuildConfig
import com.mercata.openemail.URI_CACHED_FOLDER_NAME
import com.mercata.openemail.models.URLInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.file.Files
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class FileUtils(val context: Context) {


    companion object {
        const val IMAGE_FOLDER_NAME = "images"
    }

    @OptIn(ExperimentalUuidApi::class)
    fun createImageFile(): File {
        val folder = File(context.filesDir, IMAGE_FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "${Uuid.random()}.jpg")
        file.createNewFile()
        return file
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider", // authority should match the one defined in the manifest
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

    fun Uri.getBitmapFromUri(): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(this)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun Bitmap.resizeImageToMaxSize(maxSize: Int): Bitmap {
        val width = this.width
        val height = this.height

        val squareSize = minOf(width, height)

        val xOffset = (width - squareSize) / 2
        val yOffset = (height - squareSize) / 2

        val croppedBitmap = Bitmap.createBitmap(this, xOffset, yOffset, squareSize, squareSize)

        return if (squareSize > maxSize) {
            Bitmap.createScaledBitmap(croppedBitmap, maxSize, maxSize, true)
        } else {
            croppedBitmap
        }
    }

    fun Bitmap.compressBitmap(quality: Int = 75): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    @Throws(IOException::class)
    fun sha256fileSum(
        uri: Uri,
        fromOffset: Long = 0,
        bytesCount: Int = 0
    ): Triple<String, ByteArray, Long> {
        var processedBytes: Long = 0
        val bufferSize = 1024 * 1024
        val messageDigest = MessageDigest.getInstance("SHA-256")

        // Open an InputStream using ContentResolver for the given Uri
        getInputStreamFromUri(uri)?.use { inputStream ->
            skipFully(inputStream, fromOffset) // Move to the correct offset

            DigestInputStream(inputStream, messageDigest).use { dis ->
                val buffer = ByteArray(bufferSize)
                var bytesToRead: Long = if (bytesCount > 0) bytesCount.toLong() else Long.MAX_VALUE

                while (bytesToRead > 0) {
                    val maxReadSize = minOf(bufferSize.toLong(), bytesToRead)
                    if (maxReadSize <= 0) break

                    val bytesRead = dis.read(buffer, 0, maxReadSize.toInt())
                    if (bytesRead == -1) break  // End of stream

                    processedBytes += bytesRead
                    bytesToRead -= bytesRead
                }
            }
        } ?: throw IOException("Unable to open InputStream for URI")

        // Finalize the digest and convert to hex string
        val digestBytes = messageDigest.digest()
        val hashString = digestBytes.joinToString("") { "%02x".format(it) }

        return Triple(hashString, digestBytes, processedBytes)
    }

    @Throws(IOException::class)
    private fun skipFully(inputStream: InputStream, bytesToSkip: Long) {
        context
        var remaining = bytesToSkip
        while (remaining > 0) {
            val skipped = inputStream.skip(remaining)
            if (skipped <= 0) throw IOException("Failed to skip bytes in InputStream")
            remaining -= skipped
        }
    }

    fun encryptFilePartXChaCha20Poly1305(
        inputUri: Uri,
        secretKey: ByteArray,
        bytesCount: Long,
        offset: Long
    ): ByteArray? {
        getInputStreamFromUri(inputUri)?.use { stream ->
            // Skip to the specified offset
            stream.skip(offset)

            val unencrypted = ByteArray(bytesCount.toInt())

            val buffer = ByteArray(BUFFER_SIZE)

            var offsetRead = 0
            var byteRead: Int

            // Read until we've read bytesCount or reached the end of the stream
            while (offsetRead < bytesCount) {
                val bytesToRead = minOf(BUFFER_SIZE, (bytesCount - offsetRead).toInt())
                byteRead = stream.read(buffer, 0, bytesToRead)

                if (byteRead == -1) break  // End of stream

                System.arraycopy(
                    buffer.copyOfRange(0, byteRead),
                    0,
                    unencrypted,
                    offsetRead,
                    byteRead
                )

                offsetRead += byteRead
            }

            return encrypt_xchacha20poly1305(unencrypted, secretKey)
        }

        return null
    }

    fun getAllBytesForUri(uri: Uri, offset: Long, bytesCount: Long): ByteArray? {
        getInputStreamFromUri(uri)?.use { stream ->
            // Skip to the specified offset
            stream.skip(offset)

            val unencrypted = ByteArray(bytesCount.toInt())

            val buffer = ByteArray(BUFFER_SIZE)

            var offsetRead = 0
            var byteRead: Int

            // Read until we've read bytesCount or reached the end of the stream
            while (offsetRead < bytesCount) {
                val bytesToRead = minOf(BUFFER_SIZE, (bytesCount - offsetRead).toInt())
                byteRead = stream.read(buffer, 0, bytesToRead)

                if (byteRead == -1) break  // End of stream

                System.arraycopy(
                    buffer.copyOfRange(0, byteRead),
                    0,
                    unencrypted,
                    offsetRead,
                    byteRead
                )

                offsetRead += byteRead
            }

            return unencrypted
        }
        return null
    }

    fun copyUriToInternalStorage(uri: Uri): File? {
        return try {
            val uriCacheFolder = File(context.cacheDir, URI_CACHED_FOLDER_NAME)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(uriCacheFolder, uri.getNameFromURI(context) ?: System.currentTimeMillis().toString())
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    fun getInputStreamFromUri(uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)

    private fun String.encodeHeaderValue() = URLEncoder.encode(this, Charsets.UTF_8.name()) ?: ""
}