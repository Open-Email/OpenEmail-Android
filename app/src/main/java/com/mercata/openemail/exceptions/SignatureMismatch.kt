package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.CryptoException

class SignatureMismatch(message: String) : CryptoException(message)