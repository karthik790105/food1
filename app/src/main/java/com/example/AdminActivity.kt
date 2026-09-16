package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.admin.AdminPortalApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdminPortalViewModel

/**
 * Dedicated launcher activity for BiteMart Central Admin Portal.
 * Gives the Android device a separate, dedicated "BiteMart Admin" launcher icon on the home screen.
 */
class AdminActivity : ComponentActivity() {
    private val adminViewModel: AdminPortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AdminPortalApp(
                    viewModel = adminViewModel
                )
            }
        }
    }
}
