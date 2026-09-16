package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.CustomerApp
import com.example.ui.admin.AdminPortalApp
import com.example.ui.delivery.DeliveryPartnerApp
import com.example.ui.restaurant.RestaurantPartnerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SmsNotificationHelper
import com.example.viewmodel.AdminPortalViewModel
import com.example.viewmodel.CustomerDeliveryViewModel
import com.example.viewmodel.DeliveryPartnerViewModel
import com.example.viewmodel.RestaurantPartnerViewModel

enum class BiteMartAppMode {
    ADMIN,
    DELIVERY,
    RESTAURANT,
    CUSTOMER
}

/**
 * Main entry point for BiteMart Ecosystem.
 * Allows instant navigation between:
 * 1. 🛡️ Central Operations & Admin Portal
 * 2. 🛵 Delivery Partner App
 * 3. 👨‍🍳 Restaurant Partner App
 * 4. 🛒 Customer App
 */
class MainActivity : ComponentActivity() {
    private val adminViewModel: AdminPortalViewModel by viewModels()
    private val deliveryViewModel: DeliveryPartnerViewModel by viewModels()
    private val restaurantViewModel: RestaurantPartnerViewModel by viewModels()
    private val customerViewModel: CustomerDeliveryViewModel by viewModels()

    companion object {
        const val EXTRA_APP_MODE = "EXTRA_APP_MODE"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialMode = intent.getStringExtra(EXTRA_APP_MODE)?.let { modeStr ->
            runCatching { BiteMartAppMode.valueOf(modeStr) }.getOrNull()
        } ?: BiteMartAppMode.CUSTOMER

        // Create notification channel for incoming alerts
        SmsNotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyApplicationTheme {
                var currentMode by remember { mutableStateOf(initialMode) }

                when (currentMode) {
                    BiteMartAppMode.ADMIN -> {
                        AdminPortalApp(viewModel = adminViewModel)
                    }

                    BiteMartAppMode.DELIVERY -> {
                        DeliveryPartnerApp(viewModel = deliveryViewModel)
                    }

                    BiteMartAppMode.RESTAURANT -> {
                        RestaurantPartnerApp(viewModel = restaurantViewModel)
                    }

                    BiteMartAppMode.CUSTOMER -> {
                        CustomerApp(viewModel = customerViewModel)
                    }
                }
            }
        }
    }
}



