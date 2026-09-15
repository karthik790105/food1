package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BiteMartDatabase
import com.example.data.BiteMartRepository
import com.example.data.entity.AddressEntity
import com.example.data.entity.CartEntity
import com.example.data.entity.OrderEntity
import com.example.data.entity.UserEntity
import com.example.data.firebase.FirebaseDatabaseService
import com.example.model.BusinessType
import com.example.model.CategoryFilter
import com.example.model.Coupon
import com.example.model.DeliveryAddress
import com.example.model.DeliveryPartner
import com.example.model.MenuItem
import com.example.model.OrderStatus
import com.example.model.Store
import com.example.util.SmsNotificationHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AuthMode {
    SIGN_UP,
    LOGIN
}

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION
}

enum class AppScreen {
    EXPLORE,
    SEARCH,
    STORE_DETAIL,
    CART,
    LIVE_TRACKING,
    ORDERS,
    NETWORK_ECOSYSTEM
}

data class CartSummary(
    val items: List<CartEntity> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 35.0,
    val platformFee: Double = 5.0,
    val taxes: Double = 0.0,
    val discount: Double = 0.0,
    val tip: Double = 0.0,
    val total: Double = 0.0,
    val appliedCoupon: Coupon? = null,
    val storeName: String = "",
    val storeType: String = ""
)

class CustomerDeliveryViewModel(application: Application) : AndroidViewModel(application) {

    val firebaseDb = FirebaseDatabaseService(application)
    val isCloudConnected: StateFlow<Boolean> = firebaseDb.isCloudConnected
    val cloudStatus: StateFlow<String> = firebaseDb.connectionStatus

    private val db = BiteMartDatabase.getInstance(application)
    val repository = BiteMartRepository(
        cartDao = db.cartDao(),
        orderDao = db.orderDao(),
        addressDao = db.addressDao(),
        favoriteDao = db.favoriteDao(),
        userDao = db.userDao(),
        firebaseDb = firebaseDb
    )

    private var activeOrderFirestoreListener: ListenerRegistration? = null

    init {
        // Seed stores catalogue to Cloud Firestore if connected
        firebaseDb.seedStoresIfConnected(BiteMartRepository.allStores, viewModelScope)
    }

    // User Authentication States
    val activeUser: StateFlow<UserEntity?> = repository.activeUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _authMode = MutableStateFlow(AuthMode.SIGN_UP)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    private val _authStep = MutableStateFlow(AuthStep.PHONE_INPUT)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    private val _inputName = MutableStateFlow("")
    val inputName: StateFlow<String> = _inputName.asStateFlow()

    private val _inputPhone = MutableStateFlow("")
    val inputPhone: StateFlow<String> = _inputPhone.asStateFlow()

    private val _inputOtp = MutableStateFlow("")
    val inputOtp: StateFlow<String> = _inputOtp.asStateFlow()

    private val _latestGeneratedOtp = MutableStateFlow<String?>(null)
    val latestGeneratedOtp: StateFlow<String?> = _latestGeneratedOtp.asStateFlow()

    private val _smsBannerVisible = MutableStateFlow(false)
    val smsBannerVisible: StateFlow<Boolean> = _smsBannerVisible.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(30)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private var countdownJob: Job? = null


    // UI Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.EXPLORE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Authentication Operations
    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _authErrorMessage.value = null
    }

    fun setInputName(name: String) {
        _inputName.value = name
        _authErrorMessage.value = null
    }

    fun setInputPhone(phone: String) {
        val digits = phone.filter { it.isDigit() }
        _inputPhone.value = if (digits.length <= 10) digits else digits.take(10)
        _authErrorMessage.value = null
    }

    fun setInputOtp(otp: String) {
        val digits = otp.filter { it.isDigit() }
        _inputOtp.value = if (digits.length <= 4) digits else digits.take(4)
        _authErrorMessage.value = null
    }

    fun requestOtp() {
        val phone = _inputPhone.value.trim()
        val name = _inputName.value.trim()

        if (phone.length < 10) {
            _authErrorMessage.value = "Please enter a valid 10-digit mobile number."
            return
        }

        if (_authMode.value == AuthMode.SIGN_UP && name.isBlank()) {
            _authErrorMessage.value = "Please enter your full name to sign up."
            return
        }

        _isAuthLoading.value = true
        _authErrorMessage.value = null

        viewModelScope.launch {
            val existing = repository.getUserByPhone(phone)
            if (_authMode.value == AuthMode.LOGIN && existing == null) {
                _authErrorMessage.value = "Mobile number not found. Please switch to Sign Up."
                _isAuthLoading.value = false
                return@launch
            }

            // Generate a 4-digit OTP code
            val code = (1000..9999).random().toString()
            _latestGeneratedOtp.value = code

            // Persist OTP code into the database
            repository.saveOtp(phone, code)

            // Dispatch SMS system notification to phone
            val app = getApplication<Application>()
            SmsNotificationHelper.sendOtpSmsNotification(app, phone, code)

            // Show simulated heads-up SMS banner in-app
            _smsBannerVisible.value = true
            _inputOtp.value = ""
            _authStep.value = AuthStep.OTP_VERIFICATION
            _isAuthLoading.value = false

            // Start countdown timer
            startOtpCountdown()
        }
    }

    private fun startOtpCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _countdownSeconds.value = 30
            while (_countdownSeconds.value > 0) {
                delay(1000)
                _countdownSeconds.value -= 1
            }
        }
    }

    fun resendOtp() {
        if (_countdownSeconds.value == 0) {
            requestOtp()
        }
    }

    fun autoFillOtp() {
        val code = _latestGeneratedOtp.value
        if (!code.isNullOrBlank()) {
            _inputOtp.value = code
            verifyOtpAndLogin()
        }
    }

    fun dismissSmsBanner() {
        _smsBannerVisible.value = false
    }

    fun resetAuthToPhone() {
        _authStep.value = AuthStep.PHONE_INPUT
        _inputOtp.value = ""
        _authErrorMessage.value = null
        _smsBannerVisible.value = false
    }

    fun verifyOtpAndLogin() {
        val phone = _inputPhone.value.trim()
        val otp = _inputOtp.value.trim()
        val name = _inputName.value.trim()

        if (otp.length < 4) {
            _authErrorMessage.value = "Please enter the 4-digit code sent via SMS."
            return
        }

        _isAuthLoading.value = true
        _authErrorMessage.value = null

        viewModelScope.launch {
            val success = repository.verifyAndLogin(phone, otp, name)
            _isAuthLoading.value = false
            if (success) {
                _smsBannerVisible.value = false
                _authStep.value = AuthStep.PHONE_INPUT
                _inputOtp.value = ""
                _latestGeneratedOtp.value = null
                _currentScreen.value = AppScreen.EXPLORE
            } else {
                _authErrorMessage.value = "Invalid OTP. Please check the code in your SMS messages."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authStep.value = AuthStep.PHONE_INPUT
            _inputPhone.value = ""
            _inputName.value = ""
            _inputOtp.value = ""
            _smsBannerVisible.value = false
        }
    }


    private val _currentBusinessType = MutableStateFlow(BusinessType.FOOD)
    val currentBusinessType: StateFlow<BusinessType> = _currentBusinessType.asStateFlow()

    private val _selectedStoreId = MutableStateFlow<String?>(null)
    val selectedStoreId: StateFlow<String?> = _selectedStoreId.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _vegOnlyFilter = MutableStateFlow(false)
    val vegOnlyFilter: StateFlow<Boolean> = _vegOnlyFilter.asStateFlow()

    private val _fastDeliveryFilter = MutableStateFlow(false)
    val fastDeliveryFilter: StateFlow<Boolean> = _fastDeliveryFilter.asStateFlow()

    private val _rating4PlusFilter = MutableStateFlow(false)
    val rating4PlusFilter: StateFlow<Boolean> = _rating4PlusFilter.asStateFlow()

    // Cart details
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _deliveryTip = MutableStateFlow(1.0)
    val deliveryTip: StateFlow<Double> = _deliveryTip.asStateFlow()

    private val _deliveryInstructions = MutableStateFlow("Leave at door & ring bell once")
    val deliveryInstructions: StateFlow<String> = _deliveryInstructions.asStateFlow()

    private val _paymentMethod = MutableStateFlow("UPI / Instant Pay")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Addresses - Starts empty (no default delivery address)
    private val _userAddresses = MutableStateFlow<List<DeliveryAddress>>(emptyList())
    val userAddresses: StateFlow<List<DeliveryAddress>> = _userAddresses.asStateFlow()

    private val _selectedAddressId = MutableStateFlow<String?>(null)
    val selectedAddressId: StateFlow<String?> = _selectedAddressId.asStateFlow()

    // Database reactive flows
    val cartItems: StateFlow<List<CartEntity>> = repository.cartItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeOrder: StateFlow<OrderEntity?> = repository.activeOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val favorites: StateFlow<List<String>> = repository.favorites.combine(MutableStateFlow(Unit)) { favs, _ ->
        favs.map { it.storeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculated Cart Summary (Standard delivery fee, no promotional delivery waivers/offers)
    val cartSummary: StateFlow<CartSummary> = combine(
        cartItems,
        _appliedCoupon,
        _deliveryTip
    ) { items, _, tip ->
        if (items.isEmpty()) {
            CartSummary()
        } else {
            val subtotal = items.sumOf { it.price * it.quantity }
            val first = items.first()
            val store = repository.getStoreById(first.storeId)
            val deliveryFee = store?.deliveryFee ?: 35.0
            val taxes = subtotal * 0.05
            val platformFee = 5.0
            val discount = 0.0

            val total = maxOf(0.0, subtotal + deliveryFee + taxes + platformFee + tip - discount)

            CartSummary(
                items = items,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                platformFee = platformFee,
                taxes = taxes,
                discount = discount,
                tip = tip,
                total = total,
                appliedCoupon = null,
                storeName = first.storeName,
                storeType = first.storeType
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartSummary()
    )

    // Live Tracking Simulation Coroutine
    private var trackingJob: Job? = null

    // Navigation functions
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setBusinessType(type: BusinessType) {
        _currentBusinessType.value = type
        _selectedCategory.value = if (type == BusinessType.FOOD) "all" else "all_g"
    }

    fun openStore(storeId: String) {
        _selectedStoreId.value = storeId
        _currentScreen.value = AppScreen.STORE_DETAIL
    }

    fun openLiveTracking(orderId: String) {
        _selectedOrderId.value = orderId
        _currentScreen.value = AppScreen.LIVE_TRACKING
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(categoryId: String) {
        _selectedCategory.value = categoryId
    }

    fun toggleVegFilter() {
        _vegOnlyFilter.value = !_vegOnlyFilter.value
    }

    fun toggleFastDeliveryFilter() {
        _fastDeliveryFilter.value = !_fastDeliveryFilter.value
    }

    fun toggleRatingFilter() {
        _rating4PlusFilter.value = !_rating4PlusFilter.value
    }

    fun applyCoupon(coupon: Coupon) {
        _appliedCoupon.value = coupon
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
    }

    fun setTip(tip: Double) {
        _deliveryTip.value = tip
    }

    fun setInstructions(instructions: String) {
        _deliveryInstructions.value = instructions
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun setSelectedAddress(addressId: String) {
        _selectedAddressId.value = addressId
    }

    fun addNewAddress(title: String, fullAddress: String, landmark: String) {
        val phone = activeUser.value?.phone ?: "+91 98765 43210"
        val newAddr = DeliveryAddress(
            id = "addr_${System.currentTimeMillis()}",
            title = title.ifBlank { "Delivery Address" },
            fullAddress = fullAddress,
            landmark = landmark,
            phone = phone,
            isDefault = _userAddresses.value.isEmpty()
        )
        _userAddresses.value = _userAddresses.value + newAddr
        _selectedAddressId.value = newAddr.id
    }

    fun deleteAddress(addressId: String) {
        val updated = _userAddresses.value.filter { it.id != addressId }
        _userAddresses.value = updated
        if (_selectedAddressId.value == addressId) {
            _selectedAddressId.value = updated.firstOrNull()?.id
        }
    }

    // Cart actions
    fun addItemToCart(item: MenuItem, store: Store, quantityDelta: Int = 1) {
        viewModelScope.launch {
            // If cart contains items from a different store, clear cart or handle single store
            val currentItems = cartItems.value
            if (currentItems.isNotEmpty() && currentItems.any { it.storeId != store.id }) {
                repository.clearCart()
            }
            repository.addToCart(item, store, quantityDelta)
        }
    }

    fun updateCartQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(itemId, quantity)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun toggleFavorite(storeId: String) {
        viewModelScope.launch {
            val isFav = favorites.value.contains(storeId)
            repository.toggleFavorite(storeId, isFav)
        }
    }

    // Order Placement & Real-time Live Dispatch Lifecycle Simulation
    fun placeOrder() {
        val summary = cartSummary.value
        if (summary.items.isEmpty()) return

        val address = _userAddresses.value.find { it.id == _selectedAddressId.value }
            ?: _userAddresses.value.firstOrNull() ?: return

        val orderNum = Random.nextInt(10000, 99999)
        val orderId = "BM-$orderNum"
        val otp = Random.nextInt(1000, 9999).toString()
        val itemsSummary = summary.items.joinToString(", ") { "${it.quantity}x ${it.name}" }

        val orderEntity = OrderEntity(
            orderId = orderId,
            storeId = summary.items.first().storeId,
            storeName = summary.storeName,
            storeType = summary.storeType,
            itemsSummary = itemsSummary,
            itemsCount = summary.items.sumOf { it.quantity },
            subtotal = summary.subtotal,
            deliveryFee = summary.deliveryFee,
            taxes = summary.taxes,
            discount = summary.discount,
            total = summary.total,
            status = OrderStatus.PLACED.name,
            timestamp = System.currentTimeMillis(),
            addressTitle = address.title,
            addressFull = address.fullAddress,
            partnerName = null,
            partnerPhone = null,
            partnerVehicle = null,
            partnerRating = null,
            otp = otp,
            paymentMethod = _paymentMethod.value
        )

        viewModelScope.launch {
            repository.insertOrder(orderEntity)
            repository.clearCart()
            _selectedOrderId.value = orderId
            _currentScreen.value = AppScreen.LIVE_TRACKING

            // Connect real-time Firestore listener for merchant/driver updates across apps
            activeOrderFirestoreListener?.remove()
            activeOrderFirestoreListener = firebaseDb.listenToOrderRealtime(orderId) { status, pName, pPhone, pVehicle ->
                viewModelScope.launch {
                    if (!pName.isNullOrBlank()) {
                        repository.assignPartnerToOrder(
                            orderId = orderId,
                            status = status,
                            partnerName = pName,
                            partnerPhone = pPhone ?: "",
                            partnerVehicle = pVehicle ?: "",
                            partnerRating = 4.9
                        )
                    } else {
                        repository.updateOrderStatus(orderId, status)
                    }
                }
            }

            // Start simulated real-time dispatch cycle
            startOrderLifecycleSimulation(orderId)
        }
    }

    private fun startOrderLifecycleSimulation(orderId: String) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            // Stage 1: Sent to store, waiting confirmation (3 seconds)
            delay(3500)
            repository.updateOrderStatus(orderId, OrderStatus.CONFIRMED.name)

            // Stage 2: Kitchen preparing / Grocery packing (7 seconds)
            delay(6000)
            repository.updateOrderStatus(orderId, OrderStatus.PREPARING.name)

            // Stage 3: Delivery partner assigned (8 seconds)
            delay(7000)
            val partners = listOf(
                DeliveryPartner("Ramesh Varma", "+91 98450 12345", 4.9, "Hero Splendor Pro", "KA-04-ET-8921", 1450),
                DeliveryPartner("Arun Kumar", "+91 97321 65432", 4.8, "TVS Jupiter 125", "KA-01-MQ-3312", 980),
                DeliveryPartner("Sunil Gowda", "+91 99123 88412", 4.9, "Ather 450X EV", "KA-03-JJ-4920", 2100)
            )
            val assigned = partners.random()
            repository.assignPartnerToOrder(
                orderId = orderId,
                status = OrderStatus.OUT_FOR_DELIVERY.name,
                partnerName = assigned.name,
                partnerPhone = assigned.phone,
                partnerVehicle = "${assigned.vehicle} (${assigned.vehicleNumber})",
                partnerRating = assigned.rating
            )

            // Stage 4: Arrived and Delivered (12 seconds)
            delay(12000)
            repository.updateOrderStatus(orderId, OrderStatus.DELIVERED.name)
        }
    }

    fun reorder(order: OrderEntity) {
        val store = repository.getStoreById(order.storeId) ?: BiteMartRepository.allStores.first()
        val items = repository.getMenuItemsForStore(store.id)
        viewModelScope.launch {
            repository.clearCart()
            if (items.isNotEmpty()) {
                repository.addToCart(items.first(), store, 1)
            }
            _currentScreen.value = AppScreen.CART
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeOrderFirestoreListener?.remove()
    }
}
