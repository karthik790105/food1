package com.example.data

import com.example.data.dao.AddressDao
import com.example.data.dao.CartDao
import com.example.data.dao.FavoriteDao
import com.example.data.dao.OrderDao
import com.example.data.dao.UserDao
import com.example.data.entity.AddressEntity
import com.example.data.entity.CartEntity
import com.example.data.entity.FavoriteEntity
import com.example.data.entity.OrderEntity
import com.example.data.entity.OtpRecordEntity
import com.example.data.entity.UserEntity
import com.example.data.firebase.FirebaseDatabaseService
import com.example.model.BusinessType
import com.example.model.CategoryFilter
import com.example.model.Coupon
import com.example.model.MenuItem
import com.example.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class BiteMartRepository(
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val addressDao: AddressDao,
    private val favoriteDao: FavoriteDao,
    private val userDao: UserDao,
    val firebaseDb: FirebaseDatabaseService? = null
) {
    // Reactive Room streams
    val activeUser: Flow<UserEntity?> = userDao.getActiveUser()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val cartItems: Flow<List<CartEntity>> = cartDao.getAllCartItems()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val activeOrder: Flow<OrderEntity?> = orderDao.getActiveOrder()
    val addresses: Flow<List<AddressEntity>> = addressDao.getAllAddresses()
    val favorites: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    // Authentication & User operations
    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun saveOtp(phone: String, otpCode: String) {
        userDao.saveOtpRecord(OtpRecordEntity(phone = phone, otpCode = otpCode))
    }

    suspend fun getStoredOtp(phone: String): String? {
        return userDao.getOtpRecord(phone)?.otpCode
    }

    suspend fun verifyAndLogin(phone: String, enteredOtp: String, nameIfNew: String = ""): Boolean {
        val record = userDao.getOtpRecord(phone)
        val isValid = record != null && record.otpCode == enteredOtp.trim()
        if (isValid) {
            userDao.logoutAllUsers()
            val existing = userDao.getUserByPhone(phone)
            if (existing != null) {
                userDao.setLoggedIn(phone)
                val updated = if (nameIfNew.isNotBlank() && existing.name != nameIfNew) {
                    existing.copy(name = nameIfNew, isLoggedIn = true)
                } else {
                    existing.copy(isLoggedIn = true)
                }
                userDao.saveUser(updated)
                firebaseDb?.syncUserToCloud(updated)
            } else {
                val displayName = if (nameIfNew.isNotBlank()) nameIfNew else "Customer"
                val newUser = UserEntity(phone = phone, name = displayName, isLoggedIn = true)
                userDao.saveUser(newUser)
                firebaseDb?.syncUserToCloud(newUser)
            }
            userDao.deleteOtpRecord(phone)
            return true
        }
        return false
    }

    suspend fun logout() {
        userDao.logoutAllUsers()
    }


    // Cart operations
    suspend fun addToCart(item: MenuItem, store: Store, quantityDelta: Int = 1) {
        val existing = cartDao.getCartItem(item.id)
        if (existing != null) {
            val newQty = existing.quantity + quantityDelta
            if (newQty <= 0) {
                cartDao.deleteByItemId(item.id)
            } else {
                cartDao.updateItem(existing.copy(quantity = newQty))
            }
        } else if (quantityDelta > 0) {
            cartDao.insertItem(
                CartEntity(
                    itemId = item.id,
                    storeId = store.id,
                    storeName = store.name,
                    storeType = store.type.name,
                    name = item.name,
                    price = item.price,
                    quantity = quantityDelta,
                    isVeg = item.isVeg,
                    imageUrl = item.imageUrl,
                    unit = item.unit
                )
            )
        }
    }

    suspend fun updateCartItemQuantity(itemId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteByItemId(itemId)
        } else {
            val existing = cartDao.getCartItem(itemId)
            if (existing != null) {
                cartDao.updateItem(existing.copy(quantity = quantity))
            }
        }
    }

    suspend fun clearCart() = cartDao.clearCart()

    // Orders operations with Cloud Firestore synchronization across the 4 network apps
    suspend fun insertOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
        firebaseDb?.syncOrderToCloud(order)
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        orderDao.updateStatus(orderId, status)
        val order = orderDao.getOrderById(orderId)
        if (order != null) {
            firebaseDb?.syncOrderToCloud(order.copy(status = status))
        }
    }

    suspend fun assignPartnerToOrder(
        orderId: String,
        status: String,
        partnerName: String,
        partnerPhone: String,
        partnerVehicle: String,
        partnerRating: Double
    ) {
        orderDao.assignPartner(orderId, status, partnerName, partnerPhone, partnerVehicle, partnerRating)
        val order = orderDao.getOrderById(orderId)
        if (order != null) {
            firebaseDb?.syncOrderToCloud(
                order.copy(
                    status = status,
                    partnerName = partnerName,
                    partnerPhone = partnerPhone,
                    partnerVehicle = partnerVehicle,
                    partnerRating = partnerRating
                )
            )
        }
    }

    // Delivery Partner streams and actions
    fun getAvailableOrdersForDelivery(): Flow<List<OrderEntity>> = orderDao.getAvailableOrdersForDelivery()

    fun getActiveDeliveriesForPartner(partnerName: String): Flow<List<OrderEntity>> =
        orderDao.getActiveDeliveriesForPartner(partnerName)

    fun getCompletedDeliveriesForPartner(partnerName: String): Flow<List<OrderEntity>> =
        orderDao.getCompletedDeliveriesForPartner(partnerName)

    suspend fun getOrderById(orderId: String): OrderEntity? = orderDao.getOrderById(orderId)

    // Address operations with Cloud Firestore synchronization
    suspend fun addAddress(address: AddressEntity, userPhone: String = "") {
        addressDao.insertAddress(address)
        firebaseDb?.syncAddressToCloud(address, userPhone)
    }

    suspend fun setDefaultAddress(id: String) {
        addressDao.resetDefaults()
        addressDao.setDefault(id)
    }

    suspend fun deleteAddress(id: String) {
        addressDao.deleteAddress(id)
        firebaseDb?.deleteAddressFromCloud(id)
    }

    // Favorites
    suspend fun toggleFavorite(storeId: String, isFav: Boolean) {
        if (isFav) {
            favoriteDao.removeFavorite(storeId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(storeId = storeId))
        }
    }

    fun isStoreFavorite(storeId: String): Flow<Boolean> = favoriteDao.isFavorite(storeId)

    // Initial Address Seed
    suspend fun seedInitialAddressesIfEmpty() {
        // Will check and insert default address if none exists
    }

    // Restaurant & Store Operations
    fun getOrdersForStore(storeId: String): Flow<List<OrderEntity>> = orderDao.getOrdersForStore(storeId)

    suspend fun updateRestaurantOrderStatus(
        orderId: String,
        status: String,
        partnerName: String? = null,
        partnerPhone: String? = null,
        partnerVehicle: String? = null
    ) {
        if (!partnerName.isNullOrBlank()) {
            orderDao.assignPartner(
                orderId = orderId,
                status = status,
                partnerName = partnerName,
                partnerPhone = partnerPhone ?: "+91 98450 12345",
                partnerVehicle = partnerVehicle ?: "TVS Jupiter • KA 01 EQ 4521",
                partnerRating = 4.9
            )
        } else {
            orderDao.updateStatus(orderId, status)
        }
        firebaseDb?.updateOrderStatusInCloud(orderId, status, partnerName, partnerPhone, partnerVehicle)
    }

    private val _mutableMenuItems = MutableStateFlow<List<MenuItem>>(allMenuItems)
    val menuItemsFlow: StateFlow<List<MenuItem>> = _mutableMenuItems.asStateFlow()

    private val _storeOnlineStatus = MutableStateFlow<Map<String, Boolean>>(
        allStores.associate { it.id to true }
    )
    val storeOnlineStatusFlow: StateFlow<Map<String, Boolean>> = _storeOnlineStatus.asStateFlow()

    private val _suspendedStores = MutableStateFlow<Set<String>>(emptySet())
    val suspendedStores: StateFlow<Set<String>> = _suspendedStores.asStateFlow()

    fun isStoreSuspended(storeId: String): Boolean = _suspendedStores.value.contains(storeId)

    fun toggleStoreSuspension(storeId: String): Boolean {
        val current = _suspendedStores.value.toMutableSet()
        val isNowSuspended = if (current.contains(storeId)) {
            current.remove(storeId)
            false
        } else {
            current.add(storeId)
            true
        }
        _suspendedStores.value = current
        return isNowSuspended
    }

    suspend fun cancelOrder(orderId: String) {
        orderDao.cancelOrder(orderId)
    }

    fun isStoreOnline(storeId: String): Boolean = _storeOnlineStatus.value[storeId] ?: true

    fun toggleStoreOnline(storeId: String): Boolean {
        val current = _storeOnlineStatus.value.toMutableMap()
        val currentVal = current[storeId] ?: true
        val newVal = !currentVal
        current[storeId] = newVal
        _storeOnlineStatus.value = current
        return newVal
    }

    fun toggleItemStock(itemId: String): Boolean {
        val current = _mutableMenuItems.value
        val updated = current.map { item ->
            if (item.id == itemId) item.copy(inStock = !item.inStock) else item
        }
        _mutableMenuItems.value = updated
        return updated.find { it.id == itemId }?.inStock ?: true
    }

    fun updateItemPrice(itemId: String, newPrice: Double) {
        val current = _mutableMenuItems.value
        _mutableMenuItems.value = current.map { item ->
            if (item.id == itemId) item.copy(price = newPrice) else item
        }
    }

    private val _customStores = MutableStateFlow<List<Store>>(emptyList())
    val allStoresFlow: Flow<List<Store>> = _customStores.map { allStores + it }

    fun getAllStores(): List<Store> = allStores + _customStores.value

    fun registerNewStore(store: Store, starterItems: List<MenuItem> = emptyList()) {
        val current = _customStores.value
        if (current.none { it.id == store.id } && allStores.none { it.id == store.id }) {
            _customStores.value = current + store
            val onlineMap = _storeOnlineStatus.value.toMutableMap()
            onlineMap[store.id] = true
            _storeOnlineStatus.value = onlineMap
            if (starterItems.isNotEmpty()) {
                _mutableMenuItems.value = _mutableMenuItems.value + starterItems
            }
        }
    }

    fun deleteMenuItem(itemId: String) {
        _mutableMenuItems.value = _mutableMenuItems.value.filter { it.id != itemId }
    }

    fun addMenuItem(item: MenuItem) {
        _mutableMenuItems.value = _mutableMenuItems.value + item
    }

    // Static Catalog Data
    fun getStores(type: BusinessType? = null): List<Store> {
        val list = getAllStores()
        return if (type == null) list else list.filter { it.type == type }
    }

    fun getStoreById(id: String): Store? = getAllStores().find { it.id == id }

    fun getMenuItemsForStore(storeId: String): List<MenuItem> =
        _mutableMenuItems.value.filter { it.storeId == storeId }

    fun searchStoresAndItems(query: String, type: BusinessType? = null): List<Pair<Store, List<MenuItem>>> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val matchedStores = allStores.filter { store ->
            (type == null || store.type == type) && (
                store.name.lowercase().contains(q) ||
                store.cuisines.any { it.lowercase().contains(q) } ||
                store.tagline.lowercase().contains(q)
            )
        }

        val results = mutableListOf<Pair<Store, List<MenuItem>>>()
        for (store in matchedStores) {
            results.add(Pair(store, getMenuItemsForStore(store.id)))
        }

        // Also check if any individual menu items match that belong to other stores
        for (store in allStores.filter { type == null || it.type == type }) {
            if (matchedStores.none { it.id == store.id }) {
                val matchingItems = _mutableMenuItems.value.filter {
                    it.storeId == store.id && (
                        it.name.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
                    )
                }
                if (matchingItems.isNotEmpty()) {
                    results.add(Pair(store, matchingItems))
                }
            }
        }
        return results
    }

    val availableCoupons: List<Coupon> = emptyList()

    val foodCategories: List<CategoryFilter> = listOf(
        CategoryFilter("all", "All Cuisines", BusinessType.FOOD, "restaurant"),
        CategoryFilter("biryani", "Biryani & Bowls", BusinessType.FOOD, "rice_bowl"),
        CategoryFilter("burger", "Burgers & Fries", BusinessType.FOOD, "lunch_dining"),
        CategoryFilter("pizza", "Pizzas & Pasta", BusinessType.FOOD, "local_pizza"),
        CategoryFilter("healthy", "Salads & Vegan", BusinessType.FOOD, "eco"),
        CategoryFilter("asian", "Asian & Noodles", BusinessType.FOOD, "ramen_dining"),
        CategoryFilter("dessert", "Desserts & Shakes", BusinessType.FOOD, "icecream")
    )

    val groceryCategories: List<CategoryFilter> = listOf(
        CategoryFilter("all_g", "All Aisles", BusinessType.GROCERY, "storefront"),
        CategoryFilter("fruits_veg", "Fresh Veggies & Fruits", BusinessType.GROCERY, "spa"),
        CategoryFilter("dairy", "Dairy, Bread & Eggs", BusinessType.GROCERY, "egg"),
        CategoryFilter("beverages", "Drinks & Juices", BusinessType.GROCERY, "local_cafe"),
        CategoryFilter("snacks", "Snacks & Munchies", BusinessType.GROCERY, "cookie"),
        CategoryFilter("staples", "Atta, Rice & Dal", BusinessType.GROCERY, "grain"),
        CategoryFilter("instant", "Instant 10-Min Food", BusinessType.GROCERY, "bolt")
    )

    companion object {
        val allStores = listOf(
            // Food Stores
            Store(
                id = "store_biryani",
                name = "Royal Dum Biryani House",
                type = BusinessType.FOOD,
                tagline = "Authentic slow-cooked Dum Biryanis & Kebabs",
                rating = 4.7,
                ratingCount = 1840,
                deliveryTimeMin = 22,
                distanceKm = 1.8,
                deliveryFee = 35.0,
                imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Hyderabadi", "Biryani", "Mughlai", "North Indian"),
                isPromoted = true,
                discountText = null,
                location = "100ft Road, Indiranagar"
            ),
            Store(
                id = "store_burger",
                name = "The Smashed Burger Co.",
                type = BusinessType.FOOD,
                tagline = "Crispy edges, brioche buns & thick milkshakes",
                rating = 4.5,
                ratingCount = 920,
                deliveryTimeMin = 18,
                distanceKm = 1.2,
                deliveryFee = 30.0,
                imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Burgers", "American", "Fast Food", "Fries"),
                isPromoted = false,
                discountText = null,
                location = "Defence Colony, Indiranagar"
            ),
            Store(
                id = "store_pizza",
                name = "Napoli Stone-Oven Pizzeria",
                type = BusinessType.FOOD,
                tagline = "Slow fermented sourdough crust & San Marzano tomatoes",
                rating = 4.8,
                ratingCount = 2650,
                deliveryTimeMin = 26,
                distanceKm = 2.4,
                deliveryFee = 40.0,
                imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Italian", "Pizzas", "Pastas", "Garlic Bread"),
                isPromoted = true,
                discountText = null,
                location = "Koramangala 4th Block"
            ),
            Store(
                id = "store_healthy",
                name = "Green Bowl Healthy Kitchen",
                type = BusinessType.FOOD,
                tagline = "Macro-balanced nutrient bowls, fresh juices & wraps",
                rating = 4.6,
                ratingCount = 640,
                deliveryTimeMin = 19,
                distanceKm = 1.5,
                deliveryFee = 25.0,
                imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Healthy", "Salads", "Bowls", "Smoothies"),
                isVegOnly = true,
                discountText = null,
                location = "Halasuru, Bangalore"
            ),
            Store(
                id = "store_asian",
                name = "Wok & Lantern Asian Diner",
                type = BusinessType.FOOD,
                tagline = "Steaming Dimsums, Hakka Noodles & Thai Curries",
                rating = 4.4,
                ratingCount = 1120,
                deliveryTimeMin = 24,
                distanceKm = 2.1,
                deliveryFee = 35.0,
                imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Pan-Asian", "Chinese", "Dimsums", "Thai"),
                isPromoted = false,
                discountText = null,
                location = "CMH Road, Indiranagar"
            ),

            // Grocery & Instant Mart Stores
            Store(
                id = "store_quick_mart",
                name = "BiteMart Instant 10-Min Store",
                type = BusinessType.GROCERY,
                tagline = "Lightning fast delivery of milk, veggies, ice cream & snacks",
                rating = 4.9,
                ratingCount = 5400,
                deliveryTimeMin = 10,
                distanceKm = 0.6,
                deliveryFee = 15.0,
                imageUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Instant 10-Min", "Dairy", "Snacks", "Cold Drinks"),
                isPromoted = true,
                discountText = null,
                location = "Dark Store Hub 12, Indiranagar"
            ),
            Store(
                id = "store_fresh_produce",
                name = "FarmDirect Organic Greens & Fruits",
                type = BusinessType.GROCERY,
                tagline = "Harvested this morning from local regenerative farms",
                rating = 4.7,
                ratingCount = 1950,
                deliveryTimeMin = 15,
                distanceKm = 1.3,
                deliveryFee = 25.0,
                imageUrl = "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Organic Produce", "Exotic Fruits", "Fresh Veggies", "Herbs"),
                isVegOnly = true,
                discountText = null,
                location = "Domlur Layout"
            ),
            Store(
                id = "store_artisan_bakery",
                name = "Artisan Bakery & Daily Dairy",
                type = BusinessType.GROCERY,
                tagline = "Freshly baked sourdough, whole milk, farm eggs & butter",
                rating = 4.8,
                ratingCount = 1420,
                deliveryTimeMin = 14,
                distanceKm = 1.1,
                deliveryFee = 20.0,
                imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=700&auto=format&fit=crop&q=80",
                cuisines = listOf("Bakery", "Artisan Bread", "Dairy & Eggs", "Gourmet Spreads"),
                discountText = null,
                location = "Old Airport Road"
            )
        )

        val allMenuItems = listOf(
            // Royal Dum Biryani House Items
            MenuItem(
                id = "biryani_1",
                storeId = "store_biryani",
                name = "Hyderabadi Chicken Dum Biryani",
                description = "Fragrant long-grain basmati rice layered with spiced chicken, caramelized onions & saffron. Served with spicy mirchi ka salan and cooling raita.",
                price = 299.0,
                originalPrice = 349.0,
                imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop&q=80",
                isVeg = false,
                category = "Biryani & Bowls",
                rating = 4.8,
                ratingCount = 980,
                isBestseller = true,
                unit = "Serves 1-2"
            ),
            MenuItem(
                id = "biryani_2",
                storeId = "store_biryani",
                name = "Paneer Tikka Subz Biryani",
                description = "Charcoal grilled paneer cubes and garden vegetables spiced with royal shahi garam masala, layered over aromatic saffron basmati rice.",
                price = 249.0,
                originalPrice = 289.0,
                imageUrl = "https://images.unsplash.com/photo-1645177628172-a94c1f96e6db?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Biryani & Bowls",
                rating = 4.6,
                ratingCount = 420,
                isBestseller = false,
                unit = "Serves 1-2"
            ),
            MenuItem(
                id = "biryani_3",
                storeId = "store_biryani",
                name = "Tandoori Chicken Wings (6 pcs)",
                description = "Smoky clay-oven roasted chicken wings marinated in hung yogurt, Kashmiri chili and aromatic whole spices.",
                price = 199.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=500&auto=format&fit=crop&q=80",
                isVeg = false,
                category = "Appetizers & Starters",
                rating = 4.7,
                ratingCount = 610,
                isBestseller = true,
                unit = "6 pieces"
            ),
            MenuItem(
                id = "biryani_4",
                storeId = "store_biryani",
                name = "Gulab Jamun with Rabri (2 pcs)",
                description = "Warm golden milk dough balls soaked in cardamom saffron syrup, topped with rich pistachio rabri.",
                price = 99.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Desserts",
                rating = 4.9,
                ratingCount = 890,
                isBestseller = true,
                unit = "2 pieces"
            ),

            // Smashed Burger Co. Items
            MenuItem(
                id = "burger_1",
                storeId = "store_burger",
                name = "Double Truffle Smash Cheeseburger",
                description = "Two crispy smash patties, melted aged cheddar, caramelized balsamic onions, and house truffle aioli on a toasted potato bun.",
                price = 219.0,
                originalPrice = 259.0,
                imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=80",
                isVeg = false,
                category = "Burgers",
                rating = 4.8,
                ratingCount = 740,
                isBestseller = true,
                unit = "1 burger"
            ),
            MenuItem(
                id = "burger_2",
                storeId = "store_burger",
                name = "Crispy Paneer & Jalapeño Burger",
                description = "Panko-crusted spiced cottage cheese patty, smoky chipotle mayo, crunchy lettuce, dill pickles and molten cheese.",
                price = 179.0,
                originalPrice = 209.0,
                imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Burgers",
                rating = 4.5,
                ratingCount = 380,
                isBestseller = false,
                unit = "1 burger"
            ),
            MenuItem(
                id = "burger_3",
                storeId = "store_burger",
                name = "Peri-Peri Seasoned Fries",
                description = "Crispy golden skin-on fries tossed in zesty African bird's eye chili seasoning with garlic herb dip.",
                price = 99.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Sides",
                rating = 4.6,
                ratingCount = 520,
                isBestseller = true,
                unit = "Large portion"
            ),
            MenuItem(
                id = "burger_4",
                storeId = "store_burger",
                name = "Belgian Chocolate Thickshake",
                description = "Hand-spun milkshake with Belgian dark chocolate gelato, crunchy brownie chunks, and whipped cream.",
                price = 149.0,
                originalPrice = 179.0,
                imageUrl = "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Beverages",
                rating = 4.9,
                ratingCount = 490,
                isBestseller = true,
                unit = "400 ml"
            ),

            // Napoli Stone-Oven Pizzeria
            MenuItem(
                id = "pizza_1",
                storeId = "store_pizza",
                name = "Classic Margherita D.O.P.",
                description = "Crushed San Marzano tomatoes, fresh buffalo mozzarella, fresh sweet basil leaves, and extra virgin olive oil drizzle on charred crust.",
                price = 329.0,
                originalPrice = 379.0,
                imageUrl = "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Pizzas",
                rating = 4.9,
                ratingCount = 1350,
                isBestseller = true,
                unit = "11 inch"
            ),
            MenuItem(
                id = "pizza_2",
                storeId = "store_pizza",
                name = "Pepperoni & Hot Honey Sourdough",
                description = "Loaded with spiced pepperoni, fresh mozzarella, oregano, and drizzled with habanero-infused warm hot honey.",
                price = 399.0,
                originalPrice = 449.0,
                imageUrl = "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=500&auto=format&fit=crop&q=80",
                isVeg = false,
                category = "Pizzas",
                rating = 4.8,
                ratingCount = 920,
                isBestseller = true,
                unit = "11 inch"
            ),
            MenuItem(
                id = "pizza_3",
                storeId = "store_pizza",
                name = "Cheesy Garlic Herb Pull-Apart Bread",
                description = "Garlic butter infused pull-apart loaf baked with roasted garlic cloves and melted mozzarella cheese.",
                price = 149.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1619895092538-128341789043?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Sides",
                rating = 4.7,
                ratingCount = 570,
                isBestseller = false,
                unit = "4 slices"
            ),

            // Green Bowl Healthy Kitchen
            MenuItem(
                id = "healthy_1",
                storeId = "store_healthy",
                name = "Avocado Quinoa Super Bowl",
                description = "Tri-color fluffy quinoa, sliced ripe Hass avocado, roasted cherry tomatoes, edamame, and lemon tahini vinaigrette.",
                price = 229.0,
                originalPrice = 269.0,
                imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Bowls & Salads",
                rating = 4.7,
                ratingCount = 310,
                isBestseller = true,
                unit = "350 kcal bowl"
            ),
            MenuItem(
                id = "healthy_2",
                storeId = "store_healthy",
                name = "Berry Bliss Cold-Pressed Smoothie",
                description = "Blueberries, strawberries, Greek yogurt, chia seeds, and coconut water with zero added sugar.",
                price = 139.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Beverages",
                rating = 4.8,
                ratingCount = 280,
                isBestseller = false,
                unit = "300 ml bottle"
            ),

            // Grocery: BiteMart Instant 10-Min Store
            MenuItem(
                id = "groc_1",
                storeId = "store_quick_mart",
                name = "Amul Taaza Homogenised Toned Milk",
                description = "Pasteurized & homogenized toned milk, rich in calcium and protein. Sealed for freshness.",
                price = 32.0,
                originalPrice = 35.0,
                imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Dairy & Eggs",
                rating = 4.9,
                ratingCount = 3100,
                isBestseller = true,
                unit = "1 Litre pouch"
            ),
            MenuItem(
                id = "groc_2",
                storeId = "store_quick_mart",
                name = "Farm Fresh Brown Eggs (Pack of 6)",
                description = "Farm-fresh antibiotic-free brown eggs, high protein and natural yolk color.",
                price = 58.0,
                originalPrice = 65.0,
                imageUrl = "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=500&auto=format&fit=crop&q=80",
                isVeg = false,
                category = "Dairy & Eggs",
                rating = 4.8,
                ratingCount = 1890,
                isBestseller = true,
                unit = "6 eggs pack"
            ),
            MenuItem(
                id = "groc_3",
                storeId = "store_quick_mart",
                name = "Lay's Spanish Tomato Tango Chips",
                description = "Crispy potato wafers with tangy tomato seasoning and savory spices.",
                price = 20.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Snacks & Munchies",
                rating = 4.7,
                ratingCount = 2400,
                isBestseller = true,
                unit = "90g pouch"
            ),
            MenuItem(
                id = "groc_4",
                storeId = "store_quick_mart",
                name = "Maggi 2-Minute Masala Noodles",
                description = "India's favorite instant noodles made with selected spices for the signature masala taste.",
                price = 56.0,
                originalPrice = 60.0,
                imageUrl = "https://images.unsplash.com/photo-1612927601601-6638404737ce?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Instant 10-Min Food",
                rating = 4.9,
                ratingCount = 4200,
                isBestseller = true,
                unit = "4-pack (280g)"
            ),
            MenuItem(
                id = "groc_5",
                storeId = "store_quick_mart",
                name = "Coca-Cola Zero Sugar Can",
                description = "Original refreshing Coca-Cola taste with zero sugar and zero calories.",
                price = 40.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Drinks & Juices",
                rating = 4.8,
                ratingCount = 1750,
                isBestseller = false,
                unit = "330 ml can"
            ),

            // Grocery: FarmDirect Organic Greens
            MenuItem(
                id = "groc_6",
                storeId = "store_fresh_produce",
                name = "Fresh Crisp Hybrid Tomatoes",
                description = "Plump, red vine-ripened tomatoes directly sourced from organic polyhouses.",
                price = 38.0,
                originalPrice = 45.0,
                imageUrl = "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Fresh Veggies & Fruits",
                rating = 4.8,
                ratingCount = 890,
                isBestseller = true,
                unit = "1 kg"
            ),
            MenuItem(
                id = "groc_7",
                storeId = "store_fresh_produce",
                name = "Imported Hass Avocados (Pack of 2)",
                description = "Nutty creamy Hass avocados, ideal for guacamole, keto salads and breakfast toast.",
                price = 149.0,
                originalPrice = 180.0,
                imageUrl = "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Fresh Veggies & Fruits",
                rating = 4.9,
                ratingCount = 1120,
                isBestseller = true,
                unit = "2 pcs (approx 350g)"
            ),
            MenuItem(
                id = "groc_8",
                storeId = "store_fresh_produce",
                name = "Baby Spinach & Rocket Salad Leaves",
                description = "Tender hydroponic baby spinach and peppery wild rocket leaves, triple washed and ready to eat.",
                price = 49.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Fresh Veggies & Fruits",
                rating = 4.7,
                ratingCount = 430,
                isBestseller = false,
                unit = "200g box"
            ),

            // Grocery: Artisan Bakery
            MenuItem(
                id = "groc_9",
                storeId = "store_artisan_bakery",
                name = "San Francisco Sourdough Loaf",
                description = "Traditional 48-hour naturally fermented sourdough with a blistered golden crust and chewy crumb.",
                price = 99.0,
                originalPrice = 119.0,
                imageUrl = "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Bakery & Bread",
                rating = 4.9,
                ratingCount = 680,
                isBestseller = true,
                unit = "450g loaf"
            ),
            MenuItem(
                id = "groc_10",
                storeId = "store_artisan_bakery",
                name = "Butter Croissant (2 pcs)",
                description = "Flaky, layered French butter croissants baked fresh every 3 hours.",
                price = 89.0,
                originalPrice = null,
                imageUrl = "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500&auto=format&fit=crop&q=80",
                isVeg = true,
                category = "Bakery & Bread",
                rating = 4.8,
                ratingCount = 590,
                isBestseller = true,
                unit = "2 pieces"
            )
        )
    }
}
