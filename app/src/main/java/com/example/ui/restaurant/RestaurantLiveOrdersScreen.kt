package com.example.ui.restaurant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.entity.OrderEntity
import com.example.model.OrderStatus
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen
import com.example.viewmodel.RestaurantOrderTab
import com.example.viewmodel.RestaurantPartnerViewModel

@Composable
fun RestaurantLiveOrdersScreen(
    viewModel: RestaurantPartnerViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.storeOrders.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    val selectedTab by viewModel.selectedOrderTab.collectAsState()

    val newOrders = orders.filter { it.status == OrderStatus.PLACED.name }
    val preparingOrders = orders.filter { it.status in listOf(OrderStatus.CONFIRMED.name, OrderStatus.PREPARING.name) }
    val readyOrders = orders.filter { it.status in listOf(OrderStatus.READY_FOR_PICKUP.name, OrderStatus.OUT_FOR_PICKUP.name, OrderStatus.OUT_FOR_DELIVERY.name) }
    val completedOrders = orders.filter { it.status in listOf(OrderStatus.DELIVERED.name, OrderStatus.CANCELLED.name) }

    val currentList = when (selectedTab) {
        RestaurantOrderTab.NEW -> newOrders
        RestaurantOrderTab.PREPARING -> preparingOrders
        RestaurantOrderTab.READY -> readyOrders
        RestaurantOrderTab.COMPLETED -> completedOrders
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Kitchen Display Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kitchen Display System (KDS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeStore.name} • Live Order Pipeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (newOrders.isNotEmpty()) {
                            Surface(
                                color = PrimaryOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, PrimaryOrange)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${newOrders.size} New",
                                        color = PrimaryOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Status Tabs (New, Preparing, Ready, Completed)
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryOrange,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = PrimaryOrange,
                    height = 3.dp
                )
            }
        ) {
            RestaurantOrderTab.values().forEach { tab ->
                val count = when (tab) {
                    RestaurantOrderTab.NEW -> newOrders.size
                    RestaurantOrderTab.PREPARING -> preparingOrders.size
                    RestaurantOrderTab.READY -> readyOrders.size
                    RestaurantOrderTab.COMPLETED -> completedOrders.size
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.setOrderTab(tab) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.label,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedTab == tab) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == tab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                )
            }
        }

        // Order list or empty state
        if (currentList.isEmpty()) {
            EmptyOrdersState(
                tab = selectedTab
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(currentList, key = { it.orderId }) { order ->
                    RestaurantOrderCard(
                        order = order,
                        tab = selectedTab,
                        onAccept = { prepTime -> viewModel.acceptOrder(order.orderId, prepTime) },
                        onReject = { reason -> viewModel.rejectOrder(order.orderId, reason) },
                        onReady = { viewModel.markReadyForPickup(order.orderId) },
                        onHandedOver = { viewModel.markOrderHandedOver(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantOrderCard(
    order: OrderEntity,
    tab: RestaurantOrderTab,
    onAccept: (prepMinutes: Int) -> Unit,
    onReject: (reason: String) -> Unit,
    onReady: () -> Unit,
    onHandedOver: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("Item out of stock") }
    var selectedPrepMinutes by remember { mutableStateOf(20) }

    // Checklist for cooks in kitchen
    val items = remember(order.itemsSummary) {
        order.itemsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val checkedItems = remember { mutableStateMapOf<Int, Boolean>() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            when (tab) {
                RestaurantOrderTab.NEW -> PrimaryOrange.copy(alpha = 0.5f)
                RestaurantOrderTab.PREPARING -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                RestaurantOrderTab.READY -> GroceryGreen.copy(alpha = 0.5f)
                RestaurantOrderTab.COMPLETED -> Color.LightGray.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("order_card_${order.orderId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Bar: Order ID, Time & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (tab) {
                                    RestaurantOrderTab.NEW -> PrimaryOrange.copy(alpha = 0.15f)
                                    RestaurantOrderTab.PREPARING -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    RestaurantOrderTab.READY -> GroceryGreen.copy(alpha = 0.15f)
                                    RestaurantOrderTab.COMPLETED -> Color.Gray.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                RestaurantOrderTab.NEW -> Icons.Default.ReceiptLong
                                RestaurantOrderTab.PREPARING -> Icons.Default.Restaurant
                                RestaurantOrderTab.READY -> Icons.Default.DeliveryDining
                                RestaurantOrderTab.COMPLETED -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (tab) {
                                RestaurantOrderTab.NEW -> PrimaryOrange
                                RestaurantOrderTab.PREPARING -> Color(0xFFD97706)
                                RestaurantOrderTab.READY -> GroceryGreen
                                RestaurantOrderTab.COMPLETED -> Color.Gray
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Order #${order.orderId.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Placed recently • ${order.paymentMethod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Amount Pill in Indian Rupees
                Surface(
                    color = PrimaryOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹${order.total.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Address info
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${order.addressTitle}: ${order.addressFull.take(45)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Items breakdown
            Text(
                text = "Order Items (${order.itemsCount} items):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            items.forEachIndexed { index, itemText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tab == RestaurantOrderTab.PREPARING) {
                        Checkbox(
                            checked = checkedItems[index] ?: false,
                            onCheckedChange = { checkedItems[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = GroceryGreen),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = itemText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // If rider is assigned, display rider info card
            if (!order.partnerName.isNullOrBlank() && tab in listOf(RestaurantOrderTab.PREPARING, RestaurantOrderTab.READY)) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = GroceryGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GroceryGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GroceryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.partnerName ?: "Delivery Rider Assigned",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = order.partnerVehicle ?: "Arriving at outlet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = GroceryGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = GroceryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Call",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GroceryGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Context-sensitive Action Buttons per Tab
            when (tab) {
                RestaurantOrderTab.NEW -> {
                    // Prep time selector
                    Text(
                        text = "Estimated Preparation Time:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 20, 30, 40).forEach { mins ->
                            FilterChip(
                                selected = selectedPrepMinutes == mins,
                                onClick = { selectedPrepMinutes = mins },
                                label = { Text("${mins}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryOrange
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject")
                        }

                        Button(
                            onClick = { onAccept(selectedPrepMinutes) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("btn_accept_order_${order.orderId}")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accept & Cook (${selectedPrepMinutes}m)")
                        }
                    }
                }

                RestaurantOrderTab.PREPARING -> {
                    Button(
                        onClick = onReady,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_ready_order_${order.orderId}")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Food Ready for Pickup")
                    }
                }

                RestaurantOrderTab.READY -> {
                    Button(
                        onClick = onHandedOver,
                        colors = ButtonDefaults.buttonColors(containerColor = GroceryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_handover_order_${order.orderId}")
                    ) {
                        Icon(imageVector = Icons.Default.DeliveryDining, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hand Over to Delivery Partner")
                    }
                }

                RestaurantOrderTab.COMPLETED -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (order.status == OrderStatus.DELIVERED.name) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (order.status == OrderStatus.DELIVERED.name) GroceryGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (order.status == OrderStatus.DELIVERED.name) "Fulfilled & Delivered" else "Order Cancelled",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (order.status == OrderStatus.DELIVERED.name) GroceryGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Order #${order.orderId.takeLast(6)}") },
            text = {
                Column {
                    Text("Select a reason to inform the customer:")
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "Item out of stock",
                        "Kitchen at maximum capacity",
                        "Store closing soon",
                        "Ingredient shortage"
                    ).forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rejectReason = reason }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (rejectReason == reason) PrimaryOrange else Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject(rejectReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyOrdersState(
    tab: RestaurantOrderTab
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PrimaryOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tab) {
                    RestaurantOrderTab.NEW -> Icons.Default.ReceiptLong
                    RestaurantOrderTab.PREPARING -> Icons.Default.Restaurant
                    RestaurantOrderTab.READY -> Icons.Default.DeliveryDining
                    RestaurantOrderTab.COMPLETED -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (tab) {
                RestaurantOrderTab.NEW -> "No new orders right now"
                RestaurantOrderTab.PREPARING -> "Kitchen is clear"
                RestaurantOrderTab.READY -> "No orders awaiting pickup"
                RestaurantOrderTab.COMPLETED -> "No order history yet"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (tab) {
                RestaurantOrderTab.NEW -> "Incoming customer orders will chime here in real time."
                RestaurantOrderTab.PREPARING -> "Accepted orders will appear here for preparation."
                RestaurantOrderTab.READY -> "Prepared dishes will wait here for delivery riders."
                RestaurantOrderTab.COMPLETED -> "Past delivered and cancelled orders will be archived here."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
