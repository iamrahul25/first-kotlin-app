package com.nightwise.scrollguard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceStatus(
    val isServiceRunning: Boolean = false
)

object ServiceStateBus {
    private val _state = MutableStateFlow(ServiceStatus())
    val state: StateFlow<ServiceStatus> = _state.asStateFlow()

    fun update(transform: (ServiceStatus) -> ServiceStatus) {
        _state.value = transform(_state.value)
    }
}
