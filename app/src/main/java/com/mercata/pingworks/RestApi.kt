package com.mercata.pingworks

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Url

interface RestApi {

    @GET(WELL_KNOWN_URI)
    suspend fun getWellKnownHosts(): Response<String>

    @HEAD
    suspend fun isAddressAvailable(@Url url: String): Response<Void>
}