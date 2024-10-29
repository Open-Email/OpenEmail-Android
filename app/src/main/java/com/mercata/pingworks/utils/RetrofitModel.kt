package com.mercata.pingworks.utils

import android.net.Uri
import android.util.Log
import com.mercata.pingworks.ANONYMOUS_ENCRYPTION_CIPHER
import com.mercata.pingworks.BuildConfig
import com.mercata.pingworks.DEFAULT_MAIL_SUBDOMAIN
import com.mercata.pingworks.HEADER_PREFIX
import com.mercata.pingworks.MAX_MESSAGE_SIZE
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.attachments.AttachmentsDao
import com.mercata.pingworks.db.attachments.DBAttachment
import com.mercata.pingworks.db.contacts.ContactsDao
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.messages.DBMessage
import com.mercata.pingworks.db.messages.MessagesDao
import com.mercata.pingworks.db.pending.attachments.DBPendingAttachment
import com.mercata.pingworks.db.pending.messages.DBPendingRootMessage
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.exceptions.EnvelopeAuthenticity
import com.mercata.pingworks.exceptions.SignatureMismatch
import com.mercata.pingworks.models.ComposingData
import com.mercata.pingworks.models.ContentHeaders
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.models.MessageCategory
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBPendingReaderPublicData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.response_converters.ContactsListConverterFactory
import com.mercata.pingworks.response_converters.EnvelopeIdsListConverterFactory
import com.mercata.pingworks.response_converters.UserPublicDataConverterFactory
import com.mercata.pingworks.response_converters.WellKnownHost
import com.mercata.pingworks.response_converters.WellKnownHostsConverterFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
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
import java.io.FileInputStream
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
    return getInstance("https://$hostName").getWellKnownHosts()
}

//https://main.ping.works/mail/ping.works/testdejan6/profile
suspend fun getProfilePublicData(address: String): Response<PublicUserData> {
    return getInstance("https://$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}").getProfilePublicData(
        hostPart = address.getHost(),
        localPart = address.getLocal()
    )
}

suspend fun isAddressAvailable(address: String): Response<Void> {
    val host = "$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}"
    return getInstance("https://$host").isAddressAvailable(
        hostPart = address.getHost(),
        localPart = address.getLocal()
    )
}

suspend fun registerCall(user: UserData): Response<Void> {
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

    return getInstance("https://${user.address.getMailHost()}").register(
        sotnHeader = user.sign(),
        hostPart = user.address.getHost(),
        localPart = user.address.getLocal(),
        body = postData.toRequestBody()
    )
}

suspend fun loginCall(user: UserData): Response<Void> {
    return getInstance("https://${user.address.getMailHost()}").login(
        user.sign(),
        hostPart = user.address.getHost(),
        localPart = user.address.getLocal()
    )
}

suspend fun uploadContact(
    contact: PublicUserData,
    sharedPreferences: SharedPreferences
): Response<Void> {
    val link = contact.address.connectionLink()

    val currentUser = sharedPreferences.getUserData()!!

    val encryptedRemoteAddress = encryptAnonymous(contact.address, currentUser)

    return getInstance("https://${currentUser.address.getMailHost()}").uploadContact(
        sotnHeader = currentUser.sign(),
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal(),
        link = link,
        body = encryptedRemoteAddress.toRequestBody()
    )
}

private suspend fun getAllContacts(sharedPreferences: SharedPreferences): Response<List<String>> {
    val currentUser = sharedPreferences.getUserData()!!
    return getInstance("https://${currentUser.address.getMailHost()}").getAllContacts(
        sotnHeader = currentUser.sign(),
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal()
    )
}

suspend fun deleteContact(
    contact: DBContact,
    sharedPreferences: SharedPreferences
): Response<Void> {
    val currentUser = sharedPreferences.getUserData()!!
    return getInstance("https://${currentUser.address.getMailHost()}").deleteContact(
        sotnHeader = currentUser.sign(),
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal(),
        linkAddr = contact.address.connectionLink()
    )
}

private suspend fun getAllBroadcastEnvelopes(
    sharedPreferences: SharedPreferences,
    contactsDao: ContactsDao
): List<Envelope> {
    return withContext(Dispatchers.IO) {
        contactsDao.getAll().map { contact ->
            async {
                getAllBroadcastEnvelopesForContact(sharedPreferences, contact)
            }
        }.awaitAll().fold(initial = arrayListOf(), operation = { initial, new ->
            initial.apply { addAll(new) }
        })
    }
}

private suspend fun getAllBroadcastEnvelopesForContact(
    sharedPreferences: SharedPreferences,
    contact: DBContact
): List<Envelope> {
    if (contact.receiveBroadcasts.not()) {
        return listOf()
    }
    return with(Dispatchers.IO) {
        val currentUser = sharedPreferences.getUserData()!!
        when (val idsCall = safeApiCall {
            getInstance("https://${currentUser.address.getMailHost()}").getAllBroadcastMessagesIdsForContact(
                sotnHeader = currentUser.sign(),
                hostPart = contact.address.getHost(),
                localPart = contact.address.getLocal()
            )
        }) {
            is HttpResult.Error -> {
                listOf()
            }

            is HttpResult.Success -> {
                idsCall.data?.let { ids ->
                    fetchEnvelopesForContact(
                        messageIds = ids,
                        currentUser = currentUser,
                        contact = contact,
                        link = null
                    )
                } ?: listOf()
            }
        }
    }
}

private suspend fun getAllPrivateEnvelopesForContact(
    sharedPreferences: SharedPreferences,
    contact: DBContact
): List<Envelope> {
    return withContext(Dispatchers.IO) {
        val currentUser = sharedPreferences.getUserData()!!
        when (val idsCall = safeApiCall {
            getInstance("https://${currentUser.address.getMailHost()}").getAllPrivateMessagesIdsForContact(
                sotnHeader = currentUser.sign(),
                hostPart = contact.address.getHost(),
                localPart = contact.address.getLocal(),
                connectionLink = contact.address.connectionLink()
            )
        }) {
            is HttpResult.Error -> {
                listOf()
            }

            is HttpResult.Success -> {
                idsCall.data?.let { ids ->
                    fetchEnvelopesForContact(
                        messageIds = ids,
                        currentUser = currentUser,
                        contact = contact,
                        link = contact.address.connectionLink()
                    )
                } ?: listOf()
            }
        }
    }
}

private suspend fun getAllPrivateEnvelopes(
    sharedPreferences: SharedPreferences,
    contactsDao: ContactsDao
): List<Envelope> {
    return withContext(Dispatchers.IO) {
        contactsDao.getAll().map { contact ->
            async {
                getAllPrivateEnvelopesForContact(sharedPreferences, contact)
            }
        }.awaitAll()
            .fold(initial = arrayListOf(), operation = { initial, new ->
                initial.apply { addAll(new) }
            })
    }
}

suspend fun downloadMessage(
    currentUser: UserData,
    contactAddress: Address,
    messageId: String
): Response<ResponseBody> {
    return getInstance("https://${currentUser.address.getMailHost()}").downloadMessage(
        sotnHeader = currentUser.sign(),
        hostPart = contactAddress.getHost(),
        localPart = contactAddress.getLocal(),
        messageId = messageId
    )
}

suspend fun downloadMessage(
    currentUser: UserData,
    contact: DBContact,
    messageId: String
): Response<ResponseBody> {
    return downloadMessage(currentUser, contact.address, messageId)
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
                                envelopeCall.headers!!
                            )

                        val authentic: Boolean = try {
                            envelope.assertEnvelopeAuthenticity()
                            true
                        } catch (e: EnvelopeAuthenticity) {
                            false
                        } catch (e: SignatureMismatch) {
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
    dl: Downloader
) {
    withContext(Dispatchers.IO) {
        val privateEnvelopes: Deferred<List<Envelope>> = async {
            getAllPrivateEnvelopesForContact(
                sp,
                contact
            )
        }
        val broadcastEnvelopes: Deferred<List<Envelope>> = async {
            getAllBroadcastEnvelopesForContact(
                sp,
                contact
            )
        }

        val results: List<Envelope> =
            listOf(privateEnvelopes, broadcastEnvelopes).awaitAll().fold(
                initial = arrayListOf(),
                operation = { initial, new -> initial.apply { addAll(new) } })

        saveMessagesToDb(dl, results, db.messagesDao(), db.attachmentsDao())
    }
}

suspend fun syncAllMessages(db: AppDatabase, sp: SharedPreferences, dl: Downloader) {

    withContext(Dispatchers.IO) {
        val privateEnvelopes: Deferred<List<Envelope>> = async {
            getAllPrivateEnvelopes(
                sp,
                db.userDao()
            )
        }

        val broadcastEnvelopes: Deferred<List<Envelope>> = async {
            getAllBroadcastEnvelopes(
                sp,
                db.userDao()
            )
        }

        val results: List<Envelope> =
            listOf(privateEnvelopes, broadcastEnvelopes).awaitAll().fold(
                initial = arrayListOf(),
                operation = { initial, new -> initial.apply { addAll(new) } })

        saveMessagesToDb(dl, results, db.messagesDao(), db.attachmentsDao())
    }
}

suspend fun uploadMessage(
    composingData: ComposingData,
    fileUtils: FileUtils,
    currentUser: UserData,
    currentUserPublicData: PublicUserData,
    db: AppDatabase,
    isBroadcast: Boolean,
    replyToSubjectId: String?
) {
    withContext(Dispatchers.IO) {
        val rootMessageId = currentUser.newMessageId()
        val sendingDate = Instant.now()
        val accessProfiles =
            if (isBroadcast)
                null
            else
                arrayListOf(currentUserPublicData).apply {
                    addAll(
                        composingData.recipients
                    )
                }

        val pendingRootMessage = DBPendingRootMessage(
            messageId = rootMessageId,
            subjectId = replyToSubjectId,
            timestamp = sendingDate.toEpochMilli(),
            subject = composingData.subject,
            checksum = composingData.body.hashedWithSha256().first,
            category = MessageCategory.personal.name,
            size = composingData.body.toByteArray().size.toLong(),
            authorAddress = currentUser.address,
            textBody = composingData.body,
            isBroadcast = isBroadcast
        )

        val fileParts = arrayListOf<DBPendingAttachment>()

        composingData.attachments.forEach { attachment ->
            val uri = fileUtils.getUriForFile(attachment)
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
                        subject = composingData.subject,
                        isBroadcast = isBroadcast
                    )
                )
            } else {
                //attachment too large. Split in chunks

                var partCounter = 1
                val buffer = ByteArray(MAX_MESSAGE_SIZE.toInt())
                val totalParts = (urlInfo.size + MAX_MESSAGE_SIZE - 1) / urlInfo.size
                var offset: Long = 0

                FileInputStream(attachment).use { inputStream ->
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
                                subject = composingData.subject,
                                isBroadcast = isBroadcast
                            )
                        )
                        offset += bytesRead
                        partCounter++
                    }
                }
            }
        }

        db.pendingMessagesDao().insert(pendingRootMessage)
        db.pendingAttachmentsDao().insertAll(fileParts)
        if (!isBroadcast) {
            db.pendingReadersDao()
                .insertAll(accessProfiles!!.map { it.toDBPendingReaderPublicData(rootMessageId) })
        }
        uploadPendingMessages(currentUser, db, fileUtils)
    }
}


suspend fun uploadPendingMessages(currentUser: UserData, db: AppDatabase, fileUtils: FileUtils) {
    withContext(Dispatchers.IO) {
        val pendingMessages = db.pendingMessagesDao().getAll()
        val results: List<UploadResult> =
            pendingMessages.map { pendingMessage ->
                async {
                    arrayListOf(
                        //root message
                        async {
                            UploadResult(
                                messageId = pendingMessage.message.messageId,
                                isAttachment = false,
                                result = safeApiCall {
                                    uploadRootMessage(
                                        pendingRootMessage = pendingMessage.message,
                                        recipients = pendingMessage.readers,
                                        content = pendingMessage.getRootContentHeaders(),
                                        currentUser = currentUser
                                    )
                                })
                        }
                    ).apply {
                        //attachments
                        addAll(pendingMessage.fileParts.map { part ->
                            async {
                                UploadResult(
                                    messageId = part.messageId,
                                    isAttachment = true,
                                    result = safeApiCall {
                                        uploadFileMessage(
                                            currentUser = currentUser,
                                            pendingAttachment = part,
                                            readers = pendingMessage.readers,
                                            fileUtils = fileUtils
                                        )
                                    })
                            }
                        })
                    }.awaitAll()
                }
            }.awaitAll().fold(
                initial = arrayListOf(),
                operation = { initial, new -> initial.apply { addAll(new) } })


        val everyPartUploaded: Boolean =
            results.any { it.result !is HttpResult.Success }.not()

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
                initial.apply { addAll(new.fileParts.map { Uri.parse(it.uri) }) }
            }).forEach { uri ->
                fileUtils.getFileFromUri(uri)?.delete()
            }
        } else {
            results.filter { it.result is HttpResult.Success }.forEach { successful ->
                if (successful.isAttachment) {
                    db.pendingAttachmentsDao().delete(successful.messageId)
                    pendingMessages.forEach { message ->
                        message.fileParts.firstOrNull { it.messageId == successful.messageId }?.uri?.let { uriStr ->
                            fileUtils.getFileFromUri(
                                Uri.parse(uriStr)
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
    val result: HttpResult<Void>
)

private suspend fun uploadRootMessage(
    pendingRootMessage: DBPendingRootMessage,
    recipients: List<DBPendingReaderPublicData>,
    content: ContentHeaders,
    currentUser: UserData
): Response<Void> {

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

    return getInstance("https://${currentUser.address.getMailHost()}").uploadMessageFile(
        sotnHeader = currentUser.sign(),
        contentLength = pendingRootMessage.textBody.toByteArray().size.toLong(),
        headers = envelopeHeadersMap.filter { it.key.startsWith(HEADER_PREFIX) },
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal(),
        file = sealedBody!!.toRequestBody("application/octet-stream".toMediaTypeOrNull())
    )
}

private suspend fun uploadFileMessage(
    currentUser: UserData,
    pendingAttachment: DBPendingAttachment,
    readers: List<DBPendingReaderPublicData>,
    fileUtils: FileUtils,
): Response<Void> {
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
            )
        else
            fileUtils.encryptFilePartXChaCha20Poly1305(
                inputUri = urlInfo.uri!!,
                secretKey = accessKey,
                bytesCount = pendingAttachment.partSize,
                offset = pendingAttachment.offset ?: 0
            )!!

    return getInstance("https://${currentUser.address.getMailHost()}").uploadMessageFile(
        sotnHeader = currentUser.sign(),
        contentLength = urlInfo.size,
        headers = envelopeHeadersMap.filter { it.key.startsWith(HEADER_PREFIX) },
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal(),
        file = encryptedData!!.toRequestBody("application/octet-stream".toMediaTypeOrNull())
    )
}

suspend fun saveMessagesToDb(
    dl: Downloader,
    results: List<Envelope>,
    messagesDao: MessagesDao,
    attachmentsDao: AttachmentsDao
) {
    withContext(Dispatchers.IO) {
        launch {

            val saved = messagesDao.getAll()

            val newResults =
                results.filterNot { envelope -> saved.any { dbMessage -> dbMessage.messageId == envelope.messageId } }

            val removed =
                saved.filterNot { dbMessage -> results.any { envelope -> envelope.messageId == dbMessage.messageId } }

            messagesDao.deleteList(removed)

            val envelopesPair = newResults.partition { envelope -> envelope.isRootMessage() }

            val rootEnvelopes = envelopesPair.first
            val attachmentEnvelopes = envelopesPair.second

            val rootMessages = dl.downloadMessagesPayload(rootEnvelopes).awaitAll()

            val attachments: List<DBAttachment> = rootMessages.map { root ->
                val headers = root.first.contentHeaders
                headers.fileParts?.filter { fileInfo ->
                    attachmentEnvelopes.firstOrNull { it.messageId == fileInfo.messageId }?.accessKey != null
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

            messagesDao.insertAll(
                rootMessages.map {
                    DBMessage(
                        messageId = it.first.messageId,
                        authorAddress = it.first.contact.address,
                        subject = it.first.contentHeaders.subject,
                        textBody = it.second ?: "",
                        isBroadcast = it.first.isBroadcast(),
                        timestamp = it.first.contentHeaders.date.toEpochMilli(),
                        readerAddresses = it.first.contentHeaders.readersAddresses?.joinToString(",")
                    )
                })
            attachmentsDao.insertAll(attachments)
        }
    }
}

suspend fun syncContacts(sp: SharedPreferences, dao: ContactsDao) {
    withContext(Dispatchers.IO) {
        when (val remoteAddressesCall = safeApiCall { getAllContacts(sp) }) {
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
                                safeApiCall { getProfilePublicData(remoteAddress) }) {
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
                        local.address == sp.getUserAddress() || result.any { remote -> remote.address == local.address }
                    })

                    dao.insertAll(result.map { publicData ->
                        DBContact(
                            lastSeen = publicData.lastSeen?.toString(),
                            updated = publicData.updated?.toString(),
                            address = publicData.address,
                            name = publicData.fullName,
                            receiveBroadcasts = true,
                            signingKeyAlgorithm = publicData.signingKeyAlgorithm,
                            encryptionKeyAlgorithm = publicData.encryptionKeyAlgorithm,
                            publicEncryptionKey = publicData.publicEncryptionKey,
                            publicSigningKey = publicData.publicSigningKey,
                            publicEncryptionKeyId = publicData.encryptionKeyId,
                            lastSeenPublic = publicData.lastSeenPublic,
                            //TODO get image
                            imageUrl = null
                        )
                    })
                }
            }
        }
    }
}

suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>): HttpResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val response = call.invoke()
            if (response.isSuccessful) {
                HttpResult.Success(
                    response.body(),
                    response.message(),
                    response.code(),
                    response.headers()
                )
            } else {
                HttpResult.Error(response.message(), response.code(), response.headers())
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