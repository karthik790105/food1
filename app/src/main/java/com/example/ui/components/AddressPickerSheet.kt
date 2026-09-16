package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryAddress
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen
import com.example.util.CurrentLocationInfo
import com.example.viewmodel.CustomerDeliveryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPickerSheet(
    viewModel: CustomerDeliveryViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val addresses by viewModel.userAddresses.collectAsState()
    val selectedId by viewModel.selectedAddressId.collectAsState()
    val isLocationLoading by viewModel.isLocationLoading.collectAsState()
    val locationError by viewModel.locationErrorMessage.collectAsState()
    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    val locationInfo by viewModel.currentLocationInfo.collectAsState()
    var isAddingNew by remember(addresses.isEmpty()) { mutableStateOf(addresses.isEmpty()) }

    var newTitle by remember { mutableStateOf("Home") }
    var newFullAddress by remember { mutableStateOf("") }
    var newLandmark by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentGpsLocation {
                onDismiss()
            }
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun launchGoogleMapsQuery(query: String) {
        try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun launchGoogleMapsCoordinates(lat: Double, lng: Double, label: String = "Delivery Location") {
        try {
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAddingNew) "Add Delivery Address" else "Delivery Address & Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time Google Maps GPS enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Google Maps Live Location Card
            RealTimeGoogleMapsCard(
                latitude = userLat,
                longitude = userLng,
                locationInfo = locationInfo,
                isLoading = isLocationLoading,
                onRefreshGps = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    viewModel.fetchCurrentGpsLocation()
                },
                onOpenMaps = {
                    launchGoogleMapsCoordinates(userLat, userLng, "Delivery Pin")
                },
                onUseLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    viewModel.fetchCurrentGpsLocation {
                        onDismiss()
                    }
                }
            )

            if (!locationError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = locationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isAddingNew && addresses.isNotEmpty()) {
                // Section Title: Saved Delivery Addresses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Delivery Addresses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${addresses.size} Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of Saved Addresses
                addresses.forEach { addr ->
                    val isSelected = addr.id == selectedId
                    Surface(
                        color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                viewModel.setSelectedAddress(addr.id)
                                onDismiss()
                            }
                            .testTag("address_item_${addr.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (addr.title.lowercase()) {
                                        "home" -> Icons.Default.Home
                                        "work" -> Icons.Default.Work
                                        else -> Icons.Default.LocationOn
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = addr.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (addr.id == "addr_gps") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = VegGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "GPS PIN",
                                                color = VegGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = addr.fullAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                                if (addr.landmark.isNotBlank()) {
                                    Text(
                                        text = "Near ${addr.landmark}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryOrange
                                    )
                                }
                            }

                            // View in Google Maps button
                            IconButton(
                                onClick = { launchGoogleMapsQuery(addr.fullAddress) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "View in Google Maps",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteAddress(addr.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Address",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add New Address Button
                Button(
                    onClick = { isAddingNew = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_new_address_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Another Address")
                }
            } else if (!isAddingNew && addresses.isEmpty()) {
                // Empty state when no addresses exist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved delivery address",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use the live Google Maps GPS pin above or enter your delivery details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { isAddingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_first_address_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Address Manually")
                    }
                }
            } else {
                // Add New Address Form
                // Autofill via GPS
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        viewModel.fetchCurrentGpsLocation {
                            val loc = viewModel.currentLocationInfo.value
                            if (loc != null) {
                                newTitle = loc.title
                                newFullAddress = loc.fullAddress
                                newLandmark = "${loc.area}, ${loc.city}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("autofill_gps_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-fill from Google Maps Live GPS", color = PrimaryOrange)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick autofill preset chips from Google Maps
                Text(
                    text = "Google Maps Quick Locations",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            newTitle = "Indiranagar Home"
                            newFullAddress = "12th Main Road, HAL 2nd Stage, Indiranagar, Bengaluru, Karnataka 560038"
                            newLandmark = "Near 100ft Road Metro"
                        },
                        label = { Text("Indiranagar") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                    )
                    AssistChip(
                        onClick = {
                            newTitle = "Koramangala Office"
                            newFullAddress = "5th Block, 80 Feet Road, Koramangala, Bengaluru, Karnataka 560095"
                            newLandmark = "Near Sony World Signal"
                        },
                        label = { Text("Koramangala") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                    )
                    AssistChip(
                        onClick = {
                            newTitle = "Connaught Place"
                            newFullAddress = "Inner Circle, Block C, Connaught Place, New Delhi 110001"
                            newLandmark = "Near Rajiv Chowk Metro"
                        },
                        label = { Text("Delhi CP") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                    )
                    AssistChip(
                        onClick = {
                            newTitle = "Bandra West"
                            newFullAddress = "Hill Road, Bandra West, Mumbai, Maharashtra 400050"
                            newLandmark = "Near Bandra Police Station"
                        },
                        label = { Text("Mumbai Bandra") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Address Label (e.g. Home, Work, Apartment)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_address_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newFullAddress,
                    onValueChange = { newFullAddress = it },
                    label = { Text("Complete Delivery Address (House/Flat, Street, Area)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_address_full")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newLandmark,
                    onValueChange = { newLandmark = it },
                    label = { Text("Nearby Landmark (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_address_landmark")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Verify Address on Google Maps Button
                if (newFullAddress.isNotBlank()) {
                    OutlinedButton(
                        onClick = { launchGoogleMapsQuery(newFullAddress) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Pin in Google Maps", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (addresses.isNotEmpty()) {
                        Button(
                            onClick = { isAddingNew = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Button(
                        onClick = {
                            if (newFullAddress.isNotBlank()) {
                                viewModel.addNewAddress(newTitle, newFullAddress, newLandmark)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = newFullAddress.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_new_address_btn")
                    ) {
                        Text("Save & Deliver Here")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Visual Real-Time Google Maps Location Card.
 * Renders an animated live vector map canvas with expanding radar pulse waves,
 * centered Google Maps pin, real-time latitude/longitude coordinates,
 * geocoded locality info, and direct Google Maps deep-link actions.
 */
@Composable
fun RealTimeGoogleMapsCard(
    latitude: Double,
    longitude: Double,
    locationInfo: CurrentLocationInfo?,
    isLoading: Boolean,
    onRefreshGps: () -> Unit,
    onOpenMaps: () -> Unit,
    onUseLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_radar_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 68f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Bar: Title + GPS Active Indicator + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Real-Time Google Maps GPS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(VegGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLoading) "Updating GPS location..." else "High-Accuracy Fused Provider",
                                fontSize = 11.sp,
                                color = if (isLoading) PrimaryOrange else VegGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRefreshGps,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = PrimaryOrange,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh GPS",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Embedded Interactive-Style Google Maps Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                // Vector Map Canvas: Roads, Parks, River, and Concentric Radar Pulses
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // 1. Terrain Base
                    drawRect(color = Color(0xFFF8FAFC))

                    // 2. Green Park Area (Top-Left)
                    drawRoundRect(
                        color = Color(0xFFDCFCE7),
                        topLeft = Offset(w * 0.05f, h * 0.08f),
                        size = Size(w * 0.28f, h * 0.42f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // 3. Green Park Area (Bottom-Right)
                    drawRoundRect(
                        color = Color(0xFFDCFCE7),
                        topLeft = Offset(w * 0.70f, h * 0.55f),
                        size = Size(w * 0.25f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // 4. Soft Blue River / Water Canal (Diagonal)
                    drawLine(
                        color = Color(0xFFBAE6FD),
                        start = Offset(0f, h * 0.85f),
                        end = Offset(w * 0.55f, h),
                        strokeWidth = 20f
                    )

                    // 5. Road Network (Horizontal Main Arterial Road)
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 18f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 12f
                    )

                    // 6. Road Network (Vertical Avenue)
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 18f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 12f
                    )

                    // 7. Secondary Cross Streets
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(w * 0.25f, 0f),
                        end = Offset(w * 0.25f, h),
                        strokeWidth = 8f
                    )
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(w * 0.75f, 0f),
                        end = Offset(w * 0.75f, h),
                        strokeWidth = 8f
                    )

                    // 8. Dynamic Radar Wave Pulses around customer pinpoint
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = (pulseAlpha * 0.6f)),
                        radius = pulseRadius * 0.6f,
                        center = Offset(cx, cy)
                    )
                    // High-accuracy boundary ring
                    drawCircle(
                        color = Color(0xFF2563EB).copy(alpha = 0.25f),
                        radius = 28f,
                        center = Offset(cx, cy)
                    )
                    // Pin drop center shadow
                    drawCircle(
                        color = Color(0x33000000),
                        radius = 8f,
                        center = Offset(cx, cy + 4f)
                    )
                }

                // Centered Google Maps Pin Icon
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "My GPS Pin",
                        tint = Color(0xFFEA4335), // Google Maps Red
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Floating Google Maps Watermark Badge (Bottom-Left)
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF4285F4), // Google Blue
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Google Maps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // Floating Compass Badge (Top-Right)
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Compass",
                            tint = Color(0xFFEA4335),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Floating Live GPS Coordinates (Bottom-Right)
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.4f° N, %.4f° E", latitude, longitude),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detected Address Information
            val detectedTitle = locationInfo?.title ?: "Current Location Pin"
            val detectedAddress = locationInfo?.fullAddress
                ?: "Indiranagar 12th Main Road, HAL 2nd Stage, Bengaluru, Karnataka 560038"

            Text(
                text = detectedTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detectedAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: "Deliver Here" & "Open in Google Maps"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMaps,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View on Maps",
                        fontSize = 12.sp,
                        color = PrimaryOrange
                    )
                }

                Button(
                    onClick = onUseLocation,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deliver to Pin",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

