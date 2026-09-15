package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminAuthManager
import com.example.data.BiteMartDatabase
import com.example.data.BiteMartRepository
import com.example.data.DeliveryAuthManager
import com.example.data.RestaurantAuthManager
import com.example.data.entity.OrderEntity
import com.example.data.entity.UserEntity
import com.example.model.AdminUser
import com.example.model.BusinessType
import com.example.model.DeliveryPartner
import com.example.model.MenuItem
import com.example.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AdminNavTab {
    DASHBOARD,
    ORDERS,
    RESTAURANTS,
    RIDERS,
    CUSTOMERS
}

/**
 * Primary ViewModel powering the BiteMart Admin Portal.
 * Gives platform operators full control over live orders, confirmation OTPs,
 * restaurant suspensions, rider suspensions, menu item availability, pricing changes,
 * item additions/deletions, and customer account records.
 */
class AdminPortalViewModel(application: Application) : AndroidViewModel(application) {

    val adminAuthManager = AdminAuthManager(application)
    val currentAdmin: StateFlow<AdminUser?> = adminAuthManager.currentAdmin

    private val db = BiteMartDatabase.getInstance(application)
    val repository = BiteMartRepository(
        cartDao = db.cartDao(),
        orderDao = db.orderDao(),
        addressDao = db.addressDao(),
        favoriteDao = db.favoriteDao(),
        userDao = db.userDao()
    )

    val deliveryAuthManager = DeliveryAuthManager(application)
    val restaurantAuthManager = RestaurantAuthManager(application)

    // Navigation & View state
    private val _currentNavTab = MutableStateFlow(AdminNavTab.DASHBOARD)
    val currentNavTab: StateFlow<AdminNavTab> = _currentNavTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _orderStatusFilter = MutableStateFlow("ALL")
    val orderStatusFilter: StateFlow<String> = _orderStatusFilter.asStateFlow()

    private val _selectedStoreForMenu = MutableStateFlow<Store?>(null)
    val selectedStoreForMenu: StateFlow<Store?> = _selectedStoreForMenu.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Real-time Database & Platform Streams
    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suspendedStores: StateFlow<Set<String>> = repository.suspendedStores
    val storeOnlineStatus: StateFlow<Map<String, Boolean>> = repository.storeOnlineStatusFlow
    val suspendedRiders: StateFlow<Set<String>> = deliveryAuthManager.suspendedRiders
    val allDeliveryPartners: StateFlow<List<DeliveryPartner>> = deliveryAuthManager.allPartnersFlow

    val allStores: StateFlow<List<Store>> = repository.allStoresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getAllStores())

    // Selected Store Menu Items
    private val _menuItemsForSelectedStore = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItemsForSelectedStore: StateFlow<List<MenuItem>> = _menuItemsForSelectedStore.asStateFlow()

    // Printing Order State
    private val _selectedOrderForPrint = MutableStateFlow<OrderEntity?>(null)
    val selectedOrderForPrint: StateFlow<OrderEntity?> = _selectedOrderForPrint.asStateFlow()

    fun selectOrderForPrint(order: OrderEntity?) {
        _selectedOrderForPrint.value = order
    }

    fun setNavTab(tab: AdminNavTab) {
        _currentNavTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setOrderStatusFilter(filter: String) {
        _orderStatusFilter.value = filter
    }

    fun selectStoreForMenu(store: Store?) {
        _selectedStoreForMenu.value = store
        if (store != null) {
            refreshMenuItems(store.id)
        } else {
            _menuItemsForSelectedStore.value = emptyList()
        }
    }

    fun refreshMenuItems(storeId: String) {
        _menuItemsForSelectedStore.value = repository.getMenuItemsForStore(storeId)
    }

    // Authentication
    fun login(identifier: String, pass: String): Boolean {
        val (success, message) = adminAuthManager.login(identifier, pass)
        _userMessage.value = message
        return success
    }

    fun registerAdmin(
        name: String,
        email: String,
        phone: String,
        role: String,
        dept: String,
        pass: String,
        accessCode: String
    ): Boolean {
        val (success, message) = adminAuthManager.registerAdmin(
            name = name,
            email = email,
            phone = phone,
            role = role,
            department = dept,
            password = pass,
            accessCode = accessCode
        )
        _userMessage.value = message
        return success
    }

    fun logout() {
        adminAuthManager.logout()
        _userMessage.value = "Admin session ended securely."
    }

    // Restaurant Operations
    fun toggleStoreSuspension(storeId: String) {
        val store = repository.getStoreById(storeId)
        val isNowSuspended = repository.toggleStoreSuspension(storeId)
        val name = store?.name ?: "Restaurant"
        _userMessage.value = if (isNowSuspended) {
            "⚠️ $name has been SUSPENDED by Admin. Outlet cannot receive orders."
        } else {
            "✅ $name has been REACTIVATED and can now receive orders."
        }
    }

    fun toggleStoreOnline(storeId: String) {
        val store = repository.getStoreById(storeId)
        val isNowOnline = repository.toggleStoreOnline(storeId)
        val name = store?.name ?: "Restaurant"
        _userMessage.value = if (isNowOnline) {
            "🟢 $name is now ONLINE (accepting customer orders)."
        } else {
            "🔴 $name is now OFFLINE (outlet closed / paused)."
        }
    }

    // Delivery Partner Operations
    fun toggleRiderSuspension(riderId: String) {
        val isSuspended = deliveryAuthManager.toggleRiderSuspension(riderId)
        val rider = deliveryAuthManager.registeredPartners.find { it.id == riderId }
        val name = rider?.name ?: "Rider"
        _userMessage.value = if (isSuspended) {
            "⚠️ Rider $name has been SUSPENDED by Admin."
        } else {
            "✅ Rider $name has been RESTORED and can take delivery requests."
        }
    }

    // Menu Management Operations
    fun toggleItemStock(itemId: String) {
        val isInStock = repository.toggleItemStock(itemId)
        val store = _selectedStoreForMenu.value
        if (store != null) {
            refreshMenuItems(store.id)
        }
        _userMessage.value = if (isInStock) "Item marked IN STOCK" else "Item marked OUT OF STOCK"
    }

    fun updateItemPrice(itemId: String, newPrice: Double) {
        if (newPrice <= 0.0) {
            _userMessage.value = "Please enter a valid positive price amount."
            return
        }
        repository.updateItemPrice(itemId, newPrice)
        val store = _selectedStoreForMenu.value
        if (store != null) {
            refreshMenuItems(store.id)
        }
        _userMessage.value = "Price updated to ₹${newPrice.toInt()}"
    }

    fun addMenuItem(
        storeId: String,
        name: String,
        description: String,
        price: Double,
        isVeg: Boolean,
        category: String,
        unit: String
    ): Boolean {
        if (name.isBlank()) {
            _userMessage.value = "Please enter an item name."
            return false
        }
        if (price <= 0.0) {
            _userMessage.value = "Please enter a valid price."
            return false
        }

        val newItem = MenuItem(
            id = "item_" + UUID.randomUUID().toString().take(8),
            storeId = storeId,
            name = name.trim(),
            description = description.trim(),
            price = price,
            imageUrl = if (isVeg) "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400" else "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=400",
            isVeg = isVeg,
            category = category.ifBlank { "Mains" },
            rating = 4.5,
            ratingCount = 10,
            isBestseller = false,
            unit = unit.ifBlank { "1 serving" },
            inStock = true
        )

        repository.addMenuItem(newItem)
        refreshMenuItems(storeId)
        _userMessage.value = "✅ '$name' added to store menu!"
        return true
    }

    fun deleteMenuItem(itemId: String) {
        repository.deleteMenuItem(itemId)
        val store = _selectedStoreForMenu.value
        if (store != null) {
            refreshMenuItems(store.id)
        }
        _userMessage.value = "Item removed from menu."
    }

    // Order Management Operations
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrder(orderId)
            _userMessage.value = "Order #$orderId has been CANCELLED by Admin."
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
            _userMessage.value = "Order #$orderId status updated to $status."
        }
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
