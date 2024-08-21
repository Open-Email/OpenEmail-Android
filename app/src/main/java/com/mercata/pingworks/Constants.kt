package com.mercata.pingworks

import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

val MESSAGE_LIST_ITEM_HEIGHT = 88.dp
val MARGIN_DEFAULT = 16.dp

val emailRegex = Regex("^[a-z0-9][a-z0-9\\.\\-_\\+]{2,}@[a-z0-9.-]+\\.[a-z]{2,}|xn--[a-z0-9]{2,}$")
const val WELL_KNOWN_URI = ".well-known/mail.txt"
const val DEFAULT_MAIL_SUBDOMAIN = "mail"
const val CHECKSUM_ALGORITHM = "sha256"
const val SIGNING_ALGORITHM = "ed25519"
const val SYMMETRIC_CIPHER = "xchacha20poly1305"
const val NONCE_SCHEME = "SOTN"
const val NONCE_HEADER_VALUE_HOST = "host"
const val NONCE_HEADER_VALUE_KEY = "value"
const val NONCE_HEADER_ALGORITHM_KEY = "algorithm"
const val NONCE_HEADER_SIGNATURE_KEY = "signature"
const val NONCE_HEADER_PUBKEY_KEY = "key"
const val ANONYMOUS_ENCRYPTION_CIPHER = "curve25519xsalsa20poly1305"
const val headerFieldSeparator = "; "
const val headerKeyValueSeparator = "="
val availableHosts = listOf("ping.works")

val DEFAULT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")