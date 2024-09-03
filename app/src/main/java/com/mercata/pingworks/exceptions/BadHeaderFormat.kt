package com.mercata.pingworks.exceptions

import com.mercata.pingworks.exceptions.abstract_exception.ParsingException

class BadHeaderFormat(message: String) : ParsingException(message)