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
            .associate {
                it.substringBefore(": ").lowercase() to it.substringAfter(": ")
            }

        if (!map.containsKey("Encryption-Key".lowercase()) || !map.containsKey("Signing-Key".lowercase())) return null

        val encryptionData = map["Encryption-Key".lowercase()]?.splitToSequence("; ")
            ?.associate {
                it.substringBefore("=").lowercase() to it.substringAfter("=")
            } as Map

        val signingData = map["Signing-Key".lowercase()]?.splitToSequence("; ")
            ?.associate {
                it.substringBefore("=").lowercase() to it.substringAfter("=")
            } as Map

        val lastSigningData: Map<String, String>? =
            map["Last-Signing-Key".lowercase()]?.splitToSequence("; ")
                ?.associate {
                    it.substringBefore("=").lowercase() to it.substringAfter("=")
                }

        return PublicUserData(
            address = address,
            fullName = map["Name".lowercase()] ?: "",
            lastSeenPublic = map["Last-Seen-Public".lowercase()]?.let { it == "Yes" } ?: true,
            lastSeen = map["Last-Seen".lowercase()]?.run { Instant.parse(this) },
            updated = map["Updated".lowercase()]?.run { Instant.parse(this) },
            encryptionKeyId = encryptionData["id".lowercase()]!!,
            encryptionKeyAlgorithm = encryptionData["algorithm".lowercase()]!!,
            publicEncryptionKey = encryptionData["value".lowercase()]!!,
            signingKeyAlgorithm = signingData["algorithm".lowercase()]!!,
            publicSigningKey = signingData["value".lowercase()]!!,
            lastSigningKey = lastSigningData?.get("value".lowercase()),
            lastSigningKeyAlgorithm = lastSigningData?.get("algorithm".lowercase()),
            away = map["Away".lowercase()]?.equals("Yes", true),
            publicAccess = map["Public-Access".lowercase()]?.equals("Yes", true),
            publicLinks = map["Public-Links".lowercase()]?.equals("Yes", true),
            awayWarning = map["Away-Warning".lowercase()],
            status = map["Status".lowercase()],
            about = map["About".lowercase()],
            gender = map["Gender".lowercase()],
            language = map["Languages".lowercase()],
            relationshipStatus = map["Relationship-Status".lowercase()],
            education = map["Education".lowercase()],
            placesLived = map["Places-Lived".lowercase()],
            notes = map["Notes".lowercase()],
            work = map["Work".lowercase()],
            department = map["Department".lowercase()],
            organization = map["Organization".lowercase()],
            jobTitle = map["Job-Title".lowercase()],
            interests = map["Interests".lowercase()],
            books = map["Books".lowercase()],
            music = map["Music".lowercase()],
            movies = map["Movies".lowercase()],
            sports = map["Sports".lowercase()],
            website = map["Website".lowercase()],
            mailingAddress = map["Mailing-Address".lowercase()],
            location = map["Location".lowercase()],
            phone = map["Phone".lowercase()],
            streams = map["Streams".lowercase()],
        )
    }
}
