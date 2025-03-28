package com.mercata.openemail.models

import com.mercata.openemail.utils.Address

data class Link(val address: Address, val link: String, val allowedBroadcasts: Boolean)
