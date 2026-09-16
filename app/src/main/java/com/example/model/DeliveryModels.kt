package com.example.model

enum class BusinessType {
    FOOD,
    GROCERY
}

enum class OrderStatus(val displayName: String, val description: String) {
    PLACED("Order Placed", "Your order has been sent to the store"),
    CONFIRMED("Confirmed", "Store has accepted your order"),
    PREPARING("Preparing", "Items are being freshly prepared & packed"),
    READY_FOR_PICKUP("Ready for Pickup", "Order is packed & ready for rider pickup"),
    OUT_FOR_PICKUP("Rider En Route to Store", "Delivery partner assigned and heading to pickup"),
    OUT_FOR_DELIVERY("Out for Delivery", "Rider picked up and heading to your doorstep"),
    DELIVERED("Delivered", "Delivered successfully! Enjoy your meal"),
    CANCELLED("Cancelled", "Order was cancelled")
}

data class Store(
    val id: String,
    val name: String,
    val type: BusinessType,
    val tagline: String,
    val rating: Double,
    val ratingCount: Int,
    val deliveryTimeMin: Int,
    val distanceKm: Double,
    val deliveryFee: Double,
    val minOrder: Double = 0.0,
    val imageUrl: String,
    val cuisines: List<String>,
    val isPromoted: Boolean = false,
    val discountText: String? = null,
    val isVegOnly: Boolean = false,
    val location: String = "Indiranagar, Bangalore",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946
)

data class MenuItem(
    val id: String,
    val storeId: String,
    val name: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null,
    val imageUrl: String,
    val isVeg: Boolean,
    val category: String,
    val rating: Double = 4.5,
    val ratingCount: Int = 120,
    val isBestseller: Boolean = false,
    val unit: String = "1 serving",
    val inStock: Boolean = true
)

data class CartItem(
    val item: MenuItem,
    val quantity: Int,
    val specialInstructions: String = ""
)

data class DeliveryAddress(
    val id: String,
    val title: String, // "Home", "Work", "Other"
    val fullAddress: String,
    val landmark: String = "",
    val phone: String = "+91 98765 43210",
    val isDefault: Boolean = false
)

data class DeliveryPartner(
    val name: String,
    val phone: String,
    val rating: Double = 4.9,
    val vehicle: String = "TVS Jupiter",
    val vehicleNumber: String = "KA 01 EQ 4521",
    val totalDeliveries: Int = 1420,
    val id: String = "dp_1",
    val email: String = "",
    val password: String = "1234",
    val drivingLicense: String = "KA0120220008912",
    val earningsToday: Double = 840.0,
    val isOnline: Boolean = true,
    val activeCity: String = "Indiranagar, Bangalore"
)

data class Order(
    val id: String,
    val storeId: String,
    val storeName: String,
    val storeType: BusinessType,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryFee: Double,
    val taxes: Double,
    val discount: Double,
    val tip: Double,
    val total: Double,
    val status: OrderStatus,
    val placedAtMillis: Long,
    val estimatedMinutes: Int,
    val address: DeliveryAddress,
    val deliveryPartner: DeliveryPartner?,
    val otp: String,
    val paymentMethod: String = "Google Pay / UPI"
)

data class Coupon(
    val code: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val maxDiscount: Double,
    val minSpend: Double
)

data class CategoryFilter(
    val id: String,
    val name: String,
    val businessType: BusinessType,
    val iconName: String
)

data class RestaurantOwner(
    val id: String,
    val ownerName: String,
    val email: String,
    val phone: String,
    val password: String = "1234",
    val restaurantId: String,
    val restaurantName: String,
    val businessType: BusinessType = BusinessType.FOOD,
    val cuisine: String = "Indian & Mughlai",
    val location: String = "Indiranagar, Bangalore",
    val fssaiNumber: String = "11223344556677",
    val gstin: String = "29ABCDE1234F1Z5",
    val isVerified: Boolean = true
)

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "Super Admin",
    val department: String = "Central Operations",
    val password: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)

