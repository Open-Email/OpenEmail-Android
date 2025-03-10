package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.MessageException

class BadChecksum(message: String) : MessageException(message)
