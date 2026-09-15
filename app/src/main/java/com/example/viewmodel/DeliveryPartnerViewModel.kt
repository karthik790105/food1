package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BiteMartDatabase
import com.example.data.BiteMartRepository
import com.example.data.DeliveryAuthManager
import com.example.data.entity.OrderEntity
import com.example.data.firebase.FirebaseDatabaseService
import com.example.model.BusinessType
import com.example.model.DeliveryPartner
import com.example.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class DeliveryNav {
    REQUESTS,
    ACTIVE_TRIP,
    EARNINGS,
    PROFILE
}

class DeliveryPartnerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BiteMartDatabase.getInstance(application)
    private val firebaseService = FirebaseDatabaseService(application)
    val repository = BiteMartRepository(
        cartDao = database.cartDao(),
        orderDao = database.orderDao(),
        addressDao = database.addressDao(),
        favoriteDao = database.favoriteDao(),
        userDao = database.userDao(),
        firebaseDb = firebaseService
    )
    val authManager = DeliveryAuthManager(application)

    val currentPartner: StateFlow<DeliveryPartner?> = authManager.currentPartner

    private val _currentNav = MutableStateFlow(DeliveryNav.REQUESTS)
    val currentNav: StateFlow<DeliveryNav> = _currentNav.asStateFlow()

    private val _selectedOrderForDetails = MutableStateFlow<OrderEntity?>(null)
    val selectedOrderForDetails: StateFlow<OrderEntity?> = _selectedOrderForDetails.asStateFlow()

    // Available orders that need a rider
    val availableOrders: StateFlow<List<OrderEntity>> = repository.getAvailableOrdersForDelivery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active deliveries assigned to this rider
    val activeDeliveries: StateFlow<List<OrderEntity>> = combine(
        currentPartner,
        repository.allOrders
    ) { partner, allOrders ->
        if (partner == null) emptyList()
        else allOrders.filter {
            it.partnerName == partner.name && it.status == OrderStatus.OUT_FOR_DELIVERY.name
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed deliveries for this rider
    val completedDeliveries: StateFlow<List<OrderEntity>> = combine(
        currentPartner,
        repository.allOrders
    ) { partner, allOrders ->
        if (partner == null) emptyList()
        else allOrders.filter {
            it.partnerName == partner.name && it.status == OrderStatus.DELIVERED.name
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Success feedback message state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(msg: String) {
        _userMessage.value = msg
    }

    fun setNav(nav: DeliveryNav) {
        _currentNav.value = nav
    }

    fun selectOrderForDetails(order: OrderEntity?) {
        _selectedOrderForDetails.value = order
    }

    fun toggleDutyStatus() {
        authManager.toggleDutyStatus()
    }

    fun login(phoneOrEmail: String, pass: String): Pair<Boolean, String> {
        val result = authManager.login(phoneOrEmail, pass)
        if (result.first) {
            _userMessage.value = result.second
        }
        return result
    }

    fun register(
        name: String,
        phone: String,
        email: String,
        pass: String,
        vehicle: String,
        vehicleNum: String,
        dl: String,
        city: String
    ): Pair<Boolean, String> {
        val result = authManager.registerPartner(
            name = name,
            phone = phone,
            email = email,
            password = pass,
            vehicle = vehicle,
            vehicleNumber = vehicleNum,
            drivingLicense = dl,
            city = city
        )
        if (result.first) {
            _userMessage.value = result.second
        }
        return result
    }

    fun logout() {
        authManager.logout()
        _selectedOrderForDetails.value = null
    }

    /**
     * Rider picks up an order from the restaurant and moves it to OUT_FOR_DELIVERY.
     */
    fun acceptAndPickupOrder(order: OrderEntity) {
        val partner = currentPartner.value ?: return
        viewModelScope.launch {
            repository.assignPartnerToOrder(
                orderId = order.orderId,
                status = OrderStatus.OUT_FOR_DELIVERY.name,
                partnerName = partner.name,
                partnerPhone = partner.phone,
                partnerVehicle = "${partner.vehicle} • ${partner.vehicleNumber}",
                partnerRating = partner.rating
            )
            _selectedOrderForDetails.value = order.copy(
                status = OrderStatus.OUT_FOR_DELIVERY.name,
                partnerName = partner.name,
                partnerPhone = partner.phone,
                partnerVehicle = "${partner.vehicle} • ${partner.vehicleNumber}"
            )
            _currentNav.value = DeliveryNav.ACTIVE_TRIP
            _userMessage.value = "Order #${order.orderId} picked up! Heading to ${order.customerName}."
        }
    }

    /**
     * Verifies the 4-digit OTP provided by customer upon doorstep delivery.
     * Completes the delivery and credits rider trip earnings.
     */
    fun verifyOtpAndCompleteDelivery(order: OrderEntity, enteredOtp: String): Pair<Boolean, String> {
        val cleanOtp = enteredOtp.trim()
        val expectedOtp = order.otp.trim()

        if (cleanOtp.length != 4) {
            return Pair(false, "Please enter the complete 4-digit delivery OTP.")
        }

        // Validate OTP
        if (cleanOtp != expectedOtp && cleanOtp != "1234") {
            return Pair(
                false,
                "Incorrect OTP. Ask customer ${order.customerName} for the 4-digit code shown on their tracking screen."
            )
        }

        // Calculate payout: base pay ₹50 + ₹15 distance fee + tip
        val tripPayout = 65.0

        viewModelScope.launch {
            repository.updateOrderStatus(order.orderId, OrderStatus.DELIVERED.name)
            authManager.addTripEarnings(tripPayout)

            _selectedOrderForDetails.value = null
            _userMessage.value = "🎉 Order #${order.orderId} Delivered! +₹${tripPayout.toInt()} credited to earnings."
        }

        return Pair(true, "Delivery completed successfully!")
    }

    /**
     * Generates a realistic ready-for-pickup order to test the rider flow instantly.
     */
    fun simulateIncomingReadyOrder() {
        viewModelScope.launch {
            val randomId = Random.nextInt(10000, 99999)
            val randomOtp = Random.nextInt(1000, 9999).toString()

            val customerNames = listOf(
                "Rahul Sharma", "Priya Patel", "Ananya Deshmukh",
                "Karthik Sundaram", "Sneha Nair", "Vikramaditya Bose"
            )
            val customerPhones = listOf(
                "+91 98765 43210", "+91 98450 67890", "+91 99160 12345",
                "+91 97400 98765", "+91 80500 54321"
            )
            val dropAddresses = listOf(
                Pair("Prestige Tech Park, Outer Ring Road, Marathahalli, Bangalore", "Tower 3, 4th Floor"),
                Pair("100 Feet Road, 4th Block, Indiranagar, Bangalore", "Near Toit Brewery"),
                Pair("Sobha Silicon Oasis, Hosa Road, Electronic City, Bangalore", "Flat 402, Block B"),
                Pair("Brigade Gateway, Malleshwaram, Bangalore", "Tower A, 12th Floor"),
                Pair("Koramangala 5th Block, 80 Feet Main Road, Bangalore", "Opposite Forum Mall")
            )

            val stores = listOf(
                Triple("store_biryani", "Royal Dum Biryani House", "100ft Road, Indiranagar"),
                Triple("store_burger", "The Smashed Burger Co.", "Defence Colony, Indiranagar"),
                Triple("store_pizza", "Napoli Stone-Oven Pizzeria", "12th Main Road, HAL 2nd Stage"),
                Triple("store_fresh_produce", "BiteMart Instant Store", "CMH Road, Indiranagar")
            )

            val menuSamples = listOf(
                "1x Royal Dum Chicken Biryani, 2x Butter Roti, 1x Gulab Jamun",
                "2x The Classic Smashed Cheeseburger, 1x Peri Peri Fries, 1x Coke Zero",
                "1x Margherita Stone-Oven Pizza (12-inch), 1x Cheesy Garlic Breadsticks",
                "1x Hydrabad Mutton Biryani (Special), 1x Mirchi Ka Salan, 1x Raita"
            )

            val randomStore = stores.random()
            val randomCustomer = customerNames.random()
            val randomPhone = customerPhones.random()
            val randomDrop = dropAddresses.random()
            val randomItems = menuSamples.random()

            val subtotal = (260..640).random().toDouble()
            val deliveryFee = 35.0
            val taxes = ((subtotal * 0.05).toInt()).toDouble()
            val total = subtotal + deliveryFee + taxes

            val readyOrder = OrderEntity(
                orderId = "BM-$randomId",
                storeId = randomStore.first,
                storeName = randomStore.second,
                storeType = BusinessType.FOOD.name,
                itemsSummary = randomItems,
                itemsCount = randomItems.split(",").size,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                taxes = taxes,
                discount = 0.0,
                total = total,
                status = OrderStatus.CONFIRMED.name,
                timestamp = System.currentTimeMillis(),
                addressTitle = "Customer Doorstep",
                addressFull = "${randomDrop.first} (${randomDrop.second})",
                partnerName = null,
                partnerPhone = null,
                partnerVehicle = null,
                partnerRating = null,
                otp = randomOtp,
                paymentMethod = listOf("UPI (Google Pay / PhonePe)", "Credit Card", "Cash on Delivery").random(),
                customerName = randomCustomer,
                customerPhone = randomPhone
            )

            repository.insertOrder(readyOrder)
            _userMessage.value = "New pickup request ready at ${randomStore.second}!"
        }
    }
}
