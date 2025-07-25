package com.mercata.openemail

import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

val MESSAGE_LIST_ITEM_HEIGHT = 152.dp
val PROFILE_IMAGE_HEIGHT = 300.dp
val CONTACT_LIST_ITEM_HEIGHT = 72.dp
val ATTACHMENT_LIST_ITEM_HEIGHT = 64.dp
val CONTACT_LIST_ITEM_IMAGE_SIZE = 40.dp
val MESSAGE_LIST_ITEM_IMAGE_SIZE = 48.dp
val SETTING_LIST_ITEM_SIZE = 56.dp
val MARGIN_DEFAULT = 16.dp
val DEFAULT_LIST_ITEM_STATUS_ICON_SIZE = 16.dp
val DEFAULT_CORNER_RADIUS = 16.dp
val MARGIN_SMALLER = 12.dp
val CHIP_HEIGHT = 32.dp
val CHIP_ICON_SIZE = 24.dp
val CLEAR_ICON_SIZE = 24.dp
const val animationDuration = 230
const val snackBarDuration = 4000L
const val MAX_HEADERS_SIZE = 512 * 1024
const val MAX_MESSAGE_SIZE: Long = 64 * 1024 * 1024
const val MAX_CHUNK_SIZE: Int = 1048576
const val DEFAULT_CHUNK_SIZE: Int = 8192
const val MIN_CHUNK_SIZE: Int = 1024
const val BUFFER_SIZE: Int = 1024 * 1024
const val DISABLED_ALPHA: Float = 0.5f

val emailRegex = Regex("^[a-z0-9][a-z0-9\\.\\-_\\+]{2,}@[a-z0-9.-]+\\.[a-z]{2,}|xn--[a-z0-9]{2,}$")
const val TNC_LINK = "https://open.email/terms.txt"
const val WELL_KNOWN_URI = ".well-known/mail.txt"
const val SUPPORT_ADDRESS = "support@open.email"
const val DEFAULT_MAIL_SUBDOMAIN = "mail"
const val CHECKSUM_ALGORITHM = "sha256"
const val SIGNING_ALGORITHM = "ed25519"
const val SYMMETRIC_CIPHER = "xchacha20poly1305"
const val SYMMETRIC_FILE_CIPHER = "secretstream_xchacha20poly1305"
const val NONCE_SCHEME = "SOTN"
const val NONCE_HEADER_VALUE_HOST = "host"
const val NONCE_HEADER_VALUE_KEY = "value"
const val NONCE_HEADER_ALGORITHM_KEY = "algorithm"
const val NONCE_HEADER_SIGNATURE_KEY = "signature"
const val NONCE_HEADER_PUBKEY_KEY = "key"
const val ANONYMOUS_ENCRYPTION_CIPHER = "curve25519xsalsa20poly1305"
const val headerFieldSeparator = "; "
const val headerKeyValueSeparator = "="
const val HEADER_PREFIX = "message-"
const val HEADER_MESSAGE_ID = "message-id"
const val HEADER_MESSAGE_STREAM = "message-stream"
const val HEADER_MESSAGE_ACCESS = "message-access"
const val HEADER_MESSAGE_HEADERS = "message-headers"
const val HEADER_MESSAGE_ENVELOPE_CHECKSUM = "message-checksum"
const val HEADER_MESSAGE_ENVELOPE_SIGNATURE = "message-signature"
const val HEADER_MESSAGE_ENCRYPTION = "message-encryption"
const val CHUNK_SIZE_KEY = "chunk-size"
const val HEADER_CONTENT_MESSAGE_ID = "id"
const val HEADER_CONTENT_AUTHOR = "author"
const val HEADER_CONTENT_DATE = "date"
const val HEADER_CONTENT_SIZE = "size"
const val HEADER_CONTENT_CHECKSUM = "checksum"
const val HEADER_CONTENT_SUBJECT = "subject"
const val HEADER_CONTENT_SUBJECT_ID = "subject-id"
const val HEADER_CONTENT_PARENT_ID = "parent-id"
const val HEADER_CONTENT_FILES = "files"
const val HEADER_CONTENT_CATEGORY = "category"
const val HEADER_CONTENT_READERS = "readers"
const val DEFAULT_SERVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX"
const val URI_CACHED_FOLDER_NAME = "sharedContent"
val CHECKSUM_HEADERS = arrayOf(
    HEADER_MESSAGE_ID,
    HEADER_MESSAGE_STREAM,
    HEADER_MESSAGE_ACCESS,
    HEADER_MESSAGE_HEADERS,
    HEADER_MESSAGE_ENCRYPTION
)

const val SP_ADDRESS = "sp_address"
const val SP_REFRESH_INTERVAL = "sp_refresh_interval"
const val SP_AVATAR_LINK = "sp_avatar_link"
const val SP_FULL_NAME = "sp_full_name"
const val SP_ENCRYPTION_KEY_ID = "sp_encryption_key_id"
const val SP_SIGNING_KEYS = "sp_signing_keys"
const val SP_ENCRYPTION_KEYS = "sp_encryption_keys"
const val SP_AUTOLOGIN = "sp_autologin"
const val SP_BIOMETRY = "sp_biometry"
const val SP_FIRST_TIME = "sp_first_time"
const val SP_SELECTED_NAV_SCREEN = "sp_selected_nav_screen"
const val QR_SCANNER_RESULT = "qr_scanner_result"
val availableHosts = listOf("open.email")


val DEFAULT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val DEFAULT_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")