package com.mercata.pingworks.exceptions

import com.mercata.pingworks.exceptions.abstract_exception.CryptoException

class SignatureMismatch(message: String) : CryptoException(message)