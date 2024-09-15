package com.mercata.pingworks.utils

import com.mercata.pingworks.WELL_KNOWN_URI
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.response_converters.ContactsList
import com.mercata.pingworks.response_converters.EnvelopeIdsList
import com.mercata.pingworks.response_converters.UserPublicData
import com.mercata.pingworks.response_converters.WellKnownHost
import com.mercata.pingworks.response_converters.WellKnownHosts
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

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

    @GET("/mail/{hostPart}/{localPart}/messages")
    @EnvelopeIdsList
    suspend fun getAllBroadcastMessagesIdsForContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<List<String>>

    @GET("/mail/{hostPart}/{localPart}/link/{connectionLink}/messages")
    @EnvelopeIdsList
    suspend fun getAllPrivateMessagesIdsForContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("connectionLink") connectionLink: String,
    ): Response<List<String>>

    @DELETE("/home/{hostPart}/{localPart}/links/{linkAddr}")
    suspend fun deleteContact(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("linkAddr") linkAddr: String,
    ): Response<Void>

    @HEAD("/mail/{hostPart}/{localPart}/messages/{messageId}")
    suspend fun fetchBroadcastEnvelope(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String,
    ): Response<Void>

    @HEAD("/mail/{hostPart}/{localPart}/link/{connectionLink}/messages/{messageId}")
    suspend fun fetchPrivateEnvelope(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String,
        @Path("connectionLink") connectionLink: String,
    ): Response<Void>

    @Streaming
    @GET("/mail/{hostPart}/{localPart}/messages/{messageId}")
    suspend fun downloadMessage(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String
    ): Response<ResponseBody>

    @POST("/home/{hostPart}/{localPart}/messages")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadRootMessage(
        @Header("Authorization") sotnHeader: String,
        @Header("Content-Length") contentLength: Int,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<String>

    @POST("/home/{hostPart}/{localPart}/messages")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadAttachmentFile(
        @Header("Authorization") sotnHeader: String,
        @Header("Content-Length") contentLength: Int,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Part file: MultipartBody.Part
    ): Response<String>
}

