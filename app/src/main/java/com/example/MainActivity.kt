package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.ui.admin.AdminPortalApp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SmsNotificationHelper
import com.example.viewmodel.AdminPortalViewModel

/**
 * Main entry point for BiteMart.
 * Directly launches the Central Operations Admin Portal without any app switching overlays.
 */
class MainActivity : ComponentActivity() {
    private val adminViewModel: AdminPortalViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SmsNotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyApplicationTheme {
                AdminPortalApp(
                    viewModel = adminViewModel
                )
            }
        }
    }
}
