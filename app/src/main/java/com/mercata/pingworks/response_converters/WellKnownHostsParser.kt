package com.mercata.pingworks.response_converters

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

annotation class WellKnownHosts
class WellKnownHostsConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (annotations.any { it is WellKnownHosts }) {
            WellKnownHostsConverter()
        } else {
            null
        }
    }
}

class WellKnownHostsConverter : Converter<ResponseBody, List<WellKnownHost>> {
    override fun convert(value: ResponseBody): List<WellKnownHost> {
        return value.string().split("\n").filterNot { it.startsWith("#") || it.isBlank() }
    }
}

typealias WellKnownHost = String