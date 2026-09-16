package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.delivery.DeliveryPartnerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DeliveryPartnerViewModel

/**
 * Dedicated standalone launcher activity for BiteMart Delivery Partner App.
 * Provides a direct, dedicated "BiteMart Delivery" launcher icon on the Android home screen.
 */
class DeliveryPartnerActivity : ComponentActivity() {
    private val deliveryViewModel: DeliveryPartnerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                DeliveryPartnerApp(
                    viewModel = deliveryViewModel
                )
            }
        }
    }
}
