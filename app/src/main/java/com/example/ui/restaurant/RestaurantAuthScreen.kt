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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessType
import com.example.model.RestaurantOwner
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.PrimaryOrange
import com.example.viewmodel.RestaurantPartnerViewModel

@Composable
fun RestaurantAuthScreen(
    viewModel: RestaurantPartnerViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("restaurant_auth_screen")
    ) {
        // App Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "BiteMart Partner",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "BiteMart Restaurant Partner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Merchant Kitchen Display & Operations Portal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PrimaryOrange,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Merchant Login",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_partner_login")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Register New Restaurant",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_partner_register")
            )
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (selectedTab == 0) {
                PartnerLoginForm(
                    viewModel = viewModel,
                    onNavigateToRegister = { selectedTab = 1 }
                )
            } else {
                PartnerRegisterForm(
                    viewModel = viewModel,
                    onNavigateToLogin = { selectedTab = 0 }
                )
            }
        }
    }
}

@Composable
private fun PartnerLoginForm(
    viewModel: RestaurantPartnerViewModel,
    onNavigateToRegister: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val demoAccounts = remember { viewModel.authManager.getDemoAccounts() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Sign in to Your Outlet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter your registered merchant email or phone number to access your kitchen display and menu catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        errorMessage = null
                    },
                    label = { Text("Email or 10-Digit Mobile") },
                    placeholder = { Text("e.g. owner@biryani.com or 9845012345") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_partner_id")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password / PIN") },
                    placeholder = { Text("Enter password (demo PIN: 1234)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PrimaryOrange)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_partner_password")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (identifier.isBlank()) {
                            errorMessage = "Please enter email or phone number"
                            return@Button
                        }
                        val err = viewModel.loginRestaurantOwner(identifier, password.ifBlank { "1234" })
                        if (err != null) {
                            errorMessage = err
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_partner_login_submit")
                ) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In to Restaurant Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        if (demoAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Registered Partner Accounts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select any registered outlet to immediately access their exclusive kitchen portal:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            demoAccounts.forEach { owner ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            identifier = owner.email
                            password = "1234"
                            val err = viewModel.loginRestaurantOwner(owner.email, "1234")
                            if (err != null) {
                                errorMessage = err
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (owner.businessType == BusinessType.FOOD) PrimaryOrange.copy(alpha = 0.15f)
                                    else GroceryGreen.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (owner.businessType == BusinessType.FOOD) Icons.Default.Restaurant else Icons.Default.Storefront,
                                contentDescription = null,
                                tint = if (owner.businessType == BusinessType.FOOD) PrimaryOrange else GroceryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = owner.restaurantName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Owner: ${owner.ownerName} • ${owner.cuisine}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = PrimaryOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Login",
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Want to list your new restaurant?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Register Outlet",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PartnerRegisterForm(
    viewModel: RestaurantPartnerViewModel,
    onNavigateToLogin: () -> Unit
) {
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var restaurantName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf(BusinessType.FOOD) }
    var cuisine by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var fssaiNumber by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Onboard Your Restaurant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create your dedicated merchant account. Once onboarded, you will have exclusive control over your menu catalog, live orders and kitchen availability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Owner Details
                Text(
                    text = "1. Owner Credentials",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = {
                        ownerName = it
                        errorMessage = null
                    },
                    label = { Text("Owner Full Name *") },
                    placeholder = { Text("e.g. Vikram Malhotra") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_owner_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email *") },
                        placeholder = { Text("partner@store.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_reg_email")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorMessage = null
                        },
                        label = { Text("Mobile *") },
                        placeholder = { Text("9876543210") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = PrimaryOrange)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_reg_phone")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Portal Password / PIN *") },
                    placeholder = { Text("Create password (e.g. 1234)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = PrimaryOrange)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_password")
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Restaurant Details
                Text(
                    text = "2. Restaurant Outlet Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = restaurantName,
                    onValueChange = {
                        restaurantName = it
                        errorMessage = null
                    },
                    label = { Text("Restaurant / Outlet Name *") },
                    placeholder = { Text("e.g. Punjab Grill & Tandoor") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_restaurant_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Business Type Selection
                Text(
                    text = "Store Category",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = businessType == BusinessType.FOOD,
                        onClick = { businessType = BusinessType.FOOD },
                        label = { Text("🍽️ Restaurant & Food") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryOrange
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = businessType == BusinessType.GROCERY,
                        onClick = { businessType = BusinessType.GROCERY },
                        label = { Text("🛒 Grocery & Mart") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GroceryGreen.copy(alpha = 0.15f),
                            selectedLabelColor = GroceryGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cuisine,
                    onValueChange = {
                        cuisine = it
                        errorMessage = null
                    },
                    label = { Text("Cuisine / Speciality") },
                    placeholder = { Text("e.g. North Indian, Tandoori, Mughlai") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Fastfood, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_cuisine")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        errorMessage = null
                    },
                    label = { Text("Outlet Address / Area") },
                    placeholder = { Text("e.g. 100ft Road, Indiranagar, Bengaluru") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_location")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fssaiNumber,
                    onValueChange = { fssaiNumber = it },
                    label = { Text("FSSAI License / Registration No.") },
                    placeholder = { Text("14-digit FSSAI number (optional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = PrimaryOrange)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reg_fssai")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (ownerName.isBlank()) {
                            errorMessage = "Please enter owner name"
                            return@Button
                        }
                        if (restaurantName.isBlank()) {
                            errorMessage = "Please enter restaurant name"
                            return@Button
                        }
                        if (phone.isBlank() || phone.length < 10) {
                            errorMessage = "Please enter a valid 10-digit mobile number"
                            return@Button
                        }
                        if (email.isBlank() || !email.contains("@")) {
                            errorMessage = "Please enter a valid email address"
                            return@Button
                        }

                        val err = viewModel.registerRestaurantOwner(
                            ownerName = ownerName,
                            email = email,
                            phone = phone,
                            password = password.ifBlank { "1234" },
                            restaurantName = restaurantName,
                            businessType = businessType,
                            cuisine = cuisine.ifBlank { if (businessType == BusinessType.FOOD) "Multi-Cuisine" else "Daily Essentials" },
                            location = location.ifBlank { "Indiranagar, Bengaluru" }
                        )

                        if (err != null) {
                            errorMessage = err
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_partner_register_submit")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register & Launch Kitchen Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have a partner account?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Partner Login",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
