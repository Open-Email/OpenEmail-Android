package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.MessageException

class FingerprintMismatch(message: String): MessageException(message)
