package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.ui.delivery.DeliveryPartnerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SmsNotificationHelper
import com.example.viewmodel.DeliveryPartnerViewModel

/**
 * Main entry point for BiteMart Delivery Partner.
 * Launches the Delivery Partner App with registration, login, order pickup,
 * customer order details, physical fiscal bill printing for restaurants, and OTP verification flow.
 */
class MainActivity : ComponentActivity() {
    private val deliveryViewModel: DeliveryPartnerViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel for incoming alerts
        SmsNotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyApplicationTheme {
                DeliveryPartnerApp(
                    viewModel = deliveryViewModel
                )
            }
        }
    }
}



