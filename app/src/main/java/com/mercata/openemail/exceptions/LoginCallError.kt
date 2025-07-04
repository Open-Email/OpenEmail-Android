package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.LoginException

class LoginCallError(message: String): LoginException(message)