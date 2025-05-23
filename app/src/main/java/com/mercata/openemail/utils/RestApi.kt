package com.mercata.openemail.utils

import com.mercata.openemail.WELL_KNOWN_URI
import com.mercata.openemail.models.Link
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.response_converters.ContactsList
import com.mercata.openemail.response_converters.EnvelopeIdsList
import com.mercata.openemail.response_converters.UserPublicData
import com.mercata.openemail.response_converters.WellKnownHost
import com.mercata.openemail.response_converters.WellKnownHosts
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @DELETE("/account/{hostPart}/{localPart}")
    suspend fun deleteAccount(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<Void>

    @PUT("/home/{hostPart}/{localPart}/profile")
    suspend fun updateUser(
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

    @PUT("/home/{hostPart}/{localPart}/image")
    suspend fun uploadUserImage(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Body body: RequestBody?
    ): Response<Void>

    @DELETE("/home/{hostPart}/{localPart}/image")
    suspend fun deleteUserImage(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<Void>

    @PUT("/mail/{hostPart}/{localPart}/link/{link}/notifications")
    suspend fun notifyAddress(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("link") link: String,
        @Body body: RequestBody
    ): Response<Void>

    @GET("/home/{hostPart}/{localPart}/links")
    @ContactsList
    suspend fun getAllContactLinks(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<List<Link>>

    @PUT("/home/{hostPart}/{localPart}/links/{link}")
    suspend fun updateContactLink(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("link") link: String,
        @Body body: RequestBody
    ): Response<Void>

    @GET("/mail/{hostPart}/{localPart}/image")
    suspend fun checkUserImage(
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<Void>

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
    suspend fun uploadMessageFile(
        @Header("Authorization") sotnHeader: String,
        @Header("Content-Length") contentLength: Long,
        @HeaderMap headers: Map<String, String>,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Body file: RequestBody
    ): Response<Void>

    @DELETE("/home/{hostPart}/{localPart}/messages/{messageId}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun revokeMessage(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
        @Path("messageId") messageId: String,
    ): Response<Void>


    @GET("/home/{hostPart}/{localPart}/notifications")
    @Headers("Content-Type: application/octet-stream")
    suspend fun getNotifications(
        @Header("Authorization") sotnHeader: String,
        @Path("hostPart") hostPart: String,
        @Path("localPart") localPart: String,
    ): Response<String>
}

