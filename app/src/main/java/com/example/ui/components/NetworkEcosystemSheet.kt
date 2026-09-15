package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.GroceryGreenDark
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkEcosystemSheet(
    onDismiss: () -> Unit,
    isCloudConnected: Boolean = false,
    cloudStatus: String = "Firebase Firestore Ready",
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("network_ecosystem_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Shared Firebase Database",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "4 Separate Standalone Apps • 1 Shared Firestore DB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Firebase Cloud Connection Status Banner
            Surface(
                color = if (isCloudConnected) VegGreen.copy(alpha = 0.1f) else PrimaryOrange.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCloudConnected) VegGreen.copy(alpha = 0.4f) else PrimaryOrange.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CheckCircle else Icons.Default.Hub,
                        contentDescription = null,
                        tint = if (isCloudConnected) VegGreen else PrimaryOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isCloudConnected) "Cloud Firestore: Connected & Synchronized" else "Database Engine: Firebase Firestore Configured",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCloudConnected) VegGreen else PrimaryOrange
                        )
                        Text(
                            text = "Collections: 'orders', 'stores', 'users' • Currency: Indian Rupee (₹)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Customer Base App (ACTIVE)
            EcosystemRoleCard(
                title = "1. Customer App (This App)",
                status = "ACTIVE APP",
                statusColor = VegGreen,
                icon = Icons.Default.Person,
                iconColor = PrimaryOrange,
                description = "Dedicated Android app for end customers. Places orders into the shared Firestore 'orders' collection in Indian Rupees (₹) and listens for live updates from kitchen and riders.",
                isCurrent = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Restaurant & Shop App
            EcosystemRoleCard(
                title = "2. Merchant / Store Partner App",
                status = "STANDALONE APP",
                statusColor = Color(0xFF3B82F6),
                icon = Icons.Default.Store,
                iconColor = Color(0xFF3B82F6),
                description = "Independent app used by store managers. Listens to shared orders, accepts orders, marks them CONFIRMED/PREPARING, and manages catalog inventory.",
                isCurrent = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Delivery Partner App
            EcosystemRoleCard(
                title = "3. Delivery Rider App",
                status = "STANDALONE APP",
                statusColor = GroceryGreenDark,
                icon = Icons.Default.DeliveryDining,
                iconColor = GroceryGreenDark,
                description = "Independent app used by riders. Reads available orders, claims deliveries, updates live partner details, and completes delivery via customer OTP.",
                isCurrent = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Admin App
            EcosystemRoleCard(
                title = "4. Central Admin Console App",
                status = "STANDALONE APP",
                statusColor = Color(0xFF8B5CF6),
                icon = Icons.Default.AdminPanelSettings,
                iconColor = Color(0xFF8B5CF6),
                description = "Independent administrative console app. Monitors city-wide orders, handles disputes, and oversees stores and riders.",
                isCurrent = false
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun EcosystemRoleCard(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    iconColor: Color,
    description: String,
    isCurrent: Boolean,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) PrimaryOrange.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (actionLabel != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Button(
                        onClick = onActionClick,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = iconColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_ecosystem_action")
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
