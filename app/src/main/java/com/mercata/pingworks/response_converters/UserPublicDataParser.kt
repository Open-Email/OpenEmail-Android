package com.mercata.pingworks.response_converters

import com.mercata.pingworks.models.PublicUserData
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.time.Instant

annotation class UserPublicData

class UserPublicDataConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (annotations.any { it is UserPublicData }) {
            UserPublicDataConverter()
        } else {
            null
        }
    }
}

class UserPublicDataConverter : Converter<ResponseBody, PublicUserData> {
    override fun convert(value: ResponseBody): PublicUserData? {
        val split = value.string().splitToSequence("\n")
        val address = split.first().substringAfter("Profile of ")
        val map = split.filterNot { it.startsWith("#") }
            .associate { it.substringBefore(": ") to it.substringAfter(": ") }

        if (!map.containsKey("Encryption-Key") || !map.containsKey("Signing-Key")) return null

        val encryptionData = map["Encryption-Key"]?.splitToSequence("; ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=") } as Map

        val signingData = map["Signing-Key"]?.splitToSequence("; ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=") } as Map

        val lastSigningData: Map<String, String>? = map["Last-Signing-Key"]?.splitToSequence("; ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=") }

        return PublicUserData(
            address = address,
            fullName = map["Name"] ?: "",
            lastSeenPublic = map["Last-Seen-Public"] == "Yes",
            lastSeen = map["Last-Seen"]?.run { Instant.parse(this) },
            updated = map["Updated"]?.run { Instant.parse(this) },
            encryptionKeyId = encryptionData["id"]!!,
            encryptionKeyAlgorithm = encryptionData["algorithm"]!!,
            publicEncryptionKey = encryptionData["value"]!!,
            signingKeyAlgorithm = signingData["algorithm"]!!,
            publicSigningKey = signingData["value"]!!,
            lastSigningKey = lastSigningData?.get("value"),
            lastSigningKeyAlgorithm = lastSigningData?.get("algorithm"),
        )
    }
}
