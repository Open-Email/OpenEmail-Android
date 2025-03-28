package com.mercata.openemail.response_converters

import com.mercata.openemail.models.Link
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.decryptAnonymous
import okhttp3.ResponseBody
import org.koin.java.KoinJavaComponent.inject
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import kotlin.text.Charsets.UTF_8


annotation class ContactsList

class ContactsListConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (annotations.any { it is ContactsList }) {
            ContactsListConverter()
        } else {
            null
        }
    }
}

class ContactsListConverter : Converter<ResponseBody, List<Link>> {
    override fun convert(value: ResponseBody): List<Link> {
        val sp: SharedPreferences by inject(SharedPreferences::class.java)
        return value.string()
            .splitToSequence("\n")
            .map { it.trim() }
            .filterNot { it.isBlank() }
            .map { part ->
                val parts = part
                    .splitToSequence(",")
                    .map { it.trim() }
                    .filterNot { it.isBlank() }
                val decrypted =
                    String(decryptAnonymous(parts.last(), sp.getUserData()!!), charset = UTF_8)

                if (decrypted.contains("=")) {
                    val attributesMap: Map<String, Any?> = decrypted.split(";").associate { attr ->
                        val keyValue = attr.split("=")
                        if (keyValue.size == 2) {
                            keyValue.first().lowercase() to keyValue.last().lowercase()
                        } else {
                            "address" to keyValue.first().lowercase()
                        }
                    }
                    val allowedBroadcasts = attributesMap["broadcasts"] != "no"

                    Link(
                        link = parts.first(),
                        address = attributesMap["address"]!!.toString(),
                        allowedBroadcasts = allowedBroadcasts
                    )
                } else {
                    Link(link = parts.first(), address = decrypted, allowedBroadcasts = true)
                }

            }.toList()

    }
}
