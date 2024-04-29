package com.practic.usbterminal.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import com.practic.usbterminal.ui.theme.UsbTerminalTheme
import com.practic.usbterminal.ui.util.isDarkTheme
import com.practic.usbterminal.utill.collectAsStateLifecycleAware
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            application,
            intent
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            Timber.d("onCreate() +++++++ not root task ++++++")
        }

        lifecycleScope.launchWhenCreated {
            viewModel.shouldTerminateApp.collect {
                if (it) finish()
            }
        }

        viewModel.onMainActivityCreate()

        UsbPermissionRequester.bindActivity(
            this,
            viewModel.shouldRequestUsbPermissionFlow,
            viewModel::usbPermissionWasRequested
        )

        setContent {
            val settingsData =
                viewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()
            val isDarkTheme = isDarkTheme(settingsData.value)
            viewModel.setIsDarkTheme(isDarkTheme)
            UsbTerminalTheme(isDarkTheme) {
                MainAppScreen(viewModel, onBackPressedDispatcher)
            }
            val density = LocalDensity.current
            WindowInsets.Companion.ime.getBottom(density)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onActivityResume()
    }
    override fun onPause() {
        super.onPause()
        viewModel.onActivityPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        UsbPermissionRequester.unbindActivity()
    }
}