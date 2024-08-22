package com.mercata.pingworks.response_converters

import com.mercata.pingworks.models.PublicUserData
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

annotation class UserPublicData

class UserPublicDataConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val hasJsonParser = annotations.any { it is UserPublicData }
        return if (hasJsonParser) {
            UserPublicDataConverter()
        } else {
            null
        }
    }
}

class UserPublicDataConverter : Converter<ResponseBody, PublicUserData> {
    override fun convert(value: ResponseBody): PublicUserData? {
        val map = value.string().split("\n").filterNot { it.startsWith("#") }
            .associate { it.substringBefore(": ") to it.substringAfter(": ") }

        if (!map.containsKey("Encryption-Key") || !map.containsKey("Signing-Key")) return null

        val encryptionData = map["Encryption-Key"]?.split("; ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=") } as Map

        val signingData = map["Signing-Key"]?.split("; ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=") } as Map

        return PublicUserData(
            fullName = map["Name"] ?: "",
            lastSeenPublic = map["Last-Seen-Public"] == "Yes",
            lastSeen = map["Last-Seen"]?.run {
                ZonedDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(
                    ZoneId.systemDefault()
                )
            },
            updated = map["Updated"]?.run {
                ZonedDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(
                    ZoneId.systemDefault()
                )
            },
            encryptionKeyId = encryptionData["id"]!!,
            encryptionKeyAlgorithm = encryptionData["algorithm"]!!,
            publicEncryptionKey = encryptionData["value"]!!,
            signingKeyAlgorithm = signingData["algorithm"]!!,
            publicSigningKey = signingData["value"]!!
        )
    }
}
