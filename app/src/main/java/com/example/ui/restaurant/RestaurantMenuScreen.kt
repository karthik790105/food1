package com.example.ui.restaurant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MenuItem
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen
import com.example.viewmodel.RestaurantPartnerViewModel

@Composable
fun VegIndicator(
    isVeg: Boolean,
    size: androidx.compose.ui.unit.Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isVeg) VegGreen else Color(0xFFE53935)
    Box(
        modifier = modifier
            .size(size)
            .border(1.5.dp, indicatorColor, RoundedCornerShape(3.dp))
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}

@Composable
fun RestaurantMenuScreen(
    viewModel: RestaurantPartnerViewModel,
    modifier: Modifier = Modifier
) {
    val activeStore by viewModel.activeStore.collectAsState()
    val menuItems by viewModel.filteredMenuItems.collectAsState()
    val allItems by viewModel.storeMenuItems.collectAsState()
    val searchQuery by viewModel.menuSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedMenuCategory.collectAsState()

    var editingItem by remember { mutableStateOf<MenuItem?>(null) }

    val categories = remember(allItems) {
        listOf("All") + allItems.map { it.category }.distinct()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Menu & Inventory Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeStore.name} • ${allItems.size} Total Dishes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.menuSearchQuery.value = it },
                        placeholder = { Text("Search dishes, ingredients...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.menuSearchQuery.value = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_menu_search")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categories scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.selectedMenuCategory.value = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryOrange
                                )
                            )
                        }
                    }
                }
            }

            // Menu Items List
            if (menuItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No dishes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing search filters or add a new dish to this outlet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(menuItems, key = { it.id }) { item ->
                        RestaurantMenuItemRow(
                            item = item,
                            onToggleStock = { viewModel.toggleItemStock(item.id) },
                            onEditPrice = { editingItem = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Edit Price Dialog
    editingItem?.let { item ->
        var newPriceInput by remember(item) { mutableStateOf(item.price.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Update Price for ${item.name}") },
            text = {
                Column {
                    Text(
                        text = "Enter the new billing price in Indian Rupees (₹):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { newPriceInput = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Price in ₹") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_price")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newPriceInput.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            viewModel.updateItemPrice(item.id, parsed)
                            editingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Save Price")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RestaurantMenuItemRow(
    item: MenuItem,
    onToggleStock: () -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.inStock) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.inStock) 1.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_item_row_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail image
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!item.inStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OFF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VegIndicator(isVeg = item.isVeg, size = 12.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.inStock) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${item.price.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.inStock) PrimaryOrange else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditPrice,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Price",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // In-stock toggle switch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Switch(
                    checked = item.inStock,
                    onCheckedChange = { onToggleStock() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GroceryGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("switch_stock_${item.id}")
                )
                Text(
                    text = if (item.inStock) "In Stock" else "Sold Out",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.inStock) GroceryGreen else Color.Red
                )
            }
        }
    }
}
