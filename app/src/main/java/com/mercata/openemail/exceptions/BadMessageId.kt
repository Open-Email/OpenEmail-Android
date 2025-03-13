package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.ParsingException

class BadMessageId(message: String): ParsingException(message)