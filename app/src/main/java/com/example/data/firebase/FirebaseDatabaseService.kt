package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.entity.AddressEntity
import com.example.data.entity.OrderEntity
import com.example.data.entity.UserEntity
import com.example.model.Store
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * FirebaseDatabaseService provides shared Cloud Firestore database synchronization
 * across the 4 network apps (Customer App, Merchant App, Driver App, Admin Ecosystem).
 */
class FirebaseDatabaseService(private val context: Context) {

    private val TAG = "FirebaseDatabase"
    private var firestore: FirebaseFirestore? = null

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Initializing Firebase...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    init {
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            _isCloudConnected.value = true
            _connectionStatus.value = "Firebase Cloud Database Active"
            Log.d(TAG, "Firebase Cloud Firestore initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore running in offline/local-first mode: ${e.message}")
            _isCloudConnected.value = false
            _connectionStatus.value = "Local-First Mode (Ready to link google-services.json)"
        }
    }

    /**
     * Synchronize a placed or updated order to the shared Firestore collection "orders".
     * Accessible by Restaurant App, Driver App, Customer App, and Admin.
     */
    suspend fun syncOrderToCloud(order: OrderEntity): Boolean {
        val db = firestore ?: return false
        return try {
            val orderPayload = hashMapOf(
                "orderId" to order.orderId,
                "storeId" to order.storeId,
                "storeName" to order.storeName,
                "storeType" to order.storeType,
                "itemsSummary" to order.itemsSummary,
                "itemsCount" to order.itemsCount,
                "subtotal" to order.subtotal,
                "deliveryFee" to order.deliveryFee,
                "taxes" to order.taxes,
                "discount" to order.discount,
                "total" to order.total,
                "status" to order.status,
                "timestamp" to order.timestamp,
                "addressTitle" to order.addressTitle,
                "addressFull" to order.addressFull,
                "partnerName" to (order.partnerName ?: ""),
                "partnerPhone" to (order.partnerPhone ?: ""),
                "partnerRating" to (order.partnerRating ?: 4.9),
                "partnerVehicle" to (order.partnerVehicle ?: ""),
                "deliveryPartnerName" to (order.partnerName ?: ""),
                "deliveryPartnerPhone" to (order.partnerPhone ?: ""),
                "deliveryPartnerRating" to (order.partnerRating ?: 4.9),
                "deliveryPartnerVehicle" to (order.partnerVehicle ?: ""),
                "otp" to order.otp,
                "deliveryOtp" to order.otp,
                "paymentMethod" to order.paymentMethod,
                "currency" to "INR",
                "currencySymbol" to "₹",
                "appSource" to "CUSTOMER_BASE_APP",
                "lastUpdatedAt" to System.currentTimeMillis()
            )

            db.collection("orders")
                .document(order.orderId)
                .set(orderPayload, SetOptions.merge())
                .await()

            Log.d(TAG, "Order ${order.orderId} synced to Firebase Cloud Firestore.")
            _isCloudConnected.value = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync order to Firestore (saved locally): ${e.message}")
            false
        }
    }

    /**
     * Update order status and details in Cloud Firestore from Merchant/Restaurant App.
     */
    suspend fun updateOrderStatusInCloud(
        orderId: String,
        status: String,
        partnerName: String? = null,
        partnerPhone: String? = null,
        partnerVehicle: String? = null
    ): Boolean {
        val db = firestore ?: return false
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "lastUpdatedAt" to System.currentTimeMillis()
            )
            if (!partnerName.isNullOrBlank()) updates["partnerName"] = partnerName
            if (!partnerPhone.isNullOrBlank()) updates["partnerPhone"] = partnerPhone
            if (!partnerVehicle.isNullOrBlank()) updates["partnerVehicle"] = partnerVehicle

            db.collection("orders")
                .document(orderId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d(TAG, "Order $orderId status updated to $status in Cloud Firestore.")
            _isCloudConnected.value = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update order status in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Listen in real-time to active order status updates made by Merchant or Driver App.
     */
    fun listenToOrderRealtime(
        orderId: String,
        onOrderUpdated: (status: String, partnerName: String?, partnerPhone: String?, partnerVehicle: String?) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("orders")
                .document(orderId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen error on order $orderId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: return@addSnapshotListener
                        val partnerName = snapshot.getString("partnerName")
                            ?: snapshot.getString("deliveryPartnerName")
                        val partnerPhone = snapshot.getString("partnerPhone")
                            ?: snapshot.getString("deliveryPartnerPhone")
                        val partnerVehicle = snapshot.getString("partnerVehicle")
                            ?: snapshot.getString("deliveryPartnerVehicle")
                        onOrderUpdated(status, partnerName, partnerPhone, partnerVehicle)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error registering Firestore listener: ${e.message}")
            null
        }
    }

    /**
     * Synchronize customer user account to Firestore collection "users".
     */
    suspend fun syncUserToCloud(user: UserEntity): Boolean {
        val db = firestore ?: return false
        return try {
            val userPayload = hashMapOf(
                "phone" to user.phone,
                "name" to user.name,
                "lastActive" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(user.phone)
                .set(userPayload, SetOptions.merge())
                .await()
            Log.d(TAG, "User ${user.phone} synced to Firebase Cloud.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync user to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Synchronize saved delivery address to Firestore collection "addresses".
     */
    suspend fun syncAddressToCloud(address: AddressEntity, userPhone: String): Boolean {
        val db = firestore ?: return false
        return try {
            val addressPayload = hashMapOf(
                "id" to address.id,
                "userPhone" to userPhone,
                "title" to address.title,
                "fullAddress" to address.fullAddress,
                "landmark" to address.landmark,
                "phone" to address.phone,
                "isDefault" to address.isDefault,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("addresses")
                .document(address.id)
                .set(addressPayload, SetOptions.merge())
                .await()
            Log.d(TAG, "Address ${address.id} synced to Firebase Cloud.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync address to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Delete an address from Firestore collection "addresses".
     */
    suspend fun deleteAddressFromCloud(addressId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("addresses").document(addressId).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete address from Firestore: ${e.message}")
            false
        }
    }

    /**
     * Seed stores catalogue in Cloud Firestore collection "stores"
     * so that the Merchant App, Driver App, and Customer App share identical data.
     */
    fun seedStoresIfConnected(stores: List<Store>, scope: CoroutineScope) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                for (store in stores) {
                    val storePayload = hashMapOf(
                        "id" to store.id,
                        "name" to store.name,
                        "type" to store.type.name,
                        "tagline" to store.tagline,
                        "rating" to store.rating,
                        "ratingCount" to store.ratingCount,
                        "deliveryTimeMin" to store.deliveryTimeMin,
                        "distanceKm" to store.distanceKm,
                        "deliveryFee" to store.deliveryFee,
                        "currency" to "INR",
                        "currencySymbol" to "₹",
                        "location" to store.location,
                        "cuisines" to store.cuisines,
                        "imageUrl" to store.imageUrl
                    )
                    db.collection("stores")
                        .document(store.id)
                        .set(storePayload, SetOptions.merge())
                }
                Log.d(TAG, "Stores catalogue synced to Cloud Firestore.")
            } catch (e: Exception) {
                Log.w(TAG, "Could not seed stores to Firestore: ${e.message}")
            }
        }
    }
}
