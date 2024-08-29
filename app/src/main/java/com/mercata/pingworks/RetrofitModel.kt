package com.mercata.pingworks

import android.util.Log
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
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
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


private fun getInstance(baseUrl: String): RestApi {

    val rv = Retrofit.Builder()
        .baseUrl(baseUrl)
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

suspend fun isAddressAvailable(hostname: String, localName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val host = "$DEFAULT_MAIL_SUBDOMAIN.${hostname}"
            val url = "https://$host/account/${hostname}/${localName}"
            val response = getInstance("https://$host").isAddressAvailable(url)
            response.isSuccessful
        } catch (e: HttpException) {
            false
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            false
        }
    }
}

suspend fun registerCall(user: UserData): String? {
    if (BuildConfig.DEBUG) {
        Log.i(
            "KEYS", "\n" +
                    "signPrivate: ${user.signingKeys.privateKey}\n" +
                    "signPublic: ${user.signingKeys.publicKey}\n" +
                    "encryptPrivate: ${user.encryptionKeys.privateKey}\n" +
                    "encryptPublic: ${user.encryptionKeys.publicKey}\n"
        )
    }

    return withContext(Dispatchers.IO) {
        try {
            val url =
                "https://${user.getHost()}/account/${user.address.getHost()}/${user.address.getLocal()}"

            val authNonce: String = sign(user)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

            val postData = """
            Name: ${user.name}
            Encryption-Key: id=${user.encryptionKeys.id}; algorithm=${ANONYMOUS_ENCRYPTION_CIPHER}; value=${user.encryptionKeys.publicKey}
            Signing-Key: algorithm=${SIGNING_ALGORITHM}; value=${user.signingKeys.publicKey}
            Updated: ${LocalDateTime.now().atOffset(ZoneOffset.UTC).format(formatter)}
            """.trimIndent()

            val response = getInstance("https://${user.getHost()}").register(
                sotnHeader = authNonce,
                url = url,
                body = postData.toRequestBody()
            )
            if (response.isSuccessful) {
                null
            } else {
                response.code().toString()
            }
        } catch (e: HttpException) {
            e.message()
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            e.localizedMessage
        }
    }
}

suspend fun loginCall(user: UserData): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url =
                "https://${user.getHost()}/home/${user.address.getHost()}/${user.address.getLocal()}"
            val authNonce: String = sign(user)
            val response = getInstance("https://${user.getHost()}").login(authNonce, url)
            if (response.isSuccessful) {
                null
            } else {
                response.code().toString()
            }
        } catch (e: HttpException) {
            e.message()
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            e.localizedMessage
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