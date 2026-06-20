package com.mahchin.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahchin.app.ui.navigation.MahChinApp
import com.mahchin.app.ui.theme.MahChinTheme
import com.mahchin.app.ui.viewmodel.MainViewModel
import com.mahchin.app.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()

        val app = application as MahChinApplication
        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModelFactory(app, app.repository)
            )
            val settings by vm.settings.collectAsState()

            MahChinTheme(darkTheme = settings.darkMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MahChinApp(viewModel = vm)
                }
            }
        }
    }


    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
