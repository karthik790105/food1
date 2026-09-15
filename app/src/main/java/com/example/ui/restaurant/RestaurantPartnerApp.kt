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
                            .padding(vertical = 2.dp)
                            .testTag("top_bar_store_info")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                            Text(
                                text = activeStore.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeStore.cuisines.firstOrNull() ?: activeStore.type.name} • ${activeStore.location}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
