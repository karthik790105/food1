package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AddressEntity
import com.example.data.entity.CartEntity
import com.example.data.entity.FavoriteEntity
import com.example.data.entity.OrderEntity
import com.example.data.entity.OtpRecordEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items WHERE itemId = :itemId LIMIT 1")
    suspend fun getCartItem(itemId: String): CartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CartEntity)

    @Update
    suspend fun updateItem(item: CartEntity)

    @Query("DELETE FROM cart_items WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getOrdersForStore(storeId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY timestamp DESC LIMIT 1")
    fun getActiveOrder(): Flow<OrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateStatus(orderId: String, status: String)

    @Query("UPDATE orders SET status = :status, partnerName = :partnerName, partnerPhone = :partnerPhone, partnerVehicle = :partnerVehicle, partnerRating = :partnerRating WHERE orderId = :orderId")
    suspend fun assignPartner(
        orderId: String,
        status: String,
        partnerName: String,
        partnerPhone: String,
        partnerVehicle: String,
        partnerRating: Double
    )

    @Query("SELECT * FROM orders WHERE status IN ('READY_FOR_PICKUP', 'PREPARING', 'CONFIRMED', 'PLACED') ORDER BY timestamp DESC")
    fun getAvailableOrdersForDelivery(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE partnerName = :partnerName AND status IN ('OUT_FOR_PICKUP', 'OUT_FOR_DELIVERY') ORDER BY timestamp DESC")
    fun getActiveDeliveriesForPartner(partnerName: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE partnerName = :partnerName AND status = 'DELIVERED' ORDER BY timestamp DESC")
    fun getCompletedDeliveriesForPartner(partnerName: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getOrdersForCustomer(phone: String): Flow<List<OrderEntity>>

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE orderId = :orderId")
    suspend fun cancelOrder(orderId: String)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses")
    fun getAllAddresses(): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Query("UPDATE addresses SET isDefault = 0")
    suspend fun resetDefaults()

    @Query("UPDATE addresses SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun deleteAddress(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE storeId = :storeId)")
    fun isFavorite(storeId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE storeId = :storeId")
    suspend fun removeFavorite(storeId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Query("UPDATE users SET isLoggedIn = 1 WHERE phone = :phone")
    suspend fun setLoggedIn(phone: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOtpRecord(otp: OtpRecordEntity)

    @Query("SELECT * FROM otp_records WHERE phone = :phone LIMIT 1")
    suspend fun getOtpRecord(phone: String): OtpRecordEntity?

    @Query("DELETE FROM otp_records WHERE phone = :phone")
    suspend fun deleteOtpRecord(phone: String)
}

