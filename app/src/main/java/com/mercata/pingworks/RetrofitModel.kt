package com.mercata.pingworks

import android.util.Log
import com.mercata.pingworks.models.Person
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.getMailHost
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.response_converters.ContactsListConverterFactory
import com.mercata.pingworks.response_converters.UserPublicDataConverterFactory
import com.mercata.pingworks.response_converters.WellKnownHost
import com.mercata.pingworks.response_converters.WellKnownHostsConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


private fun getInstance(baseUrl: String): RestApi {

    val rv = Retrofit.Builder()
        .baseUrl(baseUrl)
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
                    "signPrivate: ${user.signingKeys.privateKey}\n" +
                    "signPublic: ${user.signingKeys.publicKey}\n" +
                    "encryptPrivate: ${user.encryptionKeys.privateKey}\n" +
                    "encryptPublic: ${user.encryptionKeys.publicKey}\n"
        )
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

    val postData = """
            Name: ${user.name}
            Encryption-Key: id=${user.encryptionKeys.id}; algorithm=${ANONYMOUS_ENCRYPTION_CIPHER}; value=${user.encryptionKeys.publicKey}
            Signing-Key: algorithm=${SIGNING_ALGORITHM}; value=${user.signingKeys.publicKey}
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

suspend fun deleteContact(contact: Person, sharedPreferences: SharedPreferences): Response<Void> {
    val currentUser = sharedPreferences.getUserData()!!
    return getInstance("https://${currentUser.address.getMailHost()}").deleteContact(
        sotnHeader = currentUser.sign(),
        hostPart = currentUser.address.getHost(),
        localPart = currentUser.address.getLocal(),
        linkAddr = contact.address.generateLink()
    )
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