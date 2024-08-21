package com.mercata.pingworks

fun String.getHost(): String = this.substringAfter("@")
fun String.getLocal(): String = this.substringBefore("@")