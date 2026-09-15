package com.example.ui.admin

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.AdminPortalViewModel

/**
 * Root Composable for the dedicated BiteMart Admin Portal App.
 * Manages authentication flow and root dashboard routing.
 */
@Composable
fun AdminPortalApp(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(
            targetState = currentAdmin != null,
            label = "admin_auth_transition"
        ) { isAuthenticated ->
            if (isAuthenticated) {
                AdminMainScreen(viewModel = viewModel)
            } else {
                AdminAuthScreen(viewModel = viewModel)
            }
        }
    }
}
