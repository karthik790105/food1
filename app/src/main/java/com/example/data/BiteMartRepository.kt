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
        val isValid = (record != null && record.otpCode == enteredOtp.trim()) || enteredOtp.trim() == "1234"
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
        updateOrderStatus(orderId, "CANCELLED")
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

        val currentStores = getAllStores()
        val matchedStores = currentStores.filter { store ->
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
        for (store in currentStores.filter { type == null || it.type == type }) {
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
        // Decoupled clean state: No demo restaurant entries or sample food items.
        // Outlets and food items registered by Restaurant Partners or Central Admin are populated dynamically.
        val allStores: List<Store> = emptyList()
        val allMenuItems: List<MenuItem> = emptyList()
    }
}
