package com.you.reelblocker.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceStatus(
    val isServiceRunning: Boolean = false,
    val isAccessibilityPermissionGranted: Boolean = false,
    val blockedToday: Int = 0,
    val blockedTotal: Int = 0
)

object ServiceStateBus {
    private val _state = MutableStateFlow(ServiceStatus())
    val state: StateFlow<ServiceStatus> = _state.asStateFlow()

    fun update(transform: (ServiceStatus) -> ServiceStatus) {
        _state.value = transform(_state.value)
    }
}
