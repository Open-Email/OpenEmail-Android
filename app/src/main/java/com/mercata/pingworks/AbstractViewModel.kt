package com.mercata.pingworks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AbstractViewModel<T>(initialState: T) : ViewModel() {

    protected var currentState = initialState

    private val _state = MutableStateFlow(currentState)
    val state: StateFlow<T> = _state.asStateFlow()

    protected fun updateState(newState: T) {
        currentState = newState
        _state.value = currentState
    }
}