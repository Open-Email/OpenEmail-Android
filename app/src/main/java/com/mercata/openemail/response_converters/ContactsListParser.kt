package com.mercata.openemail.response_converters

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

class ContactsListConverter : Converter<ResponseBody, List<String>> {
    override fun convert(value: ResponseBody): List<String> {
        val sp: SharedPreferences by inject(SharedPreferences::class.java)
        val pairs: List<String> =
            value.string()
                .splitToSequence("\n")
                .map { it.trim() }
                .filterNot { it.isBlank() }
                .map { part ->
                    val parts = part
                        .splitToSequence(",")
                        .map { it.trim() }
                        .filterNot { it.isBlank() }
                    val decrypted = decryptAnonymous(parts.last(), sp.getUserData()!!)
                    String(decrypted, charset = UTF_8)
                }.toList()

        return pairs
    }
}
