package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.restaurant.RestaurantPartnerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RestaurantPartnerViewModel

/**
 * Dedicated standalone launcher activity for BiteMart Restaurant Partner App.
 * Provides a direct, dedicated "BiteMart Restaurant" launcher icon on the Android home screen.
 */
class RestaurantPartnerActivity : ComponentActivity() {
    private val restaurantViewModel: RestaurantPartnerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                RestaurantPartnerApp(
                    viewModel = restaurantViewModel
                )
            }
        }
    }
}
