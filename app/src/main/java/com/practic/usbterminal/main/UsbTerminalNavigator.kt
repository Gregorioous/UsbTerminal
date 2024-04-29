package com.practic.usbterminal.main

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object UsbTerminalNavigator {

    @Suppress("ObjectPropertyName")
    private val _navTargetsSharedFlow = MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)
    val navTargetsSharedFlow = _navTargetsSharedFlow.asSharedFlow()

    interface NavTarget {
        val route: String
        val isTopInBackStack: Boolean
    }

    object NavTargetBack : NavTarget {
        override val route = "Back"
        override val isTopInBackStack = false
    }

    @Suppress("unused")
    fun navigateTo(navTarget: NavTarget) {
        _navTargetsSharedFlow.tryEmit(navTarget)
    }

    fun navigateBack() {
        _navTargetsSharedFlow.tryEmit(NavTargetBack)
    }
}