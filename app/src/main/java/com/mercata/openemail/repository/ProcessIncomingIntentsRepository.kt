package com.mercata.openemail.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.mercata.openemail.URI_CACHED_FOLDER_NAME
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.getMimeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class ProcessIncomingIntentsRepository(private val context: Context, private val fileUtils: FileUtils) {

    private val _cachedUris = MutableStateFlow<List<Uri>>(listOf())
    val cachedUris: StateFlow<List<Uri>> = _cachedUris

    fun processNewIntent(intent: Intent) {
        val uris: ArrayList<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        } ?: arrayListOf()

        if (uris.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    uris.add(it)
                }
            } else {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    uris.add(it)
                }
            }
        }

        // If the intent contains the permission flag, try to take persistable permissions
        if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace() // Log the issue or notify the user as needed
                }
            }
        }

        val fileUris = uris.filter { it.getMimeType(context) != null }
            .takeIf { it.isNotEmpty() } ?: return

        val uriCacheFolder = File(context.cacheDir, URI_CACHED_FOLDER_NAME)
        uriCacheFolder.delete()
        uriCacheFolder.mkdirs()

        val files : List<Uri> =
            fileUris.mapNotNull { fileUtils.copyUriToInternalStorage(it)?.toUri() }

        _cachedUris.update { files }

    }

    fun clear() {
        _cachedUris.update { listOf() }
    }
}