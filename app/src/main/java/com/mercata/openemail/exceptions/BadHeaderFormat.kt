package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.ParsingException

class BadHeaderFormat(message: String) : ParsingException(message)