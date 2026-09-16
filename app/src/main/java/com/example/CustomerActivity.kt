package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.CustomerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CustomerDeliveryViewModel

/**
 * Dedicated standalone launcher activity for BiteMart Customer App.
 * Provides a direct, dedicated "BiteMart Customer" launcher icon on the Android home screen.
 */
class CustomerActivity : ComponentActivity() {
    private val customerViewModel: CustomerDeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CustomerApp(
                    viewModel = customerViewModel
                )
            }
        }
    }
}
