package com.mercata.pingworks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

private fun getInstance(baseUrl: String): RestApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(RestApi::class.java)

suspend fun getWellKnownHosts(hostName: String): Response<String> {
    return getInstance("https://$hostName/").getWellKnownHosts()
}

suspend fun isAddressAvailable(hostname: String, localName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://$MAIL_HOST/account/${hostname}/${localName}"
            val response = getInstance("https://$MAIL_HOST").isAddressAvailable(url)
            response.isSuccessful
        } catch (e: HttpException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}

suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>): HttpResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val response = call.invoke()
            if (response.isSuccessful) {
                HttpResult.Success(response.body(), response.message(), response.code(), response.headers())
            } else {
                HttpResult.Error(response.message(), response.code(), response.headers())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            HttpResult.Error(e.message, -1, null)
        }
    }

sealed class HttpResult<out T : Any>(open val message: String?, open val code: Int?, open val headers: Headers?) {
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