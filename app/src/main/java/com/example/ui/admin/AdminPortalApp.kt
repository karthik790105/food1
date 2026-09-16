package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.entity.OrderEntity
import com.example.util.ApkExportHelper
import com.example.data.entity.UserEntity
import com.example.model.AdminUser
import com.example.model.DeliveryPartner
import com.example.model.MenuItem
import com.example.model.Store
import com.example.ui.theme.GroceryGreenDark
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen
import com.example.viewmodel.AdminNavTab
import com.example.viewmodel.AdminPortalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Central Admin Portal for BiteMart.
 * Follows the signature orange theme container with unified management over:
 * 1. Platform KPI Overview & GMV metrics
 * 2. Live & Historic Orders with Delivery Confirmation OTP cross-checking
 * 3. Restaurant Partner Suspension & Menu/Item pricing controls
 * 4. Delivery Rider Suspension & Fleet control
 * 5. Registered Customer profiles & lifecycle tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalApp(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val currentNavTab by viewModel.currentNavTab.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val selectedStoreForMenu by viewModel.selectedStoreForMenu.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showApkDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Gate with Admin Authentication (Login & Register)
    if (currentAdmin == null) {
        AdminAuthScreen(
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    val admin = currentAdmin!!

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Badge",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BiteMart Admin",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = PrimaryOrange.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = admin.role.uppercase(),
                                        color = PrimaryOrange,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${admin.name} • ${admin.department}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    // APK Download & Export Quick Button
                    IconButton(
                        onClick = { showApkDownloadDialog = true },
                        modifier = Modifier.testTag("admin_download_apk_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Admin APK",
                            tint = PrimaryOrange
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout Admin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (selectedStoreForMenu == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val allOrders by viewModel.allOrders.collectAsState()
                    val activeOrdersCount = allOrders.count {
                        it.status !in listOf("DELIVERED", "CANCELLED")
                    }

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 11.sp) },
                        selected = currentNavTab == AdminNavTab.DASHBOARD,
                        onClick = { viewModel.setNavTab(AdminNavTab.DASHBOARD) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_admin_dashboard")
                    )

                    NavigationBarItem(
                        icon = {
                            if (activeOrdersCount > 0) {
                                BadgedBox(badge = { Badge(containerColor = PrimaryOrange) { Text("$activeOrdersCount") } }) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = "Orders")
                                }
                            } else {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Orders")
                            }
                        },
                        label = { Text("Orders", fontSize = 11.sp) },
                        selected = currentNavTab == AdminNavTab.ORDERS,
                        onClick = { viewModel.setNavTab(AdminNavTab.ORDERS) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_admin_orders")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Outlets") },
                        label = { Text("Outlets", fontSize = 11.sp) },
                        selected = currentNavTab == AdminNavTab.RESTAURANTS,
                        onClick = { viewModel.setNavTab(AdminNavTab.RESTAURANTS) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_admin_restaurants")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.TwoWheeler, contentDescription = "Riders") },
                        label = { Text("Riders", fontSize = 11.sp) },
                        selected = currentNavTab == AdminNavTab.RIDERS,
                        onClick = { viewModel.setNavTab(AdminNavTab.RIDERS) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_admin_riders")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                        label = { Text("Customers", fontSize = 11.sp) },
                        selected = currentNavTab == AdminNavTab.CUSTOMERS,
                        onClick = { viewModel.setNavTab(AdminNavTab.CUSTOMERS) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_admin_customers")
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedStoreForMenu != null) {
                // In-depth Menu Catalog Editor for Selected Restaurant
                AdminStoreMenuEditor(
                    store = selectedStoreForMenu!!,
                    viewModel = viewModel,
                    onBack = { viewModel.selectStoreForMenu(null) }
                )
            } else {
                when (currentNavTab) {
                    AdminNavTab.DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                    AdminNavTab.ORDERS -> AdminOrdersScreen(viewModel = viewModel)
                    AdminNavTab.RESTAURANTS -> AdminRestaurantsScreen(viewModel = viewModel)
                    AdminNavTab.RIDERS -> AdminRidersScreen(viewModel = viewModel)
                    AdminNavTab.CUSTOMERS -> AdminCustomersScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Admin Session") },
            text = { Text("Are you sure you want to end your administrative session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Confirm Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // APK Download & Build Information Dialog
    if (showApkDownloadDialog) {
        AdminApkDownloadDialog(
            onDismiss = { showApkDownloadDialog = false }
        )
    }
}

/**
 * 1. Admin Dashboard Screen
 * Overview cards, Key Metrics, GMV, Fleet & Outlet operational states
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val allStores by viewModel.allStores.collectAsState()
    val suspendedStores by viewModel.suspendedStores.collectAsState()
    val allRiders by viewModel.allDeliveryPartners.collectAsState()
    val suspendedRiders by viewModel.suspendedRiders.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val totalOrders = allOrders.size
    val activeOrders = allOrders.count { it.status !in listOf("DELIVERED", "CANCELLED") }
    val completedOrders = allOrders.count { it.status == "DELIVERED" }
    val cancelledOrders = allOrders.count { it.status == "CANCELLED" }
    val totalRevenue = allOrders.filter { it.status == "DELIVERED" }.sumOf { it.total }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Orange Container Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "CENTRAL OPERATIONS CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Platform Health: NORMAL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Delivered GMV",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "₹${totalRevenue.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Active Live Orders",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "$activeOrders",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4 KPI Metric Cards in Grid
        Text(
            text = "Operational Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiMetricCard(
                title = "Total Orders",
                value = "$totalOrders",
                subtitle = "$completedOrders delivered • $cancelledOrders cancelled",
                icon = Icons.Default.ReceiptLong,
                color = PrimaryOrange,
                modifier = Modifier.weight(1f)
            )

            KpiMetricCard(
                title = "Restaurants",
                value = "${allStores.size}",
                subtitle = "${suspendedStores.size} suspended",
                icon = Icons.Default.Storefront,
                color = GroceryGreenDark,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiMetricCard(
                title = "Delivery Fleet",
                value = "${allRiders.size}",
                subtitle = "${suspendedRiders.size} suspended",
                icon = Icons.Default.TwoWheeler,
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )

            KpiMetricCard(
                title = "Customers",
                value = "${allUsers.size}",
                subtitle = "Registered accounts",
                icon = Icons.Default.People,
                color = Color(0xFF7B1FA2),
                modifier = Modifier.weight(1f)
            )
        }

        // Live Alerts / Fast Action Section
        Text(
            text = "Quick Operations Jump",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminQuickActionRow(
                    icon = Icons.Default.Key,
                    title = "Delivery Confirmation OTPs",
                    subtitle = "Verify customer delivery OTPs for dispute resolution",
                    btnText = "View Orders",
                    onClick = { viewModel.setNavTab(AdminNavTab.ORDERS) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                AdminQuickActionRow(
                    icon = Icons.Default.Warning,
                    title = "Restaurant Suspensions & Menus",
                    subtitle = "Toggle outlet suspension or edit prices and stock",
                    btnText = "Manage Outlets",
                    onClick = { viewModel.setNavTab(AdminNavTab.RESTAURANTS) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                AdminQuickActionRow(
                    icon = Icons.Default.TwoWheeler,
                    title = "Rider Fleet Suspensions",
                    subtitle = "Manage active delivery personnel and disciplinary actions",
                    btnText = "Manage Riders",
                    onClick = { viewModel.setNavTab(AdminNavTab.RIDERS) }
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdminQuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    btnText: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(btnText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 2. Orders Management Tab
 * Live & past orders with Delivery Confirmation OTP, customer phone, rider phone,
 * items list, status switcher, cancel order button.
 */
@Composable
fun AdminOrdersScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val orderStatusFilter by viewModel.orderStatusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var orderToCancel by remember { mutableStateOf<OrderEntity?>(null) }
    var orderForStatusChange by remember { mutableStateOf<OrderEntity?>(null) }

    // Filter list
    val filteredOrders = remember(allOrders, orderStatusFilter, searchQuery) {
        allOrders.filter { order ->
            val matchesFilter = when (orderStatusFilter) {
                "ALL" -> true
                "LIVE" -> order.status !in listOf("DELIVERED", "CANCELLED")
                else -> order.status.equals(orderStatusFilter, ignoreCase = true)
            }

            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                    order.orderId.lowercase().contains(q) ||
                    order.storeName.lowercase().contains(q) ||
                    order.customerName.lowercase().contains(q) ||
                    order.customerPhone.contains(q) ||
                    (order.partnerName?.lowercase()?.contains(q) == true) ||
                    order.otp.contains(q)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by Order ID, OTP, Customer, Store, Rider...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_orders_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            val filterChips = listOf("ALL", "LIVE", "PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState(), enabled = false)
            ) {
                filterChips.take(4).forEach { filter ->
                    FilterChip(
                        selected = orderStatusFilter == filter,
                        onClick = { viewModel.setOrderStatusFilter(filter) },
                        label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryOrange
                        )
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filterChips.drop(4).forEach { filter ->
                    FilterChip(
                        selected = orderStatusFilter == filter,
                        onClick = { viewModel.setOrderStatusFilter(filter) },
                        label = { Text(filter.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryOrange
                        )
                    )
                }
            }
        }

        // Orders List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No orders match this criteria",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOrders, key = { it.orderId }) { order ->
                    AdminOrderCard(
                        order = order,
                        onCancel = { orderToCancel = order },
                        onChangeStatus = { orderForStatusChange = order }
                    )
                }
            }
        }
    }

    // Cancel Order Dialog
    if (orderToCancel != null) {
        val o = orderToCancel!!
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel Order #${o.orderId.take(8)}") },
            text = {
                Text("Are you sure you want to CANCEL this order for customer '${o.customerName}' at '${o.storeName}'? This action cannot be reversed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(o.orderId)
                        orderToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Change Status Dialog
    if (orderForStatusChange != null) {
        val o = orderForStatusChange!!
        val statuses = listOf("PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")

        AlertDialog(
            onDismissRequest = { orderForStatusChange = null },
            title = { Text("Update Status: #${o.orderId.take(8)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new operational status:", style = MaterialTheme.typography.bodySmall)
                    statuses.forEach { st ->
                        OutlinedButton(
                            onClick = {
                                viewModel.updateOrderStatus(o.orderId, st)
                                orderForStatusChange = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(st.replace("_", " "), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { orderForStatusChange = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Individual Order Card with Delivery Confirmation OTP prominently highlighted
 */
@Composable
fun AdminOrderCard(
    order: OrderEntity,
    onCancel: () -> Unit,
    onChangeStatus: () -> Unit
) {
    val dateStr = remember(order.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    }

    val statusColor = when (order.status) {
        "DELIVERED" -> VegGreen
        "CANCELLED" -> Color(0xFFD32F2F)
        "OUT_FOR_DELIVERY" -> PrimaryOrange
        "PREPARING" -> Color(0xFF0288D1)
        else -> Color(0xFFF57C00)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Store Name & Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: #${order.orderId} • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = order.status.replace("_", " "),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==================== DELIVERY CONFIRMATION OTP BANNER ====================
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Delivery Confirmation OTP",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "DELIVERY CONFIRMATION OTP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Customer verbal code for delivery partner",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Bold OTP Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryOrange,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = order.otp.ifBlank { "N/A" },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer & Rider Details
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Customer Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CUSTOMER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = order.customerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "📞 ${order.customerPhone}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delivery Rider Column
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DELIVERY RIDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = order.partnerName ?: "Not Assigned",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (order.partnerName != null) VegGreen else Color.Gray
                    )
                    if (order.partnerPhone != null) {
                        Text(
                            text = "📞 ${order.partnerPhone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Order Items & Total
            Text(
                text = "Items: ${order.itemsSummary}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total: ₹${order.total.toInt()} (${order.paymentMethod})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Update Status Button
                    OutlinedButton(
                        onClick = onChangeStatus,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Status", fontSize = 11.sp)
                    }

                    // Cancel Order Button (if not already cancelled/delivered)
                    if (order.status !in listOf("DELIVERED", "CANCELLED")) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Restaurants / Outlets Tab
 * Suspend/Reactivate outlet, and manage outlet menu/catalog items & pricing
 */
@Composable
fun AdminRestaurantsScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val allStores by viewModel.allStores.collectAsState()
    val suspendedStores by viewModel.suspendedStores.collectAsState()
    var restaurantSearch by remember { mutableStateOf("") }

    val filteredStores = remember(allStores, restaurantSearch) {
        allStores.filter {
            val q = restaurantSearch.trim().lowercase()
            q.isEmpty() || it.name.lowercase().contains(q) || it.cuisines.any { c -> c.lowercase().contains(q) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = restaurantSearch,
                onValueChange = { restaurantSearch = it },
                placeholder = { Text("Search restaurant or store name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                trailingIcon = {
                    if (restaurantSearch.isNotEmpty()) {
                        IconButton(onClick = { restaurantSearch = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_restaurant_search")
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStores, key = { it.id }) { store ->
                val isSuspended = suspendedStores.contains(store.id)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSuspended) Color(0xFFD32F2F).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${store.cuisines.joinToString(", ")} • ${store.type.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "📍 ${store.location} • Rating: ${store.rating} ★",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSuspended) Color(0xFFD32F2F).copy(alpha = 0.12f) else VegGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isSuspended) "SUSPENDED" else "ACTIVE",
                                    color = if (isSuspended) Color(0xFFD32F2F) else VegGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Manage Menu Catalog Button
                            OutlinedButton(
                                onClick = { viewModel.selectStoreForMenu(store) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage Menu / Prices", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Suspend / Reactivate Outlet Toggle
                            Button(
                                onClick = { viewModel.toggleStoreSuspension(store.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSuspended) VegGreen else Color(0xFFD32F2F)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSuspended) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSuspended) "Reactivate" else "Suspend",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menu Editor Sheet for Selected Restaurant
 * Admin can add dishes, change prices, toggle in/out of stock, delete items.
 */
@Composable
fun AdminStoreMenuEditor(
    store: Store,
    viewModel: AdminPortalViewModel,
    onBack: () -> Unit
) {
    val menuItems by viewModel.menuItemsForSelectedStore.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemForPriceEdit by remember { mutableStateOf<MenuItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MenuItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Back Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Stores")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Menu Management • ${menuItems.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Button(
                    onClick = { showAddItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (menuItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No menu items found for this store. Click 'Add Item' to insert dishes.",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Veg / Non-Veg Indicator
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, if (item.isVeg) VegGreen else Color(0xFFD32F2F)),
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (item.isVeg) VegGreen else Color(0xFFD32F2F),
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${item.price.toInt()} • ${item.category} • ${item.unit}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryOrange
                                )
                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // In Stock / Out of Stock Switch
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (item.inStock) "In Stock" else "Sold Out",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.inStock) VegGreen else Color.Gray
                                    )
                                    Switch(
                                        checked = item.inStock,
                                        onCheckedChange = { viewModel.toggleItemStock(item.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = VegGreen,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color.Gray
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Edit Price Button
                                IconButton(onClick = { itemForPriceEdit = item }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = PrimaryOrange)
                                }

                                // Delete Item Button
                                IconButton(onClick = { itemToDelete = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Price Dialog
    if (itemForPriceEdit != null) {
        val itm = itemForPriceEdit!!
        var newPriceStr by remember { mutableStateOf(itm.price.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { itemForPriceEdit = null },
            title = { Text("Update Price: ${itm.name}") },
            text = {
                Column {
                    Text("Enter new price in ₹:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPriceStr,
                        onValueChange = { if (it.length <= 6) newPriceStr = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = PrimaryOrange) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newPriceStr.toDoubleOrNull() ?: itm.price
                        viewModel.updateItemPrice(itm.id, parsed)
                        itemForPriceEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Save Price")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForPriceEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        var addName by remember { mutableStateOf("") }
        var addDesc by remember { mutableStateOf("") }
        var addPrice by remember { mutableStateOf("") }
        var addIsVeg by remember { mutableStateOf(true) }
        var addCategory by remember { mutableStateOf("Mains") }
        var addUnit by remember { mutableStateOf("1 serving") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item to ${store.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addDesc,
                        onValueChange = { addDesc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addPrice,
                        onValueChange = { addPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addCategory,
                        onValueChange = { addCategory = it },
                        label = { Text("Category (e.g. Starters, Biryani, Drinks)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vegetarian Item?", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = addIsVeg,
                            onCheckedChange = { addIsVeg = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VegGreen
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = addPrice.toDoubleOrNull() ?: 0.0
                        val added = viewModel.addMenuItem(
                            storeId = store.id,
                            name = addName,
                            description = addDesc,
                            price = p,
                            isVeg = addIsVeg,
                            category = addCategory,
                            unit = addUnit
                        )
                        if (added) {
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (itemToDelete != null) {
        val itm = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to permanently remove '${itm.name}' from the menu?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMenuItem(itm.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 4. Riders Fleet Tab
 * View registered delivery personnel, duty status, rating, and toggle suspension.
 */
@Composable
fun AdminRidersScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val riders by viewModel.allDeliveryPartners.collectAsState()
    val suspendedRiders by viewModel.suspendedRiders.collectAsState()
    var riderSearch by remember { mutableStateOf("") }

    val filteredRiders = remember(riders, riderSearch) {
        riders.filter {
            val q = riderSearch.trim().lowercase()
            q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) || it.vehicleNumber.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = riderSearch,
                onValueChange = { riderSearch = it },
                placeholder = { Text("Search rider by name, mobile, vehicle number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                trailingIcon = {
                    if (riderSearch.isNotEmpty()) {
                        IconButton(onClick = { riderSearch = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rider_search")
            )
        }

        if (filteredRiders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (riders.isEmpty()) "No registered delivery partners yet.\nRiders registered via Delivery Partner App will appear here." else "No riders match search.",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredRiders, key = { it.id }) { rider ->
                    val isSuspended = suspendedRiders.contains(rider.id)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSuspended) Color(0xFFD32F2F).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSuspended) Color(0xFFD32F2F).copy(alpha = 0.15f) else PrimaryOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (isSuspended) Color(0xFFD32F2F) else PrimaryOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = rider.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "📞 ${rider.phone} • ${rider.vehicle} (${rider.vehicleNumber})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSuspended) Color(0xFFD32F2F).copy(alpha = 0.12f) else VegGreen.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (isSuspended) "SUSPENDED" else (if (rider.isOnline) "ON DUTY" else "OFF DUTY"),
                                        color = if (isSuspended) Color(0xFFD32F2F) else (if (rider.isOnline) VegGreen else Color.Gray),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⭐ Rating: ${rider.rating} • ${rider.totalDeliveries} completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { viewModel.toggleRiderSuspension(rider.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSuspended) VegGreen else Color(0xFFD32F2F)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSuspended) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSuspended) "Restore Rider" else "Suspend Rider",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Customers / Users Directory Tab
 * View all registered customer profiles from Room DB.
 */
@Composable
fun AdminCustomersScreen(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    var customerSearch by remember { mutableStateOf("") }

    val filteredUsers = remember(users, customerSearch) {
        users.filter {
            val q = customerSearch.trim().lowercase()
            q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) || (it.email?.lowercase()?.contains(q) == true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = customerSearch,
                onValueChange = { customerSearch = it },
                placeholder = { Text("Search customer by name, mobile, email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                trailingIcon = {
                    if (customerSearch.isNotEmpty()) {
                        IconButton(onClick = { customerSearch = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customer_search")
            )
        }

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (users.isEmpty()) "No customers registered yet.\nUsers registered in Customer App will appear here." else "No customers match search.",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredUsers, key = { it.phone }) { user ->
                    val userOrders = allOrders.filter { it.customerPhone == user.phone }
                    val regDate = remember(user.createdAt) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.createdAt))
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryOrange.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "📞 ${user.phone} • ✉️ ${user.email?.ifBlank { "N/A" } ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Joined: $regDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryOrange.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${userOrders.size} Orders",
                                        color = PrimaryOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Dialog showing Admin & Ecosystem APK build details, direct file sharing,
 * project file paths, and download instructions.
 */
@Composable
fun AdminApkDownloadDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "BiteMart Admin APK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Direct Download & Build Info",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Build Status: Ready & Compiled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "app-debug.apk (Android Package)",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // 4 Decoupled Standalone Launchers info
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "📱 4 Decoupled Standalone Home Launchers:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "1. 🛡️ BiteMart Admin",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = PrimaryOrange
                            )
                            Text(
                                text = " (Central Operations Portal)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "2. 🛵 BiteMart Delivery",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = " (Rider Partner App)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "3. 👨‍🍳 BiteMart Restaurant",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                            Text(
                                text = " (Restaurant Partner App)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "4. 🛒 BiteMart Customer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = " (Customer Food & Grocery App)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Workspace File Location
                Column {
                    Text(
                        text = "File Path in Project Code:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ApkExportHelper.PROJECT_APK_PATH,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(ApkExportHelper.PROJECT_APK_PATH))
                                    Toast.makeText(context, "Copied path to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Path",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Instructions to download directly in Google AI Studio
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 How to Download the APK File:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryOrange
                        )
                        Text(
                            text = "• AI Studio UI: Click the Settings / Three Dots menu at top-right → select 'Download APK' or 'Export Project ZIP'.\n• Direct Sharing: Tap 'Share / Export APK' below to send the APK file via WhatsApp, Drive, or Save to Device.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ApkExportHelper.shareAppApk(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share / Export APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

