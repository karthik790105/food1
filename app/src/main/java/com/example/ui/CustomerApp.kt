package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.OrderStatus
import com.example.ui.components.ActiveOrderFloatingPill
import com.example.ui.components.AddressPickerSheet
import com.example.ui.components.NetworkEcosystemSheet
import com.example.ui.components.TopLocationHeader
import com.example.ui.components.UserProfileSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.LiveOrderTrackingScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StoreDetailScreen
import com.example.ui.theme.PrimaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CustomerDeliveryViewModel

@Composable
fun CustomerApp(
    viewModel: CustomerDeliveryViewModel,
    modifier: Modifier = Modifier
) {
    val activeUser by viewModel.activeUser.collectAsState()

    // If customer is not authenticated, display the Sign Up and Login flow first
    if (activeUser == null) {
        AuthScreen(
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val addresses by viewModel.userAddresses.collectAsState()
    val selectedAddressId by viewModel.selectedAddressId.collectAsState()
    val currentAddress = addresses.find { it.id == selectedAddressId } ?: addresses.firstOrNull()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val cloudStatus by viewModel.cloudStatus.collectAsState()

    var showAddressSheet by remember { mutableStateOf(false) }
    var showNetworkSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Handle system back navigation gracefully
    BackHandler(enabled = currentScreen != AppScreen.EXPLORE) {
        when (currentScreen) {
            AppScreen.STORE_DETAIL, AppScreen.SEARCH, AppScreen.CART, AppScreen.ORDERS, AppScreen.LIVE_TRACKING -> {
                viewModel.navigateTo(AppScreen.EXPLORE)
            }
            else -> viewModel.navigateTo(AppScreen.EXPLORE)
        }
    }

    val isOrderActive = activeOrder != null &&
            activeOrder?.status != OrderStatus.DELIVERED.name &&
            activeOrder?.status != OrderStatus.CANCELLED.name

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (currentScreen == AppScreen.EXPLORE) {
                TopLocationHeader(
                    selectedAddress = currentAddress,
                    userName = activeUser?.name ?: "Customer",
                    onAddressClick = { showAddressSheet = true },
                    onProfileClick = { showProfileSheet = true },
                    onNetworkClick = { showNetworkSheet = true }
                )
            }
        },
        bottomBar = {
            Column {
                // Floating Live Tracking banner if there is an active order and user is not on the live tracking screen
                AnimatedVisibility(
                    visible = isOrderActive && currentScreen != AppScreen.LIVE_TRACKING,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    activeOrder?.let { order ->
                        ActiveOrderFloatingPill(
                            order = order,
                            onClick = { viewModel.openLiveTracking(order.orderId) }
                        )
                    }
                }

                // Main Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("customer_bottom_nav")
                ) {
                    // Explore
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.EXPLORE,
                        onClick = { viewModel.navigateTo(AppScreen.EXPLORE) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.EXPLORE) Icons.Filled.Fastfood else Icons.Outlined.Fastfood,
                                contentDescription = "Explore",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Explore") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_explore")
                    )

                    // Search
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SEARCH,
                        onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )

                    // Cart
                    val totalCartCount = cartItems.sumOf { it.quantity }
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CART,
                        onClick = { viewModel.navigateTo(AppScreen.CART) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (totalCartCount > 0) {
                                        Badge(containerColor = PrimaryOrange) {
                                            Text(totalCartCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.CART) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                    contentDescription = "Cart",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = { Text("Cart") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_cart")
                    )

                    // Orders
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ORDERS || currentScreen == AppScreen.LIVE_TRACKING,
                        onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "Orders",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Orders") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_orders")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                AppScreen.EXPLORE -> {
                    ExploreScreen(viewModel = viewModel)
                }
                AppScreen.SEARCH -> {
                    SearchScreen(viewModel = viewModel)
                }
                AppScreen.STORE_DETAIL -> {
                    StoreDetailScreen(viewModel = viewModel)
                }
                AppScreen.CART -> {
                    CartCheckoutScreen(
                        viewModel = viewModel,
                        onOpenAddressPicker = { showAddressSheet = true }
                    )
                }
                AppScreen.LIVE_TRACKING -> {
                    LiveOrderTrackingScreen(viewModel = viewModel)
                }
                AppScreen.ORDERS -> {
                    OrdersHistoryScreen(viewModel = viewModel)
                }
                AppScreen.NETWORK_ECOSYSTEM -> {
                    ExploreScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal Bottom Sheets
    if (showAddressSheet) {
        AddressPickerSheet(
            viewModel = viewModel,
            onDismiss = { showAddressSheet = false }
        )
    }

    if (showNetworkSheet) {
        NetworkEcosystemSheet(
            onDismiss = { showNetworkSheet = false },
            isCloudConnected = isCloudConnected,
            cloudStatus = cloudStatus
        )
    }

    if (showProfileSheet) {
        UserProfileSheet(
            user = activeUser,
            currentAddress = currentAddress,
            onOpenAddressPicker = {
                showProfileSheet = false
                showAddressSheet = true
            },
            onSelectLocation = { title, fullAddress, landmark ->
                viewModel.addNewAddress(title, fullAddress, landmark)
            },
            onLogout = { viewModel.logout() },
            onDismiss = { showProfileSheet = false }
        )
    }
}
