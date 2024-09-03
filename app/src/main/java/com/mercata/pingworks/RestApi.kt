package com.mercata.pingworks

import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.response_converters.ContactsList
import com.mercata.pingworks.response_converters.UserPublicData
import com.mercata.pingworks.response_converters.WellKnownHost
import com.mercata.pingworks.response_converters.WellKnownHosts
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

interface RestApi {

    @GET(WELL_KNOWN_URI)
    @WellKnownHosts
    suspend fun getWellKnownHosts(): Response<List<WellKnownHost>>

    @HEAD("/account/{hostPart}/{localPart}")
    suspend fun isAddressAvailable(
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String
    ): Response<Void>

    @POST("/account/{hostPart}/{localPart}")
    suspend fun register(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Body body: RequestBody
    ): Response<Void>

    @HEAD("/home/{hostPart}/{localPart}")
    suspend fun login(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<Void>

    @GET("/mail/{hostPart}/{localPart}/profile")
    @UserPublicData
    suspend fun getProfilePublicData(
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String
    ): Response<PublicUserData>


    @PUT("/home/{hostPart}/{localPart}/links/{link}")
    suspend fun uploadContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("link") link: String,
        @Body body: RequestBody
    ): Response<Void>

    @GET("/home/{hostPart}/{localPart}/links")
    @ContactsList
    suspend fun getAllContacts(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<List<String>>

    @GET("/mail/{hostPart}/{localPart}/messages/{messageId}")
    suspend fun getAllBroadcastMessagesForContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String,
    ): Response<String>

    @GET("/mail/{hostPart}/{localPart}/messages")
    suspend fun getAllBroadcastMessagesIdsForContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<String>

    @HEAD("/mail/{hostPart}/{localPart}/messages/{messageId}")
    suspend fun fetchEnvelope(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String,
    ): Response<Void>

    @DELETE("/home/{hostPart}/{localPart}/links/{linkAddr}")
    suspend fun deleteContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("linkAddr") linkAddr: String,
    ): Response<Void>
}

