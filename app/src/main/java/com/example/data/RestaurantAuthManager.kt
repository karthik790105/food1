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

    // Pre-registered demo accounts for instant testing and existing outlets
    private val _registeredOwners = mutableListOf(
        RestaurantOwner(
            id = "owner_biryani",
            ownerName = "Mohammed Aslam",
            email = "owner@biryani.com",
            phone = "9845012345",
            password = "1234",
            restaurantId = "store_biryani",
            restaurantName = "Royal Dum Biryani House",
            businessType = BusinessType.FOOD,
            cuisine = "Hyderabadi, Dum Biryani, Mughlai",
            location = "100ft Road, Indiranagar, Bengaluru",
            fssaiNumber = "11223344556677",
            gstin = "29ABCDE1234F1Z5"
        ),
        RestaurantOwner(
            id = "owner_burger",
            ownerName = "Ananya Rao",
            email = "owner@burger.com",
            phone = "9845011111",
            password = "1234",
            restaurantId = "store_burger",
            restaurantName = "The Smashed Burger Co.",
            businessType = BusinessType.FOOD,
            cuisine = "Burgers, American, Fast Food",
            location = "Defence Colony, Indiranagar, Bengaluru",
            fssaiNumber = "11223344558899",
            gstin = "29ABCDE5678F1Z2"
        ),
        RestaurantOwner(
            id = "owner_pizza",
            ownerName = "Marco D'Souza",
            email = "owner@pizza.com",
            phone = "9845022222",
            password = "1234",
            restaurantId = "store_pizza",
            restaurantName = "Napoli Stone-Oven Pizzeria",
            businessType = BusinessType.FOOD,
            cuisine = "Italian, Pizzas, Pastas",
            location = "Koramangala 4th Block, Bengaluru",
            fssaiNumber = "11223344557766",
            gstin = "29ABCDE9012F1Z8"
        ),
        RestaurantOwner(
            id = "owner_quickmart",
            ownerName = "Rajesh Sharma",
            email = "owner@bitemart.com",
            phone = "9845033333",
            password = "1234",
            restaurantId = "store_quick_mart",
            restaurantName = "BiteMart Instant 10-Min Store",
            businessType = BusinessType.GROCERY,
            cuisine = "Instant Grocery, Dairy, Fresh Essentials",
            location = "Dark Store Hub 12, Indiranagar, Bengaluru",
            fssaiNumber = "11223344551122",
            gstin = "29ABCDE3456F1Z4"
        )
    )

    private val _currentOwner = MutableStateFlow<RestaurantOwner?>(null)
    val currentOwner: StateFlow<RestaurantOwner?> = _currentOwner.asStateFlow()

    init {
        loadSession()
        if (_currentOwner.value == null && _registeredOwners.isNotEmpty()) {
            val defaultOwner = _registeredOwners.first()
            saveSession(defaultOwner)
            _currentOwner.value = defaultOwner
        }
    }

    private fun loadSession() {
        val wasExplicitLogout = prefs.getBoolean("KEY_EXPLICIT_LOGOUT", false)
        val ownerId = prefs.getString("KEY_OWNER_ID", null)
        if (ownerId != null) {
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
        } else if (!wasExplicitLogout) {
            // First time launch: default to premier restaurant partner
            _registeredOwners.firstOrNull()?.let { defaultOwner ->
                _currentOwner.value = defaultOwner
                saveSession(defaultOwner)
            }
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

        val starterItems = if (businessType == BusinessType.FOOD) {
            listOf(
                MenuItem(
                    id = "item_${newStoreId}_1",
                    storeId = newStoreId,
                    name = "Signature House Special",
                    description = "Chef's signature recipe cooked fresh with authentic herbs and house spices.",
                    price = 249.0,
                    originalPrice = 299.0,
                    imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=80",
                    isVeg = false,
                    category = "Main Course",
                    rating = 4.8,
                    ratingCount = 12,
                    isBestseller = true,
                    unit = "1 portion"
                ),
                MenuItem(
                    id = "item_${newStoreId}_2",
                    storeId = newStoreId,
                    name = "Crispy Appetizer Platter",
                    description = "Golden fried spiced bites served with mint chutney and spicy dip.",
                    price = 149.0,
                    originalPrice = 179.0,
                    imageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=500&auto=format&fit=crop&q=80",
                    isVeg = true,
                    category = "Starters",
                    rating = 4.7,
                    ratingCount = 8,
                    isBestseller = false,
                    unit = "6 pieces"
                ),
                MenuItem(
                    id = "item_${newStoreId}_3",
                    storeId = newStoreId,
                    name = "Chilled Beverage Cooler",
                    description = "Refreshing chilled beverage to pair with your meal.",
                    price = 59.0,
                    originalPrice = null,
                    imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500&auto=format&fit=crop&q=80",
                    isVeg = true,
                    category = "Beverages",
                    rating = 4.9,
                    ratingCount = 20,
                    isBestseller = true,
                    unit = "300 ml"
                )
            )
        } else {
            listOf(
                MenuItem(
                    id = "item_${newStoreId}_1",
                    storeId = newStoreId,
                    name = "Farm Fresh Whole Milk",
                    description = "Pure homogenized and pasteurized fresh dairy milk.",
                    price = 34.0,
                    imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=80",
                    isVeg = true,
                    category = "Dairy & Bread",
                    rating = 4.9,
                    ratingCount = 15,
                    isBestseller = true,
                    unit = "500 ml pouch"
                ),
                MenuItem(
                    id = "item_${newStoreId}_2",
                    storeId = newStoreId,
                    name = "Fresh Organic Bananas",
                    description = "Naturally ripened bananas rich in potassium.",
                    price = 45.0,
                    imageUrl = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=500&auto=format&fit=crop&q=80",
                    isVeg = true,
                    category = "Fresh Produce",
                    rating = 4.8,
                    ratingCount = 18,
                    isBestseller = true,
                    unit = "500g pack"
                )
            )
        }

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
