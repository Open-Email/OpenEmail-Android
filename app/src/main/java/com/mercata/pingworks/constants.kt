package com.mercata.pingworks

import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

val MESSAGE_LIST_ITEM_HEIGHT = 88.dp
val MARGIN_DEFAULT = 16.dp

val emailRegex = Regex("^[a-z0-9][a-z0-9\\.\\-_\\+]{2,}@[a-z0-9.-]+\\.[a-z]{2,}|xn--[a-z0-9]{2,}$")
const val DEFAULT_MAIL_SUBDOMAIN = "mail"
const val WELL_KNOWN_URI = ".well-known/mail.txt"
const val MAIL_HOST = "mail.ping.works"

val DEFAULT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")