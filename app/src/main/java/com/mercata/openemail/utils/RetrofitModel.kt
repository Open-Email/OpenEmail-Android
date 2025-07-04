package com.mercata.openemail.utils

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.utils.Key
import com.mercata.openemail.ANONYMOUS_ENCRYPTION_CIPHER
import com.mercata.openemail.BuildConfig
import com.mercata.openemail.DEFAULT_MAIL_SUBDOMAIN
import com.mercata.openemail.HEADER_PREFIX
import com.mercata.openemail.MAX_MESSAGE_SIZE
import com.mercata.openemail.SIGNING_ALGORITHM
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.archive.toArchive
import com.mercata.openemail.db.attachments.DBAttachment
import com.mercata.openemail.db.contacts.ContactsDao
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.db.drafts.DBDraft
import com.mercata.openemail.db.messages.DBMessage
import com.mercata.openemail.db.notifications.DBNotification
import com.mercata.openemail.db.pending.attachments.DBPendingAttachment
import com.mercata.openemail.db.pending.messages.DBPendingRootMessage
import com.mercata.openemail.db.pending.readers.DBPendingReaderPublicData
import com.mercata.openemail.db.pending.readers.toPublicUserData
import com.mercata.openemail.exceptions.EnvelopeAuthenticity
import com.mercata.openemail.exceptions.SignatureMismatch
import com.mercata.openemail.models.ContentHeaders
import com.mercata.openemail.models.Envelope
import com.mercata.openemail.models.Link
import com.mercata.openemail.models.MessageCategory
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBContact
import com.mercata.openemail.models.toDBNotification
import com.mercata.openemail.models.toDBPendingReaderPublicData
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.response_converters.ContactsListConverterFactory
import com.mercata.openemail.response_converters.EnvelopeIdsListConverterFactory
import com.mercata.openemail.response_converters.UserPublicDataConverterFactory
import com.mercata.openemail.response_converters.WellKnownHost
import com.mercata.openemail.response_converters.WellKnownHostsConverterFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

typealias Address = String

fun Address.getMailHost() = "$DEFAULT_MAIL_SUBDOMAIN.${this.getHost()}"

private fun getInstance(baseUrl: String): RestApi {

    val rv = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(EnvelopeIdsListConverterFactory())
        .addConverterFactory(ContactsListConverterFactory())
        .addConverterFactory(UserPublicDataConverterFactory())
        .addConverterFactory(WellKnownHostsConverterFactory())
        .addConverterFactory(ScalarsConverterFactory.create())
    if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        rv.client(client)
    }

    return rv.build().create(RestApi::class.java)
}

suspend fun getWellKnownHosts(hostName: String): Response<List<WellKnownHost>> {
    return withContext(Dispatchers.IO) { getInstance("https://$hostName").getWellKnownHosts() }
}

suspend fun getProfilePublicData(address: Address): Response<PublicUserData> {
    return withContext(Dispatchers.IO) {
        getInstance("https://$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}").getProfilePublicData(
            hostPart = address.getHost(),
            localPart = address.getLocal()
        )
    }
}

fun Address.getProfilePictureUrl(): String =
    //"https://mail.open.email/mail/open.email/babe/image"
    "https://$DEFAULT_MAIL_SUBDOMAIN.${this.getHost()}/mail/${this.getHost()}/${this.getLocal()}/image"

suspend fun checkUserImage(address: Address): Response<Void> {
    return withContext(Dispatchers.IO) {
        getInstance("https://$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}").checkUserImage(
            hostPart = address.getHost(),
            localPart = address.getLocal(),
        )
    }
}

suspend fun isAddressAvailable(address: String): Response<Void> {
    return withContext(Dispatchers.IO) {
        val host = "$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}"
        getInstance("https://$host").isAddressAvailable(
            hostPart = address.getHost(),
            localPart = address.getLocal()
        )
    }
}

suspend fun registerCall(user: UserData): Response<Void> {
    return withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            Log.i(
                "KEYS", "\n" +
                        "signPrivate: ${user.signingKeys.pair.secretKey.asBytes.encodeToBase64()}\n" +
                        "signPublic: ${user.signingKeys.pair.publicKey.asBytes.encodeToBase64()}\n" +
                        "encryptPrivate: ${user.encryptionKeys.pair.secretKey.asBytes.encodeToBase64()}\n" +
                        "encryptPublic: ${user.encryptionKeys.pair.publicKey.asBytes.encodeToBase64()}\n"
            )
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

        val postData = """
            Name: ${user.name}
            Encryption-Key: id=${user.encryptionKeys.id}; algorithm=$ANONYMOUS_ENCRYPTION_CIPHER; value=${user.encryptionKeys.pair.publicKey.asBytes.encodeToBase64()}
            Signing-Key: algorithm=$SIGNING_ALGORITHM; value=${user.signingKeys.pair.publicKey.asBytes.encodeToBase64()}
            Updated: ${LocalDateTime.now().atOffset(ZoneOffset.UTC).format(formatter)}
            """.trimIndent()

        getInstance("https://${user.address.getMailHost()}").register(
            sotnHeader = user.sign(),
            hostPart = user.address.getHost(),
            localPart = user.address.getLocal(),
            body = postData.toRequestBody()
        )
    }
}

suspend fun deleteCurrentUser(sp: SharedPreferences): Response<Void>? {
    val user = sp.getUserData() ?: return null
    return withContext(Dispatchers.IO) {
        getInstance("https://${user.address.getMailHost()}").deleteAccount(
            sotnHeader = user.sign(),
            hostPart = user.address.getHost(),
            localPart = user.address.getLocal(),
        )
    }
}

suspend fun updateCall(user: UserData, updateData: PublicUserData): Response<Void> {
    return withContext(Dispatchers.IO) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

        val postData = arrayListOf(
            "Name: ${user.name}",
            "Encryption-Key: id=${user.encryptionKeys.id}; algorithm=$ANONYMOUS_ENCRYPTION_CIPHER; value=${user.encryptionKeys.pair.publicKey.asBytes.encodeToBase64()}",
            "Signing-Key: algorithm=$SIGNING_ALGORITHM; value=${user.signingKeys.pair.publicKey.asBytes.encodeToBase64()}",
            "Updated: ${LocalDateTime.now().atOffset(ZoneOffset.UTC).format(formatter)}",
        )

        updateData.run {
            lastSeenPublic?.let {
                postData.add("Last-Seen-Public: ${if (it) "Yes" else "No"}")
            }
            publicAccess?.let {
                postData.add("Public-Access: ${if (it) "Yes" else "No"}")
            }
            publicLinks?.let {
                postData.add("Public-Links: ${if (it) "Yes" else "No"}")
            }
            away?.let {
                postData.add("Away: ${if (it) "Yes" else "No"}")
            }
            awayWarning?.takeIf { it.isNotBlank() }?.let {
                postData.add("Away-Warning: ${it.trim()}")
            }
            status?.takeIf { it.isNotBlank() }?.let {
                postData.add("Status: ${it.trim()}")
            }
            about?.takeIf { it.isNotBlank() }?.let {
                postData.add("About: ${it.trim()}")
            }
            gender?.takeIf { it.isNotBlank() }?.let {
                postData.add("Gender: ${it.trim()}")
            }
            language?.takeIf { it.isNotBlank() }?.let {
                postData.add("Languages: ${it.trim()}")
            }
            relationshipStatus?.takeIf { it.isNotBlank() }?.let {
                postData.add("Relationship-Status: ${it.trim()}")
            }
            education?.takeIf { it.isNotBlank() }?.let {
                postData.add("Education: ${it.trim()}")
            }
            placesLived?.takeIf { it.isNotBlank() }?.let {
                postData.add("Places-Lived: ${it.trim()}")
            }
            notes?.takeIf { it.isNotBlank() }?.let {
                postData.add("Notes: ${it.trim()}")
            }
            work?.takeIf { it.isNotBlank() }?.let {
                postData.add("Work: ${it.trim()}")
            }
            department?.takeIf { it.isNotBlank() }?.let {
                postData.add("Department: ${it.trim()}")
            }
            organization?.takeIf { it.isNotBlank() }?.let {
                postData.add("Organization: ${it.trim()}")
            }
            jobTitle?.takeIf { it.isNotBlank() }?.let {
                postData.add("Job-Title: ${it.trim()}")
            }
            interests?.takeIf { it.isNotBlank() }?.let {
                postData.add("Interests: ${it.trim()}")
            }
            books?.takeIf { it.isNotBlank() }?.let {
                postData.add("Books: ${it.trim()}")
            }
            music?.takeIf { it.isNotBlank() }?.let {
                postData.add("Music: ${it.trim()}")
            }
            movies?.takeIf { it.isNotBlank() }?.let {
                postData.add("Movies: ${it.trim()}")
            }
            sports?.takeIf { it.isNotBlank() }?.let {
                postData.add("Sports: ${it.trim()}")
            }
            website?.takeIf { it.isNotBlank() }?.let {
                postData.add("Website: ${it.trim()}")
            }
            mailingAddress?.takeIf { it.isNotBlank() }?.let {
                postData.add("Mailing-Address: ${it.trim()}")
            }
            location?.takeIf { it.isNotBlank() }?.let {
                postData.add("Location: ${it.trim()}")
            }
            phone?.takeIf { it.isNotBlank() }?.let {
                postData.add("Phone: ${it.trim()}")
            }
            streams?.takeIf { it.isNotBlank() }?.let {
                postData.add("Streams: ${it.trim()}")
            }
        }

        getInstance("https://${user.address.getMailHost()}").updateUser(
            sotnHeader = user.sign(),
            hostPart = user.address.getHost(),
            localPart = user.address.getLocal(),
            body = postData.joinToString("\n").trimIndent().toRequestBody()
        )
    }
}

suspend fun loginCall(user: UserData): Response<Void> {
    return withContext(Dispatchers.IO) {
        getInstance("https://${user.address.getMailHost()}").login(
            user.sign(),
            hostPart = user.address.getHost(),
            localPart = user.address.getLocal()
        )
    }
}

suspend fun uploadContact(
    address: Address,
    sharedPreferences: SharedPreferences
): Response<Void>? {
    address.connectionLink()?.let { link ->
        return withContext(Dispatchers.IO) {

            val currentUser = sharedPreferences.getUserData() ?: return@withContext null

            val encryptedRemoteAddress = encryptAnonymous(address, currentUser)

            getInstance("https://${currentUser.address.getMailHost()}").uploadContact(
                sotnHeader = currentUser.sign(),
                hostPart = currentUser.address.getHost(),
                localPart = currentUser.address.getLocal(),
                link = link,
                body = encryptedRemoteAddress.toRequestBody()
            )
        }
    }
    return null
}

suspend fun notifyAddress(receiver: PublicUserData, sharedPreferences: SharedPreferences) {
    receiver.address.connectionLink()?.let { link ->
        withContext(Dispatchers.IO) {
            val currentUser: UserData = sharedPreferences.getUserData() ?: return@withContext

            val encryptedRemoteAddress = encryptAnonymous(
                currentUser.address,
                Key.fromBase64String(receiver.publicEncryptionKey)
            )

            getInstance("https://${currentUser.address.getMailHost()}").notifyAddress(
                sotnHeader = currentUser.sign(),
                hostPart = receiver.address.getHost(),
                localPart = receiver.address.getLocal(),
                link = link,
                body = encryptedRemoteAddress.toRequestBody()
            )
        }
    }
}

suspend fun getAllLinks(sharedPreferences: SharedPreferences): Response<List<Link>>? {
    return withContext(Dispatchers.IO) {
        val currentUser = sharedPreferences.getUserData() ?: return@withContext null
        getInstance("https://${currentUser.address.getMailHost()}").getAllContactLinks(
            sotnHeader = currentUser.sign(),
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal()
        )
    }
}

suspend fun updateBroadcastsForLink(
    sp: SharedPreferences,
    link: Link,
    allowedBroadcasts: Boolean
): Response<Void>? {
    return withContext(Dispatchers.IO) {
        val currentUser = sp.getUserData() ?: return@withContext null
        val body = listOf(
            "address=${link.address}",
            "broadcasts=${if (allowedBroadcasts) "Yes" else "No"}"
        ).joinToString(";")
        getInstance("https://${currentUser.address.getMailHost()}").updateContactLink(
            sotnHeader = currentUser.sign(),
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
            link = link.link,
            encryptAnonymous(body.trimIndent(), currentUser).toRequestBody()
        )
    }
}

suspend fun deleteContact(
    address: Address,
    sharedPreferences: SharedPreferences
): Response<Void>? {
    address.connectionLink()?.let { link ->
        return withContext(Dispatchers.IO) {
            val currentUser = sharedPreferences.getUserData() ?: return@withContext null
            getInstance("https://${currentUser.address.getMailHost()}").deleteContact(
                sotnHeader = currentUser.sign(),
                hostPart = currentUser.address.getHost(),
                localPart = currentUser.address.getLocal(),
                linkAddr = link
            )
        }
    }
    return null
}

private suspend fun getAllBroadcastEnvelopes(
    sharedPreferences: SharedPreferences,
    contactsDao: ContactsDao
): List<Envelope>? {
    return withContext(Dispatchers.IO) {
        val results = contactsDao.getAll().map { contact ->
            async {
                getAllBroadcastEnvelopesForContact(sharedPreferences, contact)
            }
        }.awaitAll()

        if (results.contains(null)) {
            null
        } else {
            results.fold(initial = arrayListOf(), operation = { initial, new ->
                initial.apply { addAll(new!!) }
            })
        }
    }
}

private suspend fun getAllBroadcastEnvelopesForContact(
    sharedPreferences: SharedPreferences,
    contact: DBContact
): List<Envelope>? {
    if (contact.receiveBroadcasts.not()) {
        return null
    }
    return withContext(Dispatchers.IO) {
        val currentUser = sharedPreferences.getUserData() ?: return@withContext null
        when (val idsCall = safeApiCall {
            getInstance("https://${currentUser.address.getMailHost()}").getAllBroadcastMessagesIdsForContact(
                sotnHeader = currentUser.sign(),
                hostPart = contact.address.getHost(),
                localPart = contact.address.getLocal()
            )
        }) {
            is HttpResult.Error -> {
                null
            }

            is HttpResult.Success -> {
                idsCall.data?.let { ids ->
                    fetchEnvelopesForContact(
                        messageIds = ids,
                        currentUser = currentUser,
                        contact = contact,
                        link = null
                    )
                }
            }
        }
    }
}

private suspend fun getAllPrivateEnvelopesForContact(
    sharedPreferences: SharedPreferences,
    contact: DBContact
): List<Envelope>? {
    contact.address.connectionLink()?.let { link ->
        return withContext(Dispatchers.IO) {
            val currentUser = sharedPreferences.getUserData() ?: return@withContext null
            when (val idsCall = safeApiCall {
                getInstance("https://${currentUser.address.getMailHost()}").getAllPrivateMessagesIdsForContact(
                    sotnHeader = currentUser.sign(),
                    hostPart = contact.address.getHost(),
                    localPart = contact.address.getLocal(),
                    connectionLink = link
                )
            }) {
                is HttpResult.Error -> {
                    null
                }

                is HttpResult.Success -> {
                    idsCall.data?.let { ids ->
                        fetchEnvelopesForContact(
                            messageIds = ids,
                            currentUser = currentUser,
                            contact = contact,
                            link = link
                        )
                    }
                }
            }
        }
    }
    return null
}

private suspend fun getAllPrivateEnvelopes(
    sp: SharedPreferences,
    db: AppDatabase
): List<Envelope>? {
    return withContext(Dispatchers.IO) {
        val contacts = db.userDao().getAll()
        val userAddress = sp.getUserAddress() ?: return@withContext null
        val tasks: ArrayList<Deferred<List<Envelope>?>> = arrayListOf()

        getNewNotifications(sp, db)?.let { notifications ->
            tasks.addAll(notifications.dataNotifications.map { new ->
                async {
                    val envelopes = getAllPrivateEnvelopesForContact(
                        sp,
                        contacts.first { contact -> contact.address == new.address })
                    db.notificationsDao().insert(new)
                    envelopes
                }
            })
        }

        //add self to fetch outbox
        tasks.add(async {
            getAllPrivateEnvelopesForContact(
                sp,
                contacts.first { contact -> contact.address == userAddress })
        })

        val results = tasks.awaitAll()

        if (results.contains(null)) {
            null
        } else {
            results.fold(
                initial = arrayListOf(),
                operation = { initial, new -> initial.apply { addAll(new!!) } })
        }
    }
}

suspend fun downloadMessage(
    currentUser: UserData,
    contactAddress: Address,
    messageId: String
): Response<ResponseBody> {
    return withContext(Dispatchers.IO) {
        getInstance("https://${currentUser.address.getMailHost()}").downloadMessage(
            sotnHeader = currentUser.sign(),
            hostPart = contactAddress.getHost(),
            localPart = contactAddress.getLocal(),
            messageId = messageId
        )
    }
}

suspend fun getNewNotifications(sp: SharedPreferences, db: AppDatabase): NotificationsResult? {
    return withContext(Dispatchers.IO) {
        val currentUser = sp.getUserData() ?: return@withContext null
        db.notificationsDao().getAll().filter { it.isExpired() }.let { expired ->
            db.notificationsDao().deleteList(expired)
        }

        val result: List<String>? = when (val call = safeApiCall {
            getInstance("https://${currentUser.address.getMailHost()}").getNotifications(
                sotnHeader = currentUser.sign(),
                hostPart = currentUser.address.getHost(),
                localPart = currentUser.address.getLocal(),
            )
        }) {
            is HttpResult.Error -> null
            is HttpResult.Success -> call.data?.splitToSequence("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.distinct()
                ?.toList()
        }

        val contacts = db.userDao().getAll()
        val oldNotifications = db.notificationsDao().getAll()

        val newNotifications: List<DBNotification> = result?.map {
            async { verifyNotification(it, currentUser) }
        }?.awaitAll()
            ?.filterNotNull()
            ?.filterNot { oldNotifications.any { old -> old.notificationId == it.notificationId } }
            ?: listOf()

        val splitResults =
            newNotifications.partition { notification ->
                contacts.any { contact -> contact.address == notification.address }
            }

        val newContactRequests = splitResults.second
        val newDataNotification = splitResults.first

        return@withContext NotificationsResult(newContactRequests, newDataNotification)
    }
}

data class NotificationsResult(
    val contactRequests: List<DBNotification>,
    val dataNotifications: List<DBNotification>
)

suspend fun verifyNotification(
    notificationLine: String,
    currentUser: UserData,
): DBNotification? {
    return withContext(Dispatchers.IO) {
        val notificationParts = notificationLine.split(",")
            .map { it.trim() }

        val id = notificationParts[0]
        val link = notificationParts[1]
        val signingKeyFP = notificationParts[2]
        val encryptedNotifier = notificationParts[3]

        val notifierAddressBytes = try {
            decryptAnonymous(
                cipherText = encryptedNotifier,
                currentUser = currentUser
            )
        } catch (_: SodiumException) {
            return@withContext null
        }


        val address = notifierAddressBytes.toString(Charsets.US_ASCII)

        val profile: PublicUserData =
            when (val call = safeApiCall { getProfilePublicData(address) }) {
                is HttpResult.Error -> null
                is HttpResult.Success -> call.data
            } ?: return@withContext null

        if (currentUser.connectionLinkFor(profile.address) != link) {
            return@withContext null
        }

        val signHash = profile.publicSigningKey.decodeFromBase64().hashedWithSha256()

        var fpMatchFound = signingKeyFP == signHash.first

        if (!fpMatchFound) {
            fpMatchFound = signingKeyFP == profile.lastSigningKey?.decodeFromBase64()
                ?.hashedWithSha256()?.first
        }

        if (fpMatchFound) {
            profile.toDBNotification(id, link)
        } else {
            null
        }
    }
}

private suspend fun fetchEnvelopesForContact(
    messageIds: List<String>,
    currentUser: UserData,
    contact: DBContact,
    link: String?
): List<Envelope> {
    return withContext(Dispatchers.IO) {
        messageIds.map { messageId ->
            async {
                when (val envelopeCall = safeApiCall {
                    if (link == null) {
                        getInstance("https://${currentUser.address.getMailHost()}").fetchBroadcastEnvelope(
                            currentUser.sign(),
                            contact.address.getHost(),
                            contact.address.getLocal(),
                            messageId
                        )
                    } else {
                        getInstance("https://${currentUser.address.getMailHost()}").fetchPrivateEnvelope(
                            currentUser.sign(),
                            contact.address.getHost(),
                            contact.address.getLocal(),
                            messageId,
                            link
                        )
                    }

                }) {
                    is HttpResult.Error -> {
                        null
                    }

                    is HttpResult.Success -> {

                        envelopeCall.headers ?: run {
                            return@async null
                        }

                        val envelope =
                            Envelope(
                                messageId,
                                currentUser,
                                contact,
                                envelopeCall.headers
                            )

                        if (!envelope.successfullyParsed) {
                            return@async null
                        }

                        val authentic: Boolean = try {
                            envelope.assertEnvelopeAuthenticity()
                            true
                        } catch (_: EnvelopeAuthenticity) {
                            false
                        } catch (_: SignatureMismatch) {
                            false
                        }

                        envelope.takeIf { authentic }
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }
}

suspend fun syncMessagesForContact(
    contact: DBContact,
    db: AppDatabase,
    sp: SharedPreferences,
    dl: DownloadRepository,
) {
    withContext(Dispatchers.IO) {
        val privateEnvelopes: Deferred<List<Envelope>?> = async {
            getAllPrivateEnvelopesForContact(
                sp,
                contact
            )
        }
        val broadcastEnvelopes: Deferred<List<Envelope>?> = async {
            getAllBroadcastEnvelopesForContact(
                sp,
                contact
            )
        }

        val results: List<List<Envelope>?> =
            listOf(privateEnvelopes, broadcastEnvelopes).awaitAll()

        if (results.contains(null).not()) {
            val folded = results.fold(
                initial = arrayListOf<Envelope>(),
                operation = { initial, new -> initial.apply { addAll(new!!) } })
            saveMessagesToDb(dl, folded, db, sp)
        }
    }
}

suspend fun syncAllMessages(db: AppDatabase, sp: SharedPreferences, dl: DownloadRepository) {

    withContext(Dispatchers.IO) {
        val privateEnvelopes: Deferred<List<Envelope>?> = async {
            getAllPrivateEnvelopes(
                sp,
                db
            )
        }

        val broadcastEnvelopes: Deferred<List<Envelope>?> = async {
            getAllBroadcastEnvelopes(
                sp,
                db.userDao()
            )
        }

        val results: List<List<Envelope>?> =
            listOf(privateEnvelopes, broadcastEnvelopes).awaitAll()

        if (results.contains(null).not()) {
            val folded = results.fold(
                initial = arrayListOf<Envelope>(),
                operation = { initial, new -> initial.apply { addAll(new!!) } })
            saveMessagesToDb(dl, folded, db, sp)
            sp.setFirstTime(false)
        }
    }
}

suspend fun uploadMessage(
    draft: DBDraft,
    recipients: List<PublicUserData>,
    fileUtils: FileUtils,
    currentUser: UserData,
    db: AppDatabase,
    sp: SharedPreferences,
    isBroadcast: Boolean,
    replyToSubjectId: String?
) {
    withContext(Dispatchers.IO) {
        val rootMessageId = currentUser.newMessageId()
        val sendingDate = Instant.now()
        val accessProfiles: List<PublicUserData>? =
            if (isBroadcast) {
                null
            } else {
                when (val call = safeApiCall { getProfilePublicData(currentUser.address) }) {
                    is HttpResult.Error -> null
                    is HttpResult.Success -> {
                        call.data?.let {
                            arrayListOf(it).apply {
                                addAll(
                                    recipients
                                )
                            }
                        }
                    }
                }
            }

        if (!isBroadcast && accessProfiles == null) {
            return@withContext
        }

        val pendingRootMessage = DBPendingRootMessage(
            messageId = rootMessageId,
            subjectId = replyToSubjectId,
            timestamp = sendingDate.toEpochMilli(),
            subject = draft.subject,
            checksum = draft.textBody.hashedWithSha256().first,
            category = MessageCategory.personal.name,
            size = draft.textBody.toByteArray().size.toLong(),
            authorAddress = currentUser.address,
            textBody = draft.textBody,
            isBroadcast = isBroadcast
        )

        val fileParts = arrayListOf<DBPendingAttachment>()

        draft.attachmentUriList?.split(",")?.map { it.toUri() }?.forEach { uri ->
            val urlInfo = fileUtils.getURLInfo(uri)

            if (urlInfo.size <= MAX_MESSAGE_SIZE) {
                val partMessageId = currentUser.newMessageId()
                fileParts.add(
                    DBPendingAttachment(
                        messageId = partMessageId,
                        subjectId = replyToSubjectId,
                        parentId = rootMessageId,
                        uri = urlInfo.uri.toString(),
                        fileName = urlInfo.name,
                        mimeType = urlInfo.mimeType,
                        fullSize = urlInfo.size,
                        modifiedAtTimestamp = urlInfo.modifiedAt.toEpochMilli(),
                        partNumber = 1,
                        partSize = urlInfo.size,
                        checkSum = fileUtils.sha256fileSum(uri).first,
                        offset = null,
                        totalParts = 1,
                        sendingDateTimestamp = sendingDate.toEpochMilli(),
                        subject = draft.subject,
                        isBroadcast = isBroadcast
                    )
                )
            } else {
                //attachment too large. Split in chunks

                var partCounter = 1
                val buffer = ByteArray(MAX_MESSAGE_SIZE.toInt())
                val totalParts = (urlInfo.size + MAX_MESSAGE_SIZE - 1) / MAX_MESSAGE_SIZE
                var offset: Long = 0

                fileUtils.getInputStreamFromUri(uri)?.use { inputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {

                        val partMessageId = currentUser.newMessageId()
                        val bytesChecksum = fileUtils.sha256fileSum(uri, offset, bytesRead)
                        fileParts.add(
                            DBPendingAttachment(
                                messageId = partMessageId,
                                subjectId = replyToSubjectId,
                                parentId = rootMessageId,
                                uri = urlInfo.uri.toString(),
                                fileName = urlInfo.name,
                                mimeType = urlInfo.mimeType,
                                fullSize = urlInfo.size,
                                modifiedAtTimestamp = urlInfo.modifiedAt.toEpochMilli(),
                                partNumber = partCounter,
                                partSize = bytesRead.toLong(),
                                checkSum = bytesChecksum.first,
                                offset = offset,
                                totalParts = totalParts.toInt(),
                                sendingDateTimestamp = sendingDate.toEpochMilli(),
                                subject = draft.subject,
                                isBroadcast = isBroadcast
                            )
                        )
                        offset += bytesRead
                        partCounter++
                    }
                }
            }
        }

        db.userDao().insertAll(recipients.map { it.toDBContact() })
        db.pendingMessagesDao().insert(pendingRootMessage)
        db.pendingAttachmentsDao().insertAll(fileParts)
        if (!isBroadcast) {
            db.pendingReadersDao()
                .insertAll(accessProfiles!!.map { it.toDBPendingReaderPublicData(rootMessageId) })
        }
        uploadPendingMessages(currentUser, db, fileUtils, sp)
    }
}


suspend fun uploadPendingMessages(
    currentUser: UserData,
    db: AppDatabase,
    fileUtils: FileUtils,
    sp: SharedPreferences
) {
    withContext(Dispatchers.IO) {
        val pendingMessages = db.pendingMessagesDao().getAll()
        val results: List<UploadResult> =
            pendingMessages.map { pendingMessage ->
                async {
                    val messagesToUpload = arrayListOf(
                        //root message
                        async {
                            val result = UploadResult(
                                messageId = pendingMessage.message.messageId,
                                isAttachment = false,
                                messagesResult = safeApiCall {
                                    uploadRootMessage(
                                        pendingRootMessage = pendingMessage.message,
                                        recipients = pendingMessage.readers,
                                        content = pendingMessage.getRootContentHeaders(),
                                        currentUser = currentUser
                                    )
                                },
                                contactResult = pendingMessage.readers.filterNot { pendingReader ->
                                    db.userDao().getAll()
                                        .any { it.address == pendingReader.address && it.uploaded }
                                }.map { reader ->
                                    async {
                                        safeApiCall { uploadContact(reader.address, sp) }
                                    }
                                }.awaitAll()
                            )

                            if (result.messagesResult is HttpResult.Success) {
                                pendingMessage.readers.filterNot { it.address == currentUser.address }
                                    .map {
                                        launch {
                                            notifyAddress(it.toPublicUserData(), sp)
                                        }
                                    }.joinAll()
                            }

                            result
                        }
                    )
                    messagesToUpload.addAll(pendingMessage.fileParts.map { part ->
                        async {
                            UploadResult(
                                messageId = part.messageId,
                                isAttachment = true,
                                messagesResult = safeApiCall {
                                    uploadFileMessage(
                                        currentUser = currentUser,
                                        pendingAttachment = part,
                                        readers = pendingMessage.readers,
                                        fileUtils = fileUtils
                                    )
                                },
                                contactResult = listOf() //No need to upload contacts for every attachment, they will be uploaded with Root only once
                            )
                        }
                    })
                    messagesToUpload.awaitAll()
                }
            }.awaitAll().fold(
                initial = arrayListOf(),
                operation = { initial, new -> initial.apply { addAll(new) } })


        val everyPartUploaded: Boolean =
            results.any { it.messagesResult !is HttpResult.Success }.not()

        if (everyPartUploaded) {
            db.pendingMessagesDao().deleteList(pendingMessages.map { it.message })
            db.pendingAttachmentsDao().deleteList(
                pendingMessages.fold(
                    initial = arrayListOf(),
                    operation = { initial, new -> initial.apply { addAll(new.fileParts) } })
            )
            db.pendingReadersDao().deleteList(
                pendingMessages.fold(
                    initial = arrayListOf(),
                    operation = { initial, new -> initial.apply { addAll(new.readers) } })
            )
            pendingMessages.fold(initial = arrayListOf<Uri>(), operation = { initial, new ->
                initial.apply { addAll(new.fileParts.map { it.uri.toUri() }) }
            }).forEach { uri ->
                fileUtils.getFileFromUri(uri)?.delete()
            }
        } else {
            results.filter { it.messagesResult is HttpResult.Success }.forEach { successful ->
                if (successful.isAttachment) {
                    db.pendingAttachmentsDao().delete(successful.messageId)
                    pendingMessages.forEach { message ->
                        message.fileParts.firstOrNull { it.messageId == successful.messageId }?.uri?.let { uriStr ->
                            fileUtils.getFileFromUri(
                                uriStr.toUri()
                            )?.delete()
                        }
                    }
                } else {
                    db.pendingMessagesDao().delete(successful.messageId)
                }
            }
        }
    }
}

data class UploadResult(
    val messageId: String,
    val isAttachment: Boolean,
    val messagesResult: HttpResult<Void>,
    val contactResult: List<HttpResult<Void>>
)

private suspend fun uploadRootMessage(
    pendingRootMessage: DBPendingRootMessage,
    recipients: List<DBPendingReaderPublicData>,
    content: ContentHeaders,
    currentUser: UserData
): Response<Void> {
    return withContext(Dispatchers.IO) {
        val accessKey = generateRandomBytes(32)
        val accessLinks = recipients.generateAccessLinks(accessKey)

        val envelopeHeadersMap =
            content.seal(
                accessKey,
                content.messageID,
                accessLinks,
                currentUser,
                pendingRootMessage.isBroadcast
            )

        val sealedBody =
            if (pendingRootMessage.isBroadcast)
                pendingRootMessage.textBody.toByteArray()
            else
                encrypt_xchacha20poly1305(
                    secretKey = accessKey,
                    message = pendingRootMessage.textBody.toByteArray()
                )

        getInstance("https://${currentUser.address.getMailHost()}").uploadMessageFile(
            sotnHeader = currentUser.sign(),
            contentLength = pendingRootMessage.textBody.toByteArray().size.toLong(),
            headers = envelopeHeadersMap.filter { it.key.startsWith(HEADER_PREFIX) },
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
            file = sealedBody!!.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
    }
}

suspend fun deleteUserImage(sp: SharedPreferences): Response<Void>? {
    return withContext(Dispatchers.IO) {
        val currentUser = sp.getUserData() ?: return@withContext null

        getInstance("https://${currentUser.address.getMailHost()}").deleteUserImage(
            sotnHeader = currentUser.sign(),
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
        )
    }
}

suspend fun uploadUserImage(
    uri: Uri,
    sp: SharedPreferences,
    fileUtils: FileUtils
): Response<Void>? {
    return withContext(Dispatchers.IO) {
        val currentUser = sp.getUserData() ?: return@withContext null

        val data: ByteArray? = with(fileUtils) {
            uri.getBitmapFromUri()?.resizeImageToMaxSize(800)?.compressBitmap()
        }

        getInstance("https://${currentUser.address.getMailHost()}").uploadUserImage(
            sotnHeader = currentUser.sign(),
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
            body = data?.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
    }
}

private suspend fun uploadFileMessage(
    currentUser: UserData,
    pendingAttachment: DBPendingAttachment,
    readers: List<DBPendingReaderPublicData>,
    fileUtils: FileUtils,
): Response<Void>? {
    return withContext(Dispatchers.IO) {
        val accessKey = generateRandomBytes(32)

        val accessLinks = readers.generateAccessLinks(accessKey)

        val envelopeHeadersMap =
            pendingAttachment.getContentHeaders(
                currentUser.address,
                readers
            ).seal(
                accessKey,
                pendingAttachment.messageId,
                accessLinks,
                currentUser,
                pendingAttachment.isBroadcast
            )

        val urlInfo = pendingAttachment.getUrlInfo()

        val encryptedData =
            if (pendingAttachment.isBroadcast)
                fileUtils.getAllBytesForUri(
                    uri = urlInfo.uri!!,
                    offset = pendingAttachment.offset ?: 0,
                    bytesCount = pendingAttachment.partSize
                ) ?: return@withContext null
            else
                fileUtils.encryptFilePartXChaCha20Poly1305(
                    inputUri = urlInfo.uri!!,
                    secretKey = accessKey,
                    bytesCount = pendingAttachment.partSize,
                    offset = pendingAttachment.offset ?: 0
                ) ?: return@withContext null

        getInstance("https://${currentUser.address.getMailHost()}").uploadMessageFile(
            sotnHeader = currentUser.sign(),
            contentLength = urlInfo.size,
            headers = envelopeHeadersMap.filter { it.key.startsWith(HEADER_PREFIX) },
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
            file = encryptedData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
    }
}

suspend fun saveMessagesToDb(
    dl: DownloadRepository,
    results: List<Envelope>,
    db: AppDatabase,
    sp: SharedPreferences,
) {
    withContext(Dispatchers.IO) {

        val removedOutbox =
            db.messagesDao().getAllWithAttachments()
                .filter { dbMessage -> dbMessage.message.authorAddress == sp.getUserAddress() && !results.any { envelope -> envelope.messageId == dbMessage.getMessageId() } }

        db.archiveDao().insertAll(removedOutbox.map { it.toArchive() })
        db.messagesDao().deleteList(removedOutbox.map { it.message })

        val envelopesPair = results.partition { envelope -> envelope.isRootMessage() }

        val rootEnvelopes = envelopesPair.first
        val attachmentEnvelopes = envelopesPair.second

        val rootMessages = dl.downloadMessagesPayload(rootEnvelopes).awaitAll()

        val attachments: List<DBAttachment> = rootMessages.map { root ->
            val headers = root.first.contentHeaders
            headers.fileParts?.filter { fileInfo ->
                attachmentEnvelopes.firstOrNull { it.messageId == fileInfo.messageId }
                    ?.takeIf { it.isBroadcast() || it.accessKey != null } != null
            }?.map { fileInfo ->
                DBAttachment(
                    attachmentMessageId = fileInfo.messageId,
                    authorAddress = root.first.contact.address,
                    parentId = root.first.messageId,
                    name = fileInfo.urlInfo.name,
                    type = fileInfo.urlInfo.mimeType,
                    fileSize = fileInfo.size,
                    partSize = fileInfo.size,
                    partIndex = fileInfo.part,
                    partsAmount = fileInfo.totalParts,
                    accessKey = attachmentEnvelopes.first { it.messageId == fileInfo.messageId }.accessKey,
                    createdTimestamp = fileInfo.urlInfo.modifiedAt.toEpochMilli(),
                )
            } ?: listOf()
        }.fold(
            initial = arrayListOf(),
            operation = { initial, new ->
                initial.apply { addAll(new) }
            })

        db.messagesDao().insertAll(
            rootMessages.map {
                DBMessage(
                    messageId = it.first.messageId,
                    authorAddress = it.first.contact.address,
                    subject = it.first.contentHeaders.subject,
                    textBody = it.second ?: "",
                    isBroadcast = it.first.isBroadcast(),
                    timestamp = it.first.contentHeaders.date.toEpochMilli(),
                    readerAddresses = it.first.contentHeaders.readersAddresses?.joinToString(","),
                    isUnread = !sp.isFirstTime() && sp.getUserAddress() != it.first.contact.address,
                    markedToDelete = false
                )
            })
        db.attachmentsDao().insertAll(attachments)
    }
}

suspend fun syncContacts(sp: SharedPreferences, dao: ContactsDao) {
    withContext(Dispatchers.IO) {
        val localContacts = dao.getAll()

        //Uploading local-only contacts
        val uploaded: List<DBContact?> =
            localContacts.filter { it.uploaded.not() }.map { contactToUpload ->
                async {
                    when (safeApiCall { uploadContact(contactToUpload.address, sp) }) {
                        is HttpResult.Error -> null
                        is HttpResult.Success -> contactToUpload
                    }
                }
            }.awaitAll()

        uploaded.filterNotNull().forEach { uploadedContact ->
            dao.update(uploadedContact.copy(uploaded = true))
        }

        //Deleting marked-to-delete local contacts
        val deleted: List<DBContact?> =
            localContacts.filter { it.markedToDelete }.map { contactToDelete ->
                async {
                    when (safeApiCall { deleteContact(contactToDelete.address, sp) }) {
                        is HttpResult.Error -> null
                        is HttpResult.Success -> contactToDelete
                    }
                }
            }.awaitAll()

        deleted.filterNotNull().forEach { deletedContact ->
            dao.delete(deletedContact)
        }

        //Downloading new remote contacts
        when (val remoteAddressesCall = safeApiCall { getAllLinks(sp) }) {
            is HttpResult.Error -> {
                Log.e(
                    "HTTP ERROR",
                    remoteAddressesCall.message ?: remoteAddressesCall.code.toString()
                )
            }

            is HttpResult.Success -> {
                remoteAddressesCall.data?.let {
                    val remotes = remoteAddressesCall.data

                    val result: List<PublicUserData> = remotes.map { remoteAddress ->
                        async {
                            when (val publicCall =
                                safeApiCall { getProfilePublicData(remoteAddress.address) }) {
                                is HttpResult.Error -> {
                                    Log.e(
                                        "HTTP ERROR",
                                        publicCall.message ?: publicCall.code.toString()
                                    )
                                }

                                is HttpResult.Success -> return@async publicCall.data
                            }
                            null
                        }
                    }.awaitAll().filterNotNull()

                    //deleting local contacts, which isn't present on remote
                    dao.deleteList(dao.getAll().filterNot { local ->
                        local.address == sp.getUserAddress() ||
                                result.any { remote -> remote.address == local.address } ||
                                local.uploaded.not()
                    })

                    dao.insertAll(result.map { publicData ->
                        publicData.toDBContact()
                    })
                }
            }
        }
    }
}

suspend fun rewokeOutboxMessage(currentUser: UserData, message: DBMessage) {
    withContext(Dispatchers.IO) {
        when (safeApiCall { removeSentMessage(currentUser, message) }) {
            is HttpResult.Error -> null
            is HttpResult.Success -> message
        }
    }
}

suspend fun removeSentMessage(currentUser: UserData, message: DBMessage): Response<Void> {
    return withContext(Dispatchers.IO) {
        getInstance("https://${currentUser.address.getMailHost()}").revokeMessage(
            sotnHeader = currentUser.sign(),
            hostPart = currentUser.address.getHost(),
            localPart = currentUser.address.getLocal(),
            messageId = message.messageId
        )
    }
}

suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>?): HttpResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val response = call.invoke()
            if (response?.isSuccessful == true) {
                HttpResult.Success(
                    response.body(),
                    response.message(),
                    response.code(),
                    response.headers()
                )
            } else {
                HttpResult.Error(response?.message(), response?.code(), response?.headers())
            }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            e.printStackTrace()
            HttpResult.Error(e.message, -1, null)
        }
    }

sealed class HttpResult<out T : Any>(
    open val message: String?,
    open val code: Int?,
    open val headers: Headers?
) {
    data class Success<out T : Any>(
        val data: T?,
        override val message: String?,
        override val code: Int,
        override val headers: Headers?
    ) :
        HttpResult<T>(message, code, headers)

    data class Error(
        override val message: String?,
        override val code: Int?,
        override val headers: Headers?
    ) :
        HttpResult<Nothing>(message, code, headers)
}