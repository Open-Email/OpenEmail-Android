package com.mercata.pingworks

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface RestApi {

    @GET(WELL_KNOWN_URI)
    suspend fun getWellKnownHosts(): Response<String>

    @HEAD
    suspend fun isAddressAvailable(@Url url: String): Response<Void>

    @POST
    suspend fun register(
        @Header("Authorization") sotnHeader: String,
        @Url url: String,
        @Body body: RequestBody
    ): Response<Void>
}