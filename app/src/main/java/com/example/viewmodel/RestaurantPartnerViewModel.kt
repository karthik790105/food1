package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BiteMartDatabase
import com.example.data.BiteMartRepository
import com.example.data.RestaurantAuthManager
import com.example.data.entity.OrderEntity
import com.example.data.firebase.FirebaseDatabaseService
import com.example.model.BusinessType
import com.example.model.MenuItem
import com.example.model.OrderStatus
import com.example.model.RestaurantOwner
import com.example.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class RestaurantNav {
    LIVE_ORDERS,
    MENU_CATALOG,
    ANALYTICS,
    SETTINGS
}

enum class RestaurantOrderTab(val label: String) {
    NEW("New Orders"),
    PREPARING("Preparing"),
    READY("Ready for Pickup"),
    COMPLETED("Completed")
}

data class RestaurantStats(
    val todaysRevenue: Double = 0.0,
    val todaysOrdersCount: Int = 0,
    val activeOrdersCount: Int = 0,
    val avgOrderValue: Double = 0.0,
    val acceptanceRatePercent: Int = 98,
    val avgPrepMinutes: Int = 18
)

class RestaurantPartnerViewModel(application: Application) : AndroidViewModel(application) {

    val firebaseDb = FirebaseDatabaseService(application)
    private val db = BiteMartDatabase.getInstance(application)
    val repository = BiteMartRepository(
        cartDao = db.cartDao(),
        orderDao = db.orderDao(),
        addressDao = db.addressDao(),
        favoriteDao = db.favoriteDao(),
        userDao = db.userDao(),
        firebaseDb = firebaseDb
    )

    val authManager = RestaurantAuthManager(application)
    val currentOwner: StateFlow<RestaurantOwner?> = authManager.currentOwner

    // All registered & built-in restaurants and outlets available for switching
    val availableStores: List<Store>
        get() = repository.getAllStores()

    // Active store strictly bound to the logged-in restaurant owner
    private val _activeStore = MutableStateFlow<Store>(
        repository.getStoreById("store_biryani") ?: BiteMartRepository.allStores.first()
    )
    val activeStore: StateFlow<Store> = _activeStore.asStateFlow()

    // Navigation & Tab state
    private val _currentNav = MutableStateFlow(RestaurantNav.LIVE_ORDERS)
    val currentNav: StateFlow<RestaurantNav> = _currentNav.asStateFlow()

    private val _selectedOrderTab = MutableStateFlow(RestaurantOrderTab.NEW)
    val selectedOrderTab: StateFlow<RestaurantOrderTab> = _selectedOrderTab.asStateFlow()

    // Store Online / Accepting Orders status
    private val _isStoreOnline = MutableStateFlow(true)
    val isStoreOnline: StateFlow<Boolean> = _isStoreOnline.asStateFlow()

    // Live Orders for currently active store
    val storeOrders: StateFlow<List<OrderEntity>> = _activeStore
        .flatMapLatest { store ->
            repository.getOrdersForStore(store.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Menu Items for currently active store
    val storeMenuItems: StateFlow<List<MenuItem>> = combine(
        _activeStore,
        repository.menuItemsFlow
    ) { store, allItems ->
        allItems.filter { it.storeId == store.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Menu search and category filter
    val menuSearchQuery = MutableStateFlow("")
    val selectedMenuCategory = MutableStateFlow("All")

    val filteredMenuItems: StateFlow<List<MenuItem>> = combine(
        storeMenuItems,
        menuSearchQuery,
        selectedMenuCategory
    ) { items, query, category ->
        items.filter { item ->
            val matchesCategory = (category == "All") || item.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Business stats
    val restaurantStats: StateFlow<RestaurantStats> = storeOrders.combine(_activeStore) { orders, _ ->
        val completedOrders = orders.filter { it.status == OrderStatus.DELIVERED.name }
        val active = orders.filter {
            it.status in listOf(OrderStatus.PLACED.name, OrderStatus.CONFIRMED.name, OrderStatus.PREPARING.name, OrderStatus.OUT_FOR_DELIVERY.name)
        }
        val revenue = completedOrders.sumOf { it.total } + (active.sumOf { it.total } * 0.9)
        val count = orders.size
        val aov = if (count > 0) revenue / count else 0.0

        RestaurantStats(
            todaysRevenue = revenue,
            todaysOrdersCount = count,
            activeOrdersCount = active.size,
            avgOrderValue = aov,
            acceptanceRatePercent = 98,
            avgPrepMinutes = 18
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RestaurantStats())

    init {
        // Collect current logged-in owner and automatically lock into their restaurant
        viewModelScope.launch {
            currentOwner.collect { owner ->
                if (owner != null) {
                    applyOwnerStore(owner)
                }
            }
        }
        // Initialize active store online status
        _isStoreOnline.value = repository.isStoreOnline(_activeStore.value.id)
        seedSampleOrdersIfEmpty()
    }

    private fun applyOwnerStore(owner: RestaurantOwner) {
        val store = repository.getStoreById(owner.restaurantId) ?: Store(
            id = owner.restaurantId,
            name = owner.restaurantName,
            type = owner.businessType,
            tagline = if (owner.businessType == BusinessType.FOOD) "Freshly prepared by ${owner.restaurantName}" else "Fast daily essentials from ${owner.restaurantName}",
            rating = 4.8,
            ratingCount = 1,
            deliveryTimeMin = 20,
            distanceKm = 1.2,
            deliveryFee = 25.0,
            imageUrl = if (owner.businessType == BusinessType.FOOD) {
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=700&auto=format&fit=crop&q=80"
            } else {
                "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=700&auto=format&fit=crop&q=80"
            },
            cuisines = owner.cuisine.split(",").map { it.trim() }.filter { it.isNotBlank() },
            location = owner.location
        ).also { repository.registerNewStore(it) }

        _activeStore.value = store
        _isStoreOnline.value = repository.isStoreOnline(store.id)
        selectedMenuCategory.value = "All"
        menuSearchQuery.value = ""
        seedSampleOrdersIfEmpty()
    }

    fun loginRestaurantOwner(identifier: String, pass: String): String? {
        val res = authManager.login(identifier, pass)
        return if (res.isSuccess) {
            val owner = res.getOrNull()!!
            applyOwnerStore(owner)
            null
        } else {
            res.exceptionOrNull()?.message ?: "Login failed"
        }
    }

    fun registerRestaurantOwner(
        ownerName: String,
        email: String,
        phone: String,
        password: String,
        restaurantName: String,
        businessType: BusinessType,
        cuisine: String,
        location: String
    ): String? {
        val res = authManager.register(
            ownerName = ownerName,
            email = email,
            phone = phone,
            password = password,
            restaurantName = restaurantName,
            businessType = businessType,
            cuisine = cuisine,
            location = location
        )
        return if (res.isSuccess) {
            val (owner, storeAndItems) = res.getOrNull()!!
            repository.registerNewStore(storeAndItems.first, storeAndItems.second)
            applyOwnerStore(owner)
            null
        } else {
            res.exceptionOrNull()?.message ?: "Registration failed"
        }
    }

    fun logoutRestaurantOwner() {
        authManager.logout()
    }

    fun seedSampleOrdersIfEmpty() {
        viewModelScope.launch {
            val store = _activeStore.value
            val sampleOrders = listOf(
                OrderEntity(
                    orderId = "BM-${(1000..9999).random()}",
                    storeId = store.id,
                    storeName = store.name,
                    storeType = store.type.name,
                    itemsSummary = "Hyderabadi Chicken Dum Biryani (x2), Mirchi Ka Salan (x1), Garlic Naan (x2)",
                    itemsCount = 5,
                    subtotal = 820.0,
                    deliveryFee = 40.0,
                    taxes = 38.0,
                    discount = 0.0,
                    total = 898.0,
                    status = OrderStatus.PLACED.name,
                    timestamp = System.currentTimeMillis() - (3 * 60 * 1000),
                    addressTitle = "Home",
                    addressFull = "Flat 402, Green Palms, Indiranagar, Bangalore",
                    partnerName = null,
                    partnerPhone = null,
                    partnerVehicle = null,
                    partnerRating = null,
                    otp = "4821",
                    paymentMethod = "UPI"
                ),
                OrderEntity(
                    orderId = "BM-${(1000..9999).random()}",
                    storeId = store.id,
                    storeName = store.name,
                    storeType = store.type.name,
                    itemsSummary = "Paneer Tikka Biryani (x1), Gulab Jamun (x2)",
                    itemsCount = 3,
                    subtotal = 430.0,
                    deliveryFee = 25.0,
                    taxes = 20.0,
                    discount = 0.0,
                    total = 475.0,
                    status = OrderStatus.PREPARING.name,
                    timestamp = System.currentTimeMillis() - (12 * 60 * 1000),
                    addressTitle = "Work",
                    addressFull = "Tower B, Prestige Acropolis, Koramangala, Bangalore",
                    partnerName = "Vikas Gowda",
                    partnerPhone = "+91 98450 12345",
                    partnerVehicle = "Honda Activa 6G • KA 03 JB 8821",
                    partnerRating = 4.9,
                    otp = "7193",
                    paymentMethod = "Cards"
                ),
                OrderEntity(
                    orderId = "BM-${(1000..9999).random()}",
                    storeId = store.id,
                    storeName = store.name,
                    storeType = store.type.name,
                    itemsSummary = "Mutton Dum Biryani (x1), Chicken 65 (x1)",
                    itemsCount = 2,
                    subtotal = 680.0,
                    deliveryFee = 35.0,
                    taxes = 25.0,
                    discount = 0.0,
                    total = 740.0,
                    status = OrderStatus.OUT_FOR_DELIVERY.name,
                    timestamp = System.currentTimeMillis() - (25 * 60 * 1000),
                    addressTitle = "Other",
                    addressFull = "12th Main Road, HAL 2nd Stage, Bangalore",
                    partnerName = "Suresh Kumar",
                    partnerPhone = "+91 99001 22334",
                    partnerVehicle = "Bajaj Pulsar 150 • KA 05 EH 1290",
                    partnerRating = 4.8,
                    otp = "3319",
                    paymentMethod = "Cash on Delivery"
                )
            )
            sampleOrders.forEach { order ->
                repository.insertOrder(order)
            }
        }
    }

    fun simulateIncomingCustomerOrder(
        customerName: String = listOf("Rahul Sharma", "Priya Nair", "Vikram Malhotra", "Sneha Patel", "Ananya Rao").random(),
        customItems: List<String>? = null,
        paymentMethod: String = listOf("UPI", "Credit Card", "Cash on Delivery").random()
    ) {
        viewModelScope.launch {
            val store = _activeStore.value
            val randomId = (1000..9999).random()
            val menu = storeMenuItems.value

            val itemsSummary = if (!customItems.isNullOrEmpty()) {
                customItems.joinToString(", ")
            } else if (menu.isNotEmpty()) {
                val sampleItems = menu.shuffled().take((1..3).random().coerceAtMost(menu.size))
                sampleItems.map { "${it.name} (x${(1..2).random()})" }.joinToString(", ")
            } else {
                listOf(
                    "Special Chef Biryani (x2), Butter Roti (x4)",
                    "Butter Chicken Special (x1), Jeera Rice (x1), Tandoori Roti (x2)",
                    "Paneer Butter Masala (x1), Dal Makhani (x1), Garlic Naan (x3)"
                ).random()
            }

            val subtotal = (220..580).random().toDouble()
            val deliveryFee = 30.0
            val taxes = ((subtotal * 0.05).toInt()).toDouble()
            val total = subtotal + deliveryFee + taxes

            val newOrder = OrderEntity(
                orderId = "BM-$randomId",
                storeId = store.id,
                storeName = store.name,
                storeType = store.type.name,
                itemsSummary = "$customerName: $itemsSummary",
                itemsCount = itemsSummary.split(",").size,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                taxes = taxes,
                discount = 0.0,
                total = total,
                status = OrderStatus.PLACED.name,
                timestamp = System.currentTimeMillis(),
                addressTitle = listOf("Home", "Office", "Apartment").random(),
                addressFull = listOf(
                    "80 Feet Road, 4th Block, Indiranagar, Bangalore",
                    "100 Feet Road, HAL 2nd Stage, Indiranagar, Bangalore",
                    "Prestige Tech Park, Marathahalli-Sarjapur Outer Ring Rd, Bangalore",
                    "Koramangala 5th Block, near Jyoti Nivas College, Bangalore"
                ).random(),
                partnerName = null,
                partnerPhone = null,
                partnerVehicle = null,
                partnerRating = null,
                otp = "${(1000..9999).random()}",
                paymentMethod = paymentMethod
            )
            repository.insertOrder(newOrder)
            _selectedOrderTab.value = RestaurantOrderTab.NEW
        }
    }

    fun simulateRushHourOrders() {
        viewModelScope.launch {
            repeat(3) {
                simulateIncomingCustomerOrder()
                kotlinx.coroutines.delay(200)
            }
        }
    }

    fun selectStore(store: Store) {
        _activeStore.value = store
        _isStoreOnline.value = repository.isStoreOnline(store.id)
        selectedMenuCategory.value = "All"
        menuSearchQuery.value = ""
        seedSampleOrdersIfEmpty()
    }

    fun deleteDish(itemId: String) {
        repository.deleteMenuItem(itemId)
    }

    fun setNav(nav: RestaurantNav) {
        _currentNav.value = nav
    }

    fun setOrderTab(tab: RestaurantOrderTab) {
        _selectedOrderTab.value = tab
    }

    fun toggleStoreOnline() {
        val newStatus = repository.toggleStoreOnline(_activeStore.value.id)
        _isStoreOnline.value = newStatus
    }

    // Order Fulfillment Actions
    fun acceptOrder(orderId: String, prepMinutes: Int = 20) {
        viewModelScope.launch {
            repository.updateRestaurantOrderStatus(
                orderId = orderId,
                status = OrderStatus.PREPARING.name,
                partnerName = "Vikas Gowda (Rider Assigned)",
                partnerPhone = "+91 98450 12345",
                partnerVehicle = "Honda Activa 6G • KA 03 JB 8821"
            )
            // Auto switch to PREPARING tab to view kitchen cooking
            _selectedOrderTab.value = RestaurantOrderTab.PREPARING
        }
    }

    fun rejectOrder(orderId: String, reason: String = "Kitchen busy / Item unavailable") {
        viewModelScope.launch {
            repository.updateRestaurantOrderStatus(
                orderId = orderId,
                status = OrderStatus.CANCELLED.name
            )
        }
    }

    fun markReadyForPickup(orderId: String) {
        viewModelScope.launch {
            repository.updateRestaurantOrderStatus(
                orderId = orderId,
                status = OrderStatus.OUT_FOR_DELIVERY.name,
                partnerName = "Vikas Gowda (At Restaurant Doorstep)",
                partnerPhone = "+91 98450 12345",
                partnerVehicle = "Honda Activa 6G • KA 03 JB 8821"
            )
            _selectedOrderTab.value = RestaurantOrderTab.READY
        }
    }

    fun markOrderHandedOver(orderId: String) {
        viewModelScope.launch {
            repository.updateRestaurantOrderStatus(
                orderId = orderId,
                status = OrderStatus.DELIVERED.name
            )
            _selectedOrderTab.value = RestaurantOrderTab.COMPLETED
        }
    }

    // Menu Operations
    fun toggleItemStock(itemId: String) {
        repository.toggleItemStock(itemId)
    }

    fun updateItemPrice(itemId: String, newPrice: Double) {
        if (newPrice > 0) {
            repository.updateItemPrice(itemId, newPrice)
        }
    }

    fun addNewDish(
        name: String,
        description: String,
        price: Double,
        isVeg: Boolean,
        category: String,
        unit: String
    ) {
        val newItem = MenuItem(
            id = "dish_${UUID.randomUUID().toString().take(8)}",
            storeId = _activeStore.value.id,
            name = name,
            description = description,
            price = price,
            originalPrice = price * 1.15,
            imageUrl = if (isVeg) {
                "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=80"
            } else {
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop&q=80"
            },
            isVeg = isVeg,
            category = category.ifBlank { "Main Course" },
            rating = 4.8,
            ratingCount = 1,
            isBestseller = false,
            unit = unit.ifBlank { "1 serving" },
            inStock = true
        )
        repository.addMenuItem(newItem)
    }
}
