package com.example.ui.restaurant

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessType
import com.example.ui.theme.GroceryGreen
import com.example.ui.theme.PrimaryOrange
import com.example.viewmodel.RestaurantPartnerViewModel

@Composable
fun RestaurantAuthScreen(
    viewModel: RestaurantPartnerViewModel,
    modifier: Modifier = Modifier
) {
    var isRegisterTab by remember { mutableStateOf(false) }
    var globalError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("restaurant_auth_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Hero Header matching Customer App style
            RestaurantHeroHeader(isRegister = isRegisterTab)

            // Animated Error Banner
            AnimatedVisibility(visible = !globalError.isNullOrBlank()) {
                globalError?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Segmented Pill Tab Switcher
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isRegisterTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (!isRegisterTab) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRegisterTab = false
                                globalError = null
                            }
                            .testTag("tab_partner_login")
                    ) {
                        Text(
                            text = "Merchant Login",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (!isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRegisterTab) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRegisterTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isRegisterTab) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRegisterTab = true
                                globalError = null
                            }
                            .testTag("tab_partner_register")
                    ) {
                        Text(
                            text = "Register Restaurant",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRegisterTab) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            // Forms
            if (!isRegisterTab) {
                PartnerLoginForm(
                    viewModel = viewModel,
                    onError = { globalError = it },
                    onNavigateToRegister = {
                        isRegisterTab = true
                        globalError = null
                    }
                )
            } else {
                PartnerRegisterForm(
                    viewModel = viewModel,
                    onError = { globalError = it },
                    onNavigateToLogin = {
                        isRegisterTab = false
                        globalError = null
                    }
                )
            }

            // Security Trust Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% Encrypted & Authenticated Merchant Session",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RestaurantHeroHeader(isRegister: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryOrange, Color(0xFFFF8F00))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "BiteMart Partner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = PrimaryOrange
        )
        Text(
            text = if (isRegister) "Register Your Restaurant Outlet" else "Kitchen & Merchant Operations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isRegister)
                "Join BiteMart to accept orders, manage your catalog & track live revenue"
            else
                "Log in to manage live orders, kitchen display system & dish availability",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun PartnerLoginForm(
    viewModel: RestaurantPartnerViewModel,
    onError: (String?) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Sign in to Your Outlet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter your registered merchant email or 10-digit mobile number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    onError(null)
                },
                label = { Text("Email or 10-Digit Mobile") },
                placeholder = { Text("e.g. owner@biryani.com or 9845012345") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_partner_id")
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onError(null)
                },
                label = { Text("Password / PIN") },
                placeholder = { Text("Enter your password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PrimaryOrange)
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_partner_password")
            )

            Button(
                onClick = {
                    if (identifier.isBlank()) {
                        onError("Please enter your registered email or phone number")
                        return@Button
                    }
                    if (password.isBlank()) {
                        onError("Please enter your password")
                        return@Button
                    }
                    val err = viewModel.loginRestaurantOwner(identifier.trim(), password.trim())
                    onError(err)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_partner_login_submit")
            ) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In to Restaurant Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // Quick Select Registered Outlets for instant demonstration
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick 1-Tap Outlets Login",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap any registered outlet to sign in directly:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val sampleOutlets = listOf(
                Triple("Royal Dum Biryani House", "owner@biryani.com", "🍗 Hyderabadi Dum Biryani"),
                Triple("Napoli Stone-Oven Pizzeria", "owner@pizza.com", "🍕 Woodfired Pizza & Pasta"),
                Triple("The Smashed Burger Co.", "owner@burger.com", "🍔 Gourmet Burgers & Fries"),
                Triple("BiteMart 10-Min Mart", "owner@bitemart.com", "🛒 Instant Daily Essentials")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sampleOutlets.forEach { (name, email, cuisine) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                identifier = email
                                password = "1234"
                                val err = viewModel.loginRestaurantOwner(email, "1234")
                                onError(err)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$email • $cuisine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Log in as $name",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

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
}

@Composable
private fun PartnerRegisterForm(
    viewModel: RestaurantPartnerViewModel,
    onError: (String?) -> Unit,
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

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Onboard Your Restaurant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your merchant account. Once onboarded, only you can manage your menu catalog and view customer orders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section 1: Owner Details
            Text(
                text = "1. Owner Credentials",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = {
                    ownerName = it
                    onError(null)
                },
                label = { Text("Owner Full Name *") },
                placeholder = { Text("e.g. Vikram Malhotra") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryOrange)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_owner_name")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onError(null)
                    },
                    label = { Text("Email *") },
                    placeholder = { Text("partner@store.com") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        focusedLabelColor = PrimaryOrange
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reg_email")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        onError(null)
                    },
                    label = { Text("Mobile *") },
                    placeholder = { Text("9876543210") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = PrimaryOrange)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        focusedLabelColor = PrimaryOrange
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reg_phone")
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onError(null)
                },
                label = { Text("Portal Password / PIN *") },
                placeholder = { Text("Create a secure password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = PrimaryOrange)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_password")
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))

            // Section 2: Restaurant Details
            Text(
                text = "2. Restaurant Outlet Details",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )

            OutlinedTextField(
                value = restaurantName,
                onValueChange = {
                    restaurantName = it
                    onError(null)
                },
                label = { Text("Restaurant / Outlet Name *") },
                placeholder = { Text("e.g. Punjab Grill & Tandoor") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = PrimaryOrange)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_restaurant_name")
            )

            // Business Type Selection
            Text(
                text = "Store Category",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
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

            OutlinedTextField(
                value = cuisine,
                onValueChange = {
                    cuisine = it
                    onError(null)
                },
                label = { Text("Cuisine / Speciality") },
                placeholder = { Text("e.g. North Indian, Tandoori, Mughlai") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Fastfood, contentDescription = null, tint = PrimaryOrange)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_cuisine")
            )

            OutlinedTextField(
                value = location,
                onValueChange = {
                    location = it
                    onError(null)
                },
                label = { Text("Outlet Address / Area") },
                placeholder = { Text("e.g. 100ft Road, Indiranagar, Bengaluru") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_location")
            )

            OutlinedTextField(
                value = fssaiNumber,
                onValueChange = { fssaiNumber = it },
                label = { Text("FSSAI License / Registration No. (Optional)") },
                placeholder = { Text("14-digit FSSAI number") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = PrimaryOrange)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reg_fssai")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (ownerName.isBlank()) {
                        onError("Please enter owner name")
                        return@Button
                    }
                    if (restaurantName.isBlank()) {
                        onError("Please enter restaurant name")
                        return@Button
                    }
                    if (phone.isBlank() || phone.length < 10) {
                        onError("Please enter a valid 10-digit mobile number")
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        onError("Please enter a valid email address")
                        return@Button
                    }
                    if (password.isBlank() || password.length < 4) {
                        onError("Please enter a password with at least 4 characters")
                        return@Button
                    }

                    val err = viewModel.registerRestaurantOwner(
                        ownerName = ownerName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        password = password.trim(),
                        restaurantName = restaurantName.trim(),
                        businessType = businessType,
                        cuisine = cuisine.ifBlank { if (businessType == BusinessType.FOOD) "Multi-Cuisine" else "Daily Essentials" },
                        location = location.ifBlank { "Indiranagar, Bengaluru" }
                    )

                    onError(err)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_partner_register_submit")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register & Launch Kitchen Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

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
}
