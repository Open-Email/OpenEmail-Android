package com.mercata.pingworks.response_converters

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

annotation class EnvelopeIdsList

class EnvelopeIdsListConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (annotations.any { it is EnvelopeIdsList }) {
            EnvelopeIdsListConverter()
        } else {
            null
        }
    }
}

class EnvelopeIdsListConverter : Converter<ResponseBody, List<String>> {
    override fun convert(value: ResponseBody): List<String> {
        return value.string()
            .split("\n")
            .map { it.trim() }
            .filterNot { it.isBlank() }
    }
}