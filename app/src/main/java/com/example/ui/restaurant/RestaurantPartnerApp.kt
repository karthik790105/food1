package com.example.ui.restaurant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OrderStatus
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.PrimaryOrange
import com.example.viewmodel.RestaurantNav
import com.example.viewmodel.RestaurantPartnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantPartnerApp(
    viewModel: RestaurantPartnerViewModel,
    onSwitchToCustomerApp: () -> Unit = {},
    onSwitchToDeliveryApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOwner by viewModel.currentOwner.collectAsState()

    if (currentOwner == null) {
        RestaurantAuthScreen(
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    val currentNav by viewModel.currentNav.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    val isStoreOnline by viewModel.isStoreOnline.collectAsState()
    val orders by viewModel.storeOrders.collectAsState()
    val availableStores = viewModel.availableStores

    var showOutletSwitchDialog by remember { mutableStateOf(false) }
    var showSimulateOrderDialog by remember { mutableStateOf(false) }
    var customCustomerName by remember { mutableStateOf("Rahul Sharma") }
    var customPaymentMethod by remember { mutableStateOf("UPI") }

    val pendingOrdersCount = orders.count {
        it.status in listOf(OrderStatus.PLACED.name, OrderStatus.CONFIRMED.name, OrderStatus.PREPARING.name)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showOutletSwitchDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("top_bar_switch_outlet")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeStore.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch outlet",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Tap to Switch Restaurant",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Online / Offline Status Chip
                    Surface(
                        color = if (isStoreOnline) GroceryGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isStoreOnline) GroceryGreen else Color.Red),
                        modifier = Modifier
                            .clickable { viewModel.toggleStoreOnline() }
                            .testTag("top_toggle_online")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isStoreOnline) GroceryGreen else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isStoreOnline) "ONLINE" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStoreOnline) GroceryGreen else Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Simulate Customer Order Trigger
                    Surface(
                        color = PrimaryOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrimaryOrange),
                        modifier = Modifier
                            .clickable { showSimulateOrderDialog = true }
                            .testTag("top_btn_simulate_order")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Simulate Order",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+Order",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 1: Live Orders
                NavigationBarItem(
                    selected = currentNav == RestaurantNav.LIVE_ORDERS,
                    onClick = { viewModel.setNav(RestaurantNav.LIVE_ORDERS) },
                    icon = {
                        if (pendingOrdersCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = PrimaryOrange) {
                                        Text(pendingOrdersCount.toString())
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Live Orders")
                            }
                        } else {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Live Orders")
                        }
                    },
                    label = { Text("KDS Orders") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_kds_orders")
                )

                // Tab 2: Menu Catalog
                NavigationBarItem(
                    selected = currentNav == RestaurantNav.MENU_CATALOG,
                    onClick = { viewModel.setNav(RestaurantNav.MENU_CATALOG) },
                    icon = { Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Menu") },
                    label = { Text("Menu") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_menu_catalog")
                )

                // Tab 3: Insights & Analytics
                NavigationBarItem(
                    selected = currentNav == RestaurantNav.ANALYTICS,
                    onClick = { viewModel.setNav(RestaurantNav.ANALYTICS) },
                    icon = { Icon(imageVector = Icons.Default.Insights, contentDescription = "Insights") },
                    label = { Text("Insights") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_restaurant_analytics")
                )

                // Tab 4: Store Settings
                NavigationBarItem(
                    selected = currentNav == RestaurantNav.SETTINGS,
                    onClick = { viewModel.setNav(RestaurantNav.SETTINGS) },
                    icon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = "Outlet") },
                    label = { Text("Outlet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryOrange,
                        selectedTextColor = PrimaryOrange,
                        indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_restaurant_settings")
                )
            }
        }
    ) { innerPadding ->
        when (currentNav) {
            RestaurantNav.LIVE_ORDERS -> {
                RestaurantLiveOrdersScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            RestaurantNav.MENU_CATALOG -> {
                RestaurantMenuScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            RestaurantNav.ANALYTICS -> {
                RestaurantAnalyticsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            RestaurantNav.SETTINGS -> {
                RestaurantSettingsScreen(
                    viewModel = viewModel,
                    onSwitchToCustomerApp = onSwitchToCustomerApp,
                    onSwitchToDeliveryApp = onSwitchToDeliveryApp,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Switch Outlet Dialog accessible from TopBar across all tabs
    if (showOutletSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showOutletSwitchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Restaurant Outlet", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Choose an outlet to manage live orders, menu, and KDS pipeline:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    for (store in availableStores) {
                        val isSelected = store.id == activeStore.id
                        Surface(
                            color = if (isSelected) PrimaryOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectStore(store)
                                    showOutletSwitchDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryOrange else Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = store.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = "${store.location} • ${store.cuisines.take(2).joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        color = PrimaryOrange,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOutletSwitchDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Simulate Customer Order Dialog
    if (showSimulateOrderDialog) {
        AlertDialog(
            onDismissRequest = { showSimulateOrderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Customer Order", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Simulate an incoming order from a customer to test your kitchen display tickets and prep workflow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Quick Simulation Presets:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateIncomingCustomerOrder()
                                showSimulateOrderDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ 1-Tap Order", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.simulateRushHourOrders()
                                showSimulateOrderDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔥 Rush (3x)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or Customize Customer Details:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCustomerName,
                        onValueChange = { customCustomerName = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryOrange) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UPI", "Card", "Cash").forEach { method ->
                            FilterChip(
                                selected = customPaymentMethod == method,
                                onClick = { customPaymentMethod = method },
                                label = { Text(method, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryOrange
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customCustomerName.ifBlank { "Customer" }
                        viewModel.simulateIncomingCustomerOrder(
                            customerName = name,
                            paymentMethod = customPaymentMethod
                        )
                        showSimulateOrderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Simulate Order")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSimulateOrderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
