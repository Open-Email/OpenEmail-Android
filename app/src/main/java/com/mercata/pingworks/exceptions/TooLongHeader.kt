package com.mercata.pingworks.exceptions

import com.mercata.pingworks.exceptions.abstract_exception.MessageException

class TooLongHeader(message: String) : MessageException(message)