package com.mercata.pingworks

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun Address.getHost(): String = this.substringAfter("@")
fun Address.getLocal(): String = this.substringBefore("@")
fun String.parseDate(): ZonedDateTime =
    ZonedDateTime.parse(this, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z"))