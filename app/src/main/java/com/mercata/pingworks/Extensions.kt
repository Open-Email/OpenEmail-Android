package com.mercata.pingworks

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