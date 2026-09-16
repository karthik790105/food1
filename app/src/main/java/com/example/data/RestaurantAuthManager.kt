package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BusinessType
import com.example.model.MenuItem
import com.example.model.RestaurantOwner
import com.example.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages restaurant partner authentication, registration of new restaurant outlets,
 * and session state for the restaurant owner portal.
 */
class RestaurantAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("restaurant_partner_auth_prefs", Context.MODE_PRIVATE)

    // Standalone clean state: No demo accounts pre-registered.
    // Restaurant partners register fresh accounts and create their real outlets.
    private val _registeredOwners = mutableListOf<RestaurantOwner>()

    private val _currentOwner = MutableStateFlow<RestaurantOwner?>(null)
    val currentOwner: StateFlow<RestaurantOwner?> = _currentOwner.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val wasExplicitLogout = prefs.getBoolean("KEY_EXPLICIT_LOGOUT", false)
        val ownerId = prefs.getString("KEY_OWNER_ID", null)
        if (ownerId != null && !wasExplicitLogout) {
            val owner = _registeredOwners.find { it.id == ownerId } ?: RestaurantOwner(
                id = ownerId,
                ownerName = prefs.getString("KEY_OWNER_NAME", "Partner Owner") ?: "Partner Owner",
                email = prefs.getString("KEY_OWNER_EMAIL", "") ?: "",
                phone = prefs.getString("KEY_OWNER_PHONE", "") ?: "",
                password = "",
                restaurantId = prefs.getString("KEY_RESTAURANT_ID", "store_biryani") ?: "store_biryani",
                restaurantName = prefs.getString("KEY_RESTAURANT_NAME", "My Restaurant") ?: "My Restaurant",
                businessType = try {
                    BusinessType.valueOf(prefs.getString("KEY_BUSINESS_TYPE", "FOOD") ?: "FOOD")
                } catch (e: Exception) {
                    BusinessType.FOOD
                },
                cuisine = prefs.getString("KEY_CUISINE", "Multi-Cuisine") ?: "Multi-Cuisine",
                location = prefs.getString("KEY_LOCATION", "Indiranagar, Bengaluru") ?: "Indiranagar, Bengaluru",
                fssaiNumber = prefs.getString("KEY_FSSAI", "11223344556677") ?: "11223344556677",
                gstin = prefs.getString("KEY_GSTIN", "29ABCDE1234F1Z5") ?: "29ABCDE1234F1Z5"
            )
            if (_registeredOwners.none { it.id == owner.id }) {
                _registeredOwners.add(owner)
            }
            _currentOwner.value = owner
        } else {
            // Initially unauthenticated: User must register or login
            _currentOwner.value = null
        }
    }

    fun login(identifier: String, pass: String): Result<RestaurantOwner> {
        val cleanId = identifier.trim().lowercase()
        val digitsOnly = identifier.filter { it.isDigit() }
        val found = _registeredOwners.find { owner ->
            owner.email.lowercase() == cleanId ||
            (digitsOnly.isNotBlank() && owner.phone.filter { it.isDigit() }.endsWith(digitsOnly))
        }

        if (found == null) {
            return Result.failure(Exception("No registered restaurant found for '$identifier'. Please register your outlet."))
        }

        if (found.password.isNotBlank() && found.password != pass) {
            return Result.failure(Exception("Invalid password. Please check your credentials."))
        }

        saveSession(found)
        _currentOwner.value = found
        return Result.success(found)
    }

    fun register(
        ownerName: String,
        email: String,
        phone: String,
        password: String,
        restaurantName: String,
        businessType: BusinessType,
        cuisine: String,
        location: String,
        fssaiNumber: String = "11223344556677",
        gstin: String = "29ABCDE1234F1Z5"
    ): Result<Pair<RestaurantOwner, Pair<Store, List<MenuItem>>>> {
        if (ownerName.isBlank()) return Result.failure(Exception("Please enter owner name."))
        if (restaurantName.isBlank()) return Result.failure(Exception("Please enter restaurant name."))
        if (phone.isBlank() || phone.length < 10) return Result.failure(Exception("Please enter a valid 10-digit mobile number."))
        if (email.isBlank() || !email.contains("@")) return Result.failure(Exception("Please enter a valid email address."))

        val newStoreId = "store_custom_${UUID.randomUUID().toString().take(6)}"
        val newOwnerId = "owner_${UUID.randomUUID().toString().take(6)}"

        val newStore = Store(
            id = newStoreId,
            name = restaurantName.trim(),
            type = businessType,
            tagline = if (businessType == BusinessType.FOOD) "Freshly prepared delicacies by ${restaurantName.trim()}" else "Fast daily essentials from ${restaurantName.trim()}",
            rating = 4.8,
            ratingCount = 1,
            deliveryTimeMin = 20,
            distanceKm = 1.2,
            deliveryFee = 25.0,
            imageUrl = if (businessType == BusinessType.FOOD) {
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=700&auto=format&fit=crop&q=80"
            } else {
                "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=700&auto=format&fit=crop&q=80"
            },
            cuisines = cuisine.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Popular") },
            location = location.ifBlank { "Indiranagar, Bengaluru" }
        )

        // Clean standalone state: No sample food items. Restaurant owners add their actual menu items.
        val starterItems: List<MenuItem> = emptyList()

        val newOwner = RestaurantOwner(
            id = newOwnerId,
            ownerName = ownerName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            password = password.trim(),
            restaurantId = newStoreId,
            restaurantName = restaurantName.trim(),
            businessType = businessType,
            cuisine = cuisine.ifBlank { "Multi-Cuisine" },
            location = location.ifBlank { "Indiranagar, Bengaluru" },
            fssaiNumber = fssaiNumber.ifBlank { "1122334455${(1000..9999).random()}" },
            gstin = gstin.ifBlank { "29ABCDE${(1000..9999).random()}F1Z" }
        )

        _registeredOwners.add(newOwner)
        saveSession(newOwner)
        _currentOwner.value = newOwner

        return Result.success(Pair(newOwner, Pair(newStore, starterItems)))
    }

    fun logout() {
        prefs.edit().clear().putBoolean("KEY_EXPLICIT_LOGOUT", true).apply()
        _currentOwner.value = null
    }

    private fun saveSession(owner: RestaurantOwner) {
        prefs.edit()
            .putBoolean("KEY_EXPLICIT_LOGOUT", false)
            .putString("KEY_OWNER_ID", owner.id)
            .putString("KEY_OWNER_NAME", owner.ownerName)
            .putString("KEY_OWNER_EMAIL", owner.email)
            .putString("KEY_OWNER_PHONE", owner.phone)
            .putString("KEY_RESTAURANT_ID", owner.restaurantId)
            .putString("KEY_RESTAURANT_NAME", owner.restaurantName)
            .putString("KEY_BUSINESS_TYPE", owner.businessType.name)
            .putString("KEY_CUISINE", owner.cuisine)
            .putString("KEY_LOCATION", owner.location)
            .putString("KEY_FSSAI", owner.fssaiNumber)
            .putString("KEY_GSTIN", owner.gstin)
            .apply()
    }

    fun getDemoAccounts(): List<RestaurantOwner> = _registeredOwners.toList()
}
