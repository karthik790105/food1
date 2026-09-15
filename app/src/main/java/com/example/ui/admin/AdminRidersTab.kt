package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DeliveryPartner
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.RatingGold
import com.example.ui.theme.VegGreen
import com.example.viewmodel.AdminPortalViewModel

@Composable
fun AdminRidersTab(
    viewModel: AdminPortalViewModel,
    modifier: Modifier = Modifier
) {
    val riders by viewModel.allDeliveryPartners.collectAsStateWithLifecycle()
    val suspendedRiders by viewModel.suspendedRiders.collectAsStateWithLifecycle()
    var riderSearchQuery by remember { mutableStateOf("") }
    var riderToSuspendConfirm by remember { mutableStateOf<DeliveryPartner?>(null) }

    val filteredRiders = riders.filter { rider ->
        riderSearchQuery.isBlank() ||
                rider.name.contains(riderSearchQuery.trim(), ignoreCase = true) ||
                rider.phone.contains(riderSearchQuery.trim()) ||
                rider.vehicle.contains(riderSearchQuery.trim(), ignoreCase = true) ||
                rider.vehicleNumber.contains(riderSearchQuery.trim(), ignoreCase = true) ||
                rider.activeCity.contains(riderSearchQuery.trim(), ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_riders_tab")
    ) {
        // Search Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = riderSearchQuery,
                    onValueChange = { riderSearchQuery = it },
                    placeholder = { Text("Search riders by name, phone, vehicle, or city") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange)
                    },
                    trailingIcon = {
                        if (riderSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { riderSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_rider_search_input")
                )
            }
        }

        // Riders List
        if (filteredRiders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DeliveryDining,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (riderSearchQuery.isNotEmpty()) "No matching delivery agents found" else "No registered riders in network",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Riders registered via the BiteMart Delivery app will sync here for central control.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredRiders, key = { it.id }) { rider ->
                    val isSuspended = suspendedRiders.contains(rider.id)
                    AdminRiderCard(
                        rider = rider,
                        isSuspended = isSuspended,
                        onToggleSuspension = { riderToSuspendConfirm = rider }
                    )
                }
            }
        }
    }

    // Suspension Alert Dialog
    riderToSuspendConfirm?.let { rider ->
        val isCurrentlySuspended = suspendedRiders.contains(rider.id)
        AlertDialog(
            onDismissRequest = { riderToSuspendConfirm = null },
            icon = {
                Icon(
                    imageVector = if (isCurrentlySuspended) Icons.Default.Check else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isCurrentlySuspended) VegGreen else NonVegRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(if (isCurrentlySuspended) "Reactivate Rider ${rider.name}?" else "Suspend Rider ${rider.name}?")
            },
            text = {
                Text(
                    if (isCurrentlySuspended)
                        "Reactivating this delivery partner allows them to receive order dispatches and deliver again."
                    else
                        "Suspending this rider will block them from accepting new orders immediately."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleRiderSuspension(rider.id)
                        riderToSuspendConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlySuspended) VegGreen else NonVegRed
                    )
                ) {
                    Text(if (isCurrentlySuspended) "Restore Rider" else "Suspend Rider")
                }
            },
            dismissButton = {
                TextButton(onClick = { riderToSuspendConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminRiderCard(
    rider: DeliveryPartner,
    isSuspended: Boolean,
    onToggleSuspension: () -> Unit,
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
            .testTag("admin_rider_card_${rider.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Online status, and Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (rider.isOnline) VegGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (rider.isOnline) "🟢 On Duty (Online)" else "⚪ Off Duty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rider.isOnline) VegGreen else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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
                            text = "${rider.rating}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RatingGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact & Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = rider.phone, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                if (rider.email.isNotBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = rider.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${rider.vehicle} (${rider.vehicleNumber.ifBlank { "Unregistered" }}) • ${rider.totalDeliveries} Deliveries",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "City: ${rider.activeCity} • License: ${rider.drivingLicense.ifBlank { "Verified" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Suspended Banner
            if (isSuspended) {
                Spacer(modifier = Modifier.height(10.dp))
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
                            text = "RIDER SUSPENDED BY ADMIN • Order Dispatch Blocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NonVegRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Suspend / Reactivate Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onToggleSuspension,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSuspended) VegGreen else NonVegRed
                    ),
                    modifier = Modifier.testTag("admin_rider_suspend_btn_${rider.id}")
                ) {
                    Icon(
                        imageVector = if (isSuspended) Icons.Default.Check else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSuspended) "Reactivate Rider" else "Suspend Rider",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
