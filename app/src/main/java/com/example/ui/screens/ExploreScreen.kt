package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessType
import com.example.model.Store
import com.example.ui.components.FoodGroceryToggle
import com.example.ui.components.StoreCard
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.GroceryGreenDark
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.PrimaryOrangeDark
import com.example.ui.theme.RatingGold
import com.example.ui.theme.VegGreen
import com.example.viewmodel.CustomerDeliveryViewModel

@Composable
fun ExploreScreen(
    viewModel: CustomerDeliveryViewModel,
    modifier: Modifier = Modifier
) {
    val businessType by viewModel.currentBusinessType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val vegOnly by viewModel.vegOnlyFilter.collectAsState()
    val fastDelivery by viewModel.fastDeliveryFilter.collectAsState()
    val rating4Plus by viewModel.rating4PlusFilter.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val categories = if (businessType == BusinessType.FOOD) {
        viewModel.repository.foodCategories
    } else {
        viewModel.repository.groceryCategories
    }

    // Filter stores based on type, search and filter chips
    val allStores = viewModel.repository.getStores(businessType)
    val filteredStores = allStores.filter { store ->
        val matchesSearch = if (searchQuery.isBlank()) true else {
            store.name.contains(searchQuery, ignoreCase = true) ||
            store.cuisines.any { it.contains(searchQuery, ignoreCase = true) } ||
            store.tagline.contains(searchQuery, ignoreCase = true)
        }
        val matchesVeg = if (vegOnly) store.isVegOnly else true
        val matchesFast = if (fastDelivery) store.deliveryTimeMin <= 20 else true
        val matchesRating = if (rating4Plus) store.rating >= 4.5 else true
        val matchesCategory = if (selectedCategory == "all" || selectedCategory == "all_g") true else {
            store.cuisines.any { it.contains(selectedCategory, ignoreCase = true) }
        }

        matchesSearch && matchesVeg && matchesFast && matchesRating && matchesCategory
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen_list"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Food vs Grocery Toggle
        item {
            FoodGroceryToggle(
                selectedType = businessType,
                onSelectType = { viewModel.setBusinessType(it) }
            )
        }

        // 2. Search Field
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = if (businessType == BusinessType.FOOD) {
                                "Search for Biryani, Burgers, Pizza or Cafes..."
                            } else {
                                "Search for Milk, Veggies, Snacks or Fruits..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (businessType == BusinessType.FOOD) PrimaryOrange else GroceryGreenDark
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (businessType == BusinessType.FOOD) PrimaryOrange else GroceryGreenDark,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )
            }
        }

        // 3. Quick Category Pills
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = if (businessType == BusinessType.FOOD) "What's on your mind?" else "Explore Grocery Aisles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category.id
                        Surface(
                            color = if (isSelected) {
                                if (businessType == BusinessType.FOOD) PrimaryOrange else GroceryGreenDark
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .clickable {
                                    if (isSelected) viewModel.setCategory(if (businessType == BusinessType.FOOD) "all" else "all_g")
                                    else viewModel.setCategory(category.id)
                                }
                                .testTag("category_chip_${category.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Smart Filter Chips (Veg only, 4.0+ Stars, Fast Delivery)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Veg Only filter
                FilterChip(
                    selected = vegOnly,
                    onClick = { viewModel.toggleVegFilter() },
                    label = { Text("Pure Veg") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(VegGreen)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VegGreen.copy(alpha = 0.15f),
                        selectedLabelColor = VegGreen
                    ),
                    modifier = Modifier.testTag("filter_veg_chip")
                )

                // Rating 4.0+
                FilterChip(
                    selected = rating4Plus,
                    onClick = { viewModel.toggleRatingFilter() },
                    label = { Text("Ratings 4.5+") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_rating_chip")
                )

                // Fast Delivery
                FilterChip(
                    selected = fastDelivery,
                    onClick = { viewModel.toggleFastDeliveryFilter() },
                    label = { Text("⚡ Under 20 mins") },
                    modifier = Modifier.testTag("filter_fast_chip")
                )
            }
        }

        // 6. Section Header: Restaurants or Stores List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (businessType == BusinessType.FOOD) {
                        "Popular Restaurants (${filteredStores.size})"
                    } else {
                        "Top Grocery Stores & Dark Hubs (${filteredStores.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 7. Store Cards List
        if (filteredStores.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryOrange.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (allStores.isEmpty()) Icons.Default.ShoppingBag else Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (allStores.isEmpty()) "No Outlets Listed Yet" else "No matches found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (allStores.isEmpty()) {
                                "Restaurants and grocery hubs registered via the Partner App or Admin Portal will appear here live."
                            } else {
                                "Try clearing search or relaxing your filter chips."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredStores) { store ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    StoreCard(
                        store = store,
                        isFavorite = favorites.contains(store.id),
                        onFavoriteToggle = { viewModel.toggleFavorite(store.id) },
                        onClick = { viewModel.openStore(store.id) }
                    )
                }
            }
        }
    }
}
