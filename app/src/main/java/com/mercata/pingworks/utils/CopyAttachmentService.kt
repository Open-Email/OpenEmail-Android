package com.mercata.pingworks.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class CopyAttachmentService(val context: Context) {

    fun copyUriToLocalStorage(uri: Uri, outputFileName: String): File? {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            val outputDir = context.filesDir
            val outputFile = File(outputDir, outputFileName)

            outputFile.delete()
            outputFile.createNewFile()

            FileOutputStream(outputFile).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }

            inputStream?.close()

            return outputFile
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

}