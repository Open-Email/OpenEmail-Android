package com.mercata.pingworks.utils

import android.util.Log
import com.mercata.pingworks.ANONYMOUS_ENCRYPTION_CIPHER
import com.mercata.pingworks.BuildConfig
import com.mercata.pingworks.DEFAULT_MAIL_SUBDOMAIN
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.AttachmentsDao
import com.mercata.pingworks.db.contacts.ContactsDao
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.messages.DBAttachment
import com.mercata.pingworks.db.messages.DBMessage
import com.mercata.pingworks.db.messages.MessagesDao
import com.mercata.pingworks.exceptions.EnvelopeAuthenticity
import com.mercata.pingworks.exceptions.SignatureMismatch
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.models.PublicUserData
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
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
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
    val link = contact.address.generateLink()

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

suspend fun getAllContacts(sharedPreferences: SharedPreferences): Response<List<String>> {
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
        linkAddr = contact.address.generateLink()
    )
}

suspend fun getAllBroadcastEnvelopes(
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

suspend fun getAllBroadcastEnvelopesForContact(
    sharedPreferences: SharedPreferences,
    contact: DBContact
): List<Envelope> {
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

suspend fun getAllPrivateEnvelopesForContact(
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
                connectionLink = contact.address.generateLink()
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
                        link = contact.address.generateLink()
                    )
                } ?: listOf()
            }
        }
    }
}

suspend fun getAllPrivateEnvelopes(
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
                            Envelope(messageId, currentUser, contact, envelopeCall.headers!!)

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

suspend fun saveMessagesToDb(
    dl: Downloader,
    results: List<Envelope>,
    messagesDao: MessagesDao,
    attachmentsDao: AttachmentsDao
) {
    withContext(Dispatchers.IO) {
        launch {
            val rootMessages =
                dl.downloadMessagesPayload(results.filter { it.isRootMessage() }).awaitAll()
            val attachmentEnvelopes = results.filter { !it.isRootMessage() }
            val attachments: ArrayList<DBAttachment> = arrayListOf()
            rootMessages.forEach { root ->
                val headers = root.first.contentHeaders
                headers.files.map { fileInfo ->
                    attachments.add(
                        DBAttachment(
                            //TODO multipart
                            attachmentMessageId = fileInfo.messageIds.first(),
                            authorAddress = root.first.contact.address,
                            parentId = root.first.messageId,
                            name = fileInfo.name,
                            type = fileInfo.mimeType,
                            size = fileInfo.size,
                            accessKeyHex = attachmentEnvelopes.first { it.messageId == fileInfo.messageIds.first() }.accessKey!!.asHexString,
                            createdTimestamp = fileInfo.modifiedAt
                        )
                    )
                }
            }
            messagesDao.insertAll(
                rootMessages.map {
                    DBMessage(
                        messageId = it.first.messageId,
                        authorAddress = it.first.contact.address,
                        subject = it.first.contentHeaders.subject,
                        textBody = it.second ?: "",
                        isBroadcast = it.first.isBroadcast(),
                        timestamp = it.first.contentHeaders.date.toEpochMilli()
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
                        result.any { remote -> remote.address == local.address }
                    })

                    //inserting new and updating old local contacts
                    val broadcastReceivingAddresses =
                        dao.getAll().filter { it.receiveBroadcasts }.map { it.address }
                    dao.insertAll(result.map { publicData ->
                        DBContact(
                            lastSeen = publicData.lastSeen?.toString(),
                            updated = publicData.updated?.toString(),
                            address = publicData.address,
                            name = publicData.fullName,
                            receiveBroadcasts = broadcastReceivingAddresses.contains(
                                publicData.address
                            ),
                            signingKeyAlgorithm = publicData.signingKeyAlgorithm,
                            encryptionKeyAlgorithm = publicData.encryptionKeyAlgorithm,
                            publicEncryptionKey = publicData.publicEncryptionKey,
                            publicSigningKey = publicData.publicSigningKey,
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