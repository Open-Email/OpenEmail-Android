package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.MessageException

class TooLongHeader(message: String) : MessageException(message)