package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DeliveryPartner
import com.example.ui.delivery.FiscalBillPrintDialog
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.PrimaryOrangeDark
import com.example.viewmodel.AdminNavTab
import com.example.viewmodel.AdminPortalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val currentNavTab by viewModel.currentNavTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val selectedOrderForPrint by viewModel.selectedOrderForPrint.collectAsStateWithLifecycle()
    val allDeliveryPartners by viewModel.allDeliveryPartners.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val admin = currentAdmin ?: return

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = PrimaryOrange,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BiteMart Admin Portal",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${admin.name} • ${admin.role} (${admin.department})",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Logout Button
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.testTag("admin_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                    }

                    // Real-time Action Notification Banner
                    AnimatedVisibility(visible = userMessage != null) {
                        userMessage?.let { msg ->
                            Surface(
                                color = Color.Black.copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearUserMessage() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentNavTab == AdminNavTab.DASHBOARD,
                    onClick = { viewModel.setNavTab(AdminNavTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("admin_nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentNavTab == AdminNavTab.ORDERS,
                    onClick = { viewModel.setNavTab(AdminNavTab.ORDERS) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders") },
                    label = { Text("Orders & OTP", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("admin_nav_orders")
                )
                NavigationBarItem(
                    selected = currentNavTab == AdminNavTab.RESTAURANTS,
                    onClick = { viewModel.setNavTab(AdminNavTab.RESTAURANTS) },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = "Restaurants") },
                    label = { Text("Stores & Menu", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("admin_nav_restaurants")
                )
                NavigationBarItem(
                    selected = currentNavTab == AdminNavTab.RIDERS,
                    onClick = { viewModel.setNavTab(AdminNavTab.RIDERS) },
                    icon = { Icon(Icons.Default.DeliveryDining, contentDescription = "Riders") },
                    label = { Text("Riders", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("admin_nav_riders")
                )
                NavigationBarItem(
                    selected = currentNavTab == AdminNavTab.CUSTOMERS,
                    onClick = { viewModel.setNavTab(AdminNavTab.CUSTOMERS) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text("Customers", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("admin_nav_customers")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentNavTab) {
                AdminNavTab.DASHBOARD -> AdminDashboardTab(viewModel = viewModel, admin = admin)
                AdminNavTab.ORDERS -> AdminOrdersTab(viewModel = viewModel)
                AdminNavTab.RESTAURANTS -> AdminRestaurantsTab(viewModel = viewModel)
                AdminNavTab.RIDERS -> AdminRidersTab(viewModel = viewModel)
                AdminNavTab.CUSTOMERS -> AdminCustomersTab(viewModel = viewModel)
            }
        }
    }

    // Fiscal Bill Print Dialog integration
    selectedOrderForPrint?.let { order ->
        val matchedPartner = allDeliveryPartners.find {
            it.phone == order.partnerPhone || it.name == order.partnerName
        } ?: if (!order.partnerName.isNullOrBlank()) {
            DeliveryPartner(
                id = "admin-rider-ref",
                name = order.partnerName ?: "Assigned Partner",
                phone = order.partnerPhone ?: "9876543210",
                vehicle = order.partnerVehicle ?: "Motorcycle"
            )
        } else null

        FiscalBillPrintDialog(
            order = order,
            partner = matchedPartner,
            onDismiss = { viewModel.selectOrderForPrint(null) },
            onBillHandedOver = { viewModel.selectOrderForPrint(null) }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.Default.Logout, contentDescription = null, tint = PrimaryOrange)
            },
            title = { Text("Sign Out of Admin Portal?") },
            text = { Text("You will need to sign in again to access central operations.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Stay Logged In")
                }
            }
        )
    }
}
