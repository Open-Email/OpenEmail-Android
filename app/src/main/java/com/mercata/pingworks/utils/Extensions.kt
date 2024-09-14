package com.mercata.pingworks.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun Address.getHost(): String = this.substringAfter("@")
fun Address.getLocal(): String = this.substringBefore("@")
fun String.parseSimpleDate(): ZonedDateTime =
    ZonedDateTime.parse(this, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z"))

fun String.parseServerDate(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.parse(this), ZoneId.systemDefault())

fun Uri.getNameFromURI(context: Context): String {
    val c: Cursor = context.contentResolver.query(this, null, null, null, null)!!
    c.moveToFirst()
    val result =
        c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) }
            ?: ""
    c.close()
    return result
}