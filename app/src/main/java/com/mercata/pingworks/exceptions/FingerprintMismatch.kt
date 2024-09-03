package com.mercata.pingworks.exceptions

import com.mercata.pingworks.exceptions.abstract_exception.MessageException

class FingerprintMismatch(message: String): MessageException(message)
