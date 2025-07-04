package com.mercata.openemail.exceptions

import com.mercata.openemail.exceptions.abstract_exception.LoginException

class NoUserSaved(message: String): LoginException(message)