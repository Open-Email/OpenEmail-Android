package com.mercata.pingworks

import com.mercata.pingworks.models.Address

fun Address.getHost(): String = this.substringAfter("@")
fun Address.getLocal(): String = this.substringBefore("@")