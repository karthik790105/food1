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

    // Active store strictly bound to the logged-in restaurant owner
    private val _activeStore = MutableStateFlow<Store>(
        authManager.currentOwner.value?.let { repository.getStoreById(it.restaurantId) }
            ?: (repository.getStoreById("store_biryani") ?: BiteMartRepository.allStores.first())
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
