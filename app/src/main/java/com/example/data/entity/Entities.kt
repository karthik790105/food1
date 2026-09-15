package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemId: String,
    val storeId: String,
    val storeName: String,
    val storeType: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val isVeg: Boolean,
    val imageUrl: String,
    val unit: String,
    val instructions: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val storeId: String,
    val storeName: String,
    val storeType: String,
    val itemsSummary: String,
    val itemsCount: Int,
    val subtotal: Double,
    val deliveryFee: Double,
    val taxes: Double,
    val discount: Double,
    val total: Double,
    val status: String,
    val timestamp: Long,
    val addressTitle: String,
    val addressFull: String,
    val partnerName: String?,
    val partnerPhone: String?,
    val partnerVehicle: String?,
    val partnerRating: Double?,
    val otp: String,
    val paymentMethod: String,
    val customerName: String = "Rahul Sharma",
    val customerPhone: String = "+91 98765 43210"
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val fullAddress: String,
    val landmark: String,
    val phone: String,
    val isDefault: Boolean
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val storeId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val phone: String,
    val name: String,
    val email: String? = null,
    val isLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "otp_records")
data class OtpRecordEntity(
    @PrimaryKey
    val phone: String,
    val otpCode: String,
    val sentAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 5 * 60 * 1000
)

