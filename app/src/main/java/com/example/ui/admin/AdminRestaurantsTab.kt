package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MenuItem
import com.example.model.Store
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.RatingGold
import com.example.ui.theme.VegGreen
import com.example.viewmodel.AdminPortalViewModel

@Composable
fun AdminRestaurantsTab(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val stores by viewModel.allStores.collectAsStateWithLifecycle()
    val suspendedStores by viewModel.suspendedStores.collectAsStateWithLifecycle()
    val storeOnlineStatus by viewModel.storeOnlineStatus.collectAsStateWithLifecycle()
    val selectedStoreForMenu by viewModel.selectedStoreForMenu.collectAsStateWithLifecycle()

    var restaurantSearchQuery by remember { mutableStateOf("") }
    var storeToSuspendConfirm by remember { mutableStateOf<Store?>(null) }

    val filteredStores = stores.filter { store ->
        restaurantSearchQuery.isBlank() ||
                store.name.contains(restaurantSearchQuery.trim(), ignoreCase = true) ||
                store.cuisines.any { it.contains(restaurantSearchQuery.trim(), ignoreCase = true) } ||
                store.location.contains(restaurantSearchQuery.trim(), ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_restaurants_tab")
    ) {
        // Search Bar Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = restaurantSearchQuery,
                    onValueChange = { restaurantSearchQuery = it },
                    placeholder = { Text("Search restaurants by name, cuisine, location") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange)
                    },
                    trailingIcon = {
                        if (restaurantSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { restaurantSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_restaurant_search_input")
                )
            }
        }

        // Stores List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredStores, key = { it.id }) { store ->
                val isSuspended = suspendedStores.contains(store.id)
                val isOnline = storeOnlineStatus[store.id] ?: true

                AdminRestaurantCard(
                    store = store,
                    isSuspended = isSuspended,
                    isOnline = isOnline,
                    onToggleOnline = { viewModel.toggleStoreOnline(store.id) },
                    onToggleSuspension = { storeToSuspendConfirm = store },
                    onManageMenu = { viewModel.selectStoreForMenu(store) }
                )
            }
        }
    }

    // Suspension Confirmation Dialog
    storeToSuspendConfirm?.let { store ->
        val currentlySuspended = suspendedStores.contains(store.id)
        AlertDialog(
            onDismissRequest = { storeToSuspendConfirm = null },
            icon = {
                Icon(
                    imageVector = if (currentlySuspended) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (currentlySuspended) VegGreen else NonVegRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(if (currentlySuspended) "Reactivate ${store.name}?" else "Suspend ${store.name}?")
            },
            text = {
                Text(
                    if (currentlySuspended)
                        "Reactivating this restaurant allows customers to place orders again and restores menu visibility."
                    else
                        "Suspending this restaurant will block new customer orders immediately and flag the restaurant in the partner portal."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleStoreSuspension(store.id)
                        storeToSuspendConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentlySuspended) VegGreen else NonVegRed
                    )
                ) {
                    Text(if (currentlySuspended) "Reactivate Store" else "Suspend Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToSuspendConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Menu Management Modal Dialog
    selectedStoreForMenu?.let { store ->
        AdminMenuManagementDialog(
            store = store,
            viewModel = viewModel,
            onDismiss = { viewModel.selectStoreForMenu(null) }
        )
    }
}

@Composable
fun AdminRestaurantCard(
    store: Store,
    isSuspended: Boolean,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    onToggleSuspension: () -> Unit,
    onManageMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isSuspended) NonVegRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_store_card_${store.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Cuisine & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = store.cuisines.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RatingGold.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${store.rating}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RatingGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = store.location,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suspended Banner (if applicable)
            if (isSuspended) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NonVegRed.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = NonVegRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SUSPENDED BY ADMIN • Ordering Disabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NonVegRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Status Toggles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online/Offline Status Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOnline) "🟢 Outlet Online" else "🔴 Outlet Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) VegGreen else NonVegRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { onToggleOnline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VegGreen,
                            checkedTrackColor = VegGreen.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("admin_store_online_switch_${store.id}")
                    )
                }

                // Suspend / Reactivate Action
                OutlinedButton(
                    onClick = onToggleSuspension,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSuspended) VegGreen else NonVegRed
                    ),
                    modifier = Modifier.testTag("admin_store_suspend_btn_${store.id}")
                ) {
                    Icon(
                        imageVector = if (isSuspended) Icons.Default.Check else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isSuspended) "Reactivate" else "Suspend", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Manage Menu Button
            Button(
                onClick = onManageMenu,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_manage_menu_btn_${store.id}")
            ) {
                Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Menu & Pricing", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Dialog for Admin to control a restaurant's menu items:
 * In-stock toggle, price updates, item deletion, and adding new items.
 */
@Composable
fun AdminMenuManagementDialog(
    store: Store,
    viewModel: AdminPortalViewModel,
    onDismiss: () -> Unit
) {
    val menuItems by viewModel.menuItemsForSelectedStore.collectAsStateWithLifecycle()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEditPrice by remember { mutableStateOf<MenuItem?>(null) }
    var itemToDeleteConfirm by remember { mutableStateOf<MenuItem?>(null) }
    var menuSearchQuery by remember { mutableStateOf("") }

    val filteredItems = menuItems.filter { item ->
        menuSearchQuery.isBlank() ||
                item.name.contains(menuSearchQuery.trim(), ignoreCase = true) ||
                item.category.contains(menuSearchQuery.trim(), ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("admin_menu_management_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header in Primary Orange
                Surface(
                    color = PrimaryOrange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Menu & Inventory Management (${menuItems.size} items)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Controls Bar: Search & Add Item Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = menuSearchQuery,
                            onValueChange = { menuSearchQuery = it },
                            placeholder = { Text("Filter items...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_add_menu_item_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 12.sp)
                        }
                    }
                }

                // Menu Items List
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No menu items found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            AdminMenuItemCard(
                                item = item,
                                onToggleStock = { viewModel.toggleItemStock(item.id) },
                                onEditPrice = { itemToEditPrice = item },
                                onDelete = { itemToDeleteConfirm = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add New Menu Item Dialog
    if (showAddItemDialog) {
        AdminAddItemDialog(
            storeId = store.id,
            viewModel = viewModel,
            onDismiss = { showAddItemDialog = false }
        )
    }

    // Edit Item Price Dialog
    itemToEditPrice?.let { item ->
        var newPriceText by remember { mutableStateOf("${item.price.toInt()}") }
        AlertDialog(
            onDismissRequest = { itemToEditPrice = null },
            title = { Text("Update Price: ${item.name}") },
            text = {
                Column {
                    Text("Current price: ₹${item.price.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPriceText,
                        onValueChange = { newPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("New Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newPriceText.toDoubleOrNull() ?: 0.0
                        if (parsed > 0) {
                            viewModel.updateItemPrice(item.id, parsed)
                            itemToEditPrice = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Save Price")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEditPrice = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Item Confirmation Dialog
    itemToDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDeleteConfirm = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NonVegRed) },
            title = { Text("Remove Item?") },
            text = { Text("Are you sure you want to permanently remove '${item.name}' from ${store.name}'s menu?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMenuItem(item.id)
                        itemToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NonVegRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminMenuItemCard(
    item: MenuItem,
    onToggleStock: () -> Unit,
    onEditPrice: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_menu_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (item.isVeg) VegGreen else NonVegRed),
                        modifier = Modifier.size(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (item.isVeg) VegGreen else NonVegRed)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.category} • ${item.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${item.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryOrange,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onEditPrice,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Price", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Stock Toggle & Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.inStock) "In Stock" else "Out of Stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.inStock) VegGreen else NonVegRed
                    )
                    Switch(
                        checked = item.inStock,
                        onCheckedChange = { onToggleStock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VegGreen,
                            checkedTrackColor = VegGreen.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("admin_item_stock_switch_${item.id}")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = NonVegRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AdminAddItemDialog(
    storeId: String,
    viewModel: AdminPortalViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isVeg by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("Mains") }
    var unit by remember { mutableStateOf("1 serving") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Menu Item") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Special Paneer Biryani") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Price (₹) *") },
                    placeholder = { Text("e.g. 240") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isVeg,
                        onClick = { isVeg = true },
                        label = { Text("Veg") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VegGreen.copy(alpha = 0.15f),
                            selectedLabelColor = VegGreen
                        )
                    )
                    FilterChip(
                        selected = !isVeg,
                        onClick = { isVeg = false },
                        label = { Text("Non-Veg") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NonVegRed.copy(alpha = 0.15f),
                            selectedLabelColor = NonVegRed
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Portion / Unit") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val added = viewModel.addMenuItem(
                        storeId = storeId,
                        name = name,
                        description = description,
                        price = price,
                        isVeg = isVeg,
                        category = category,
                        unit = unit
                    )
                    if (added) {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("Add to Menu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
