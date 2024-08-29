package com.mercata.pingworks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.toPerson
import com.mercata.pingworks.models.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class AbstractViewModel<T>(initialState: T) : ViewModel(), KoinComponent {

    protected var currentState: T = initialState

    protected val sharedPreferences: SharedPreferences by inject()
    protected val db: AppDatabase by inject()

    private val _state = MutableStateFlow(currentState)
    val state: StateFlow<T> = _state.asStateFlow()

    protected fun updateState(newState: T) {
        currentState = newState
        _state.value = currentState
    }
}