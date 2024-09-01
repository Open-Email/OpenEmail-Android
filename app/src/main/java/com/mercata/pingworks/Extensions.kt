package com.mercata.pingworks


fun Address.getHost(): String = this.substringAfter("@")
fun Address.getLocal(): String = this.substringBefore("@")