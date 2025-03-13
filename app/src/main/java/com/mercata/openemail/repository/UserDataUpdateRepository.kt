package com.mercata.openemail.repository

import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserDataUpdateRepository(
    val sp: SharedPreferences
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val _userData = MutableStateFlow<PublicUserData?>(null)
    val userData: StateFlow<PublicUserData?> = _userData

    fun updateCurrentUserData() {
        coroutineScope.launch {
            when(val call = safeApiCall { getProfilePublicData(sp.getUserAddress()!!) }) {
                is HttpResult.Error -> {
                    //ignore
                }
                is HttpResult.Success -> {
                    _userData.update { call.data }
                }
            }
        }
    }
}