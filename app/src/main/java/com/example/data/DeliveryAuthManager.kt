package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DeliveryPartner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages Delivery Partner registration, authentication credentials,
 * duty status (Online/Offline), and session persistence.
 */
class DeliveryAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("delivery_partner_auth_prefs", Context.MODE_PRIVATE)

    // Removed pre-registered demo riders as requested
    private val _registeredPartners = mutableListOf<DeliveryPartner>()
    private val _partnersStateFlow = MutableStateFlow<List<DeliveryPartner>>(emptyList())
    val allPartnersFlow: StateFlow<List<DeliveryPartner>> = _partnersStateFlow.asStateFlow()

    private val _suspendedRiders = MutableStateFlow<Set<String>>(emptySet())
    val suspendedRiders: StateFlow<Set<String>> = _suspendedRiders.asStateFlow()

    fun isRiderSuspended(riderId: String): Boolean = _suspendedRiders.value.contains(riderId)

    fun toggleRiderSuspension(riderId: String): Boolean {
        val current = _suspendedRiders.value.toMutableSet()
        val isSuspended = if (current.contains(riderId)) {
            current.remove(riderId)
            false
        } else {
            current.add(riderId)
            true
        }
        _suspendedRiders.value = current
        return isSuspended
    }

    private val _currentPartner = MutableStateFlow<DeliveryPartner?>(null)
    val currentPartner: StateFlow<DeliveryPartner?> = _currentPartner.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val wasExplicitLogout = prefs.getBoolean("KEY_EXPLICIT_LOGOUT", false)
        val savedIds = prefs.getStringSet("KEY_SAVED_PARTNER_IDS", emptySet()) ?: emptySet()

        savedIds.forEach { pid ->
            val name = prefs.getString("KEY_PARTNER_NAME_$pid", null)
            val phone = prefs.getString("KEY_PARTNER_PHONE_$pid", null)
            val email = prefs.getString("KEY_PARTNER_EMAIL_$pid", "") ?: ""
            val pwd = prefs.getString("KEY_PARTNER_PWD_$pid", "1234") ?: "1234"
            val vehicle = prefs.getString("KEY_PARTNER_VEHICLE_$pid", "Motorcycle") ?: "Motorcycle"
            val vehicleNum = prefs.getString("KEY_PARTNER_VEHICLE_NUM_$pid", "") ?: ""
            val dl = prefs.getString("KEY_PARTNER_DL_$pid", "") ?: ""
            val city = prefs.getString("KEY_PARTNER_CITY_$pid", "Bangalore") ?: "Bangalore"
            val totalDeliveries = prefs.getInt("KEY_PARTNER_DELIVERIES_$pid", 0)
            val earningsToday = prefs.getFloat("KEY_PARTNER_EARNINGS_$pid", 0f).toDouble()
            val isOnline = prefs.getBoolean("KEY_PARTNER_ONLINE_$pid", true)

            if (name != null && phone != null) {
                val partner = DeliveryPartner(
                    id = pid,
                    name = name,
                    phone = phone,
                    email = email,
                    password = pwd,
                    rating = 5.0,
                    vehicle = vehicle,
                    vehicleNumber = vehicleNum,
                    drivingLicense = dl,
                    totalDeliveries = totalDeliveries,
                    earningsToday = earningsToday,
                    isOnline = isOnline,
                    activeCity = city
                )
                if (_registeredPartners.none { it.id == pid }) {
                    _registeredPartners.add(partner)
                }
            }
        }

        val currentPartnerId = prefs.getString("KEY_CURRENT_PARTNER_ID", null)
        if (!wasExplicitLogout && currentPartnerId != null) {
            val partner = _registeredPartners.find { it.id == currentPartnerId }
            _currentPartner.value = partner
        } else {
            _currentPartner.value = null
        }
        _partnersStateFlow.value = _registeredPartners.toList()
    }

    val registeredPartners: List<DeliveryPartner>
        get() = _registeredPartners.toList()

    fun login(identifier: String, password: String): Pair<Boolean, String> {
        val cleanIdentifier = identifier.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanIdentifier.isBlank()) {
            return Pair(false, "Please enter your registered mobile number or email.")
        }
        if (cleanPassword.isBlank()) {
            return Pair(false, "Please enter your password.")
        }

        if (_registeredPartners.isEmpty()) {
            return Pair(false, "No registered delivery partners found. Please switch to the 'Register as Partner' tab to create an account.")
        }

        val partner = _registeredPartners.find {
            (it.phone.contains(cleanIdentifier) || it.email.lowercase() == cleanIdentifier) &&
                    (it.password == cleanPassword || it.password.isBlank())
        }

        return if (partner != null) {
            _currentPartner.value = partner
            saveSession(partner)
            Pair(true, "Welcome back, ${partner.name}!")
        } else {
            Pair(false, "Invalid credentials. Please verify your phone number and password or register a new account.")
        }
    }

    fun registerPartner(
        name: String,
        phone: String,
        email: String,
        password: String,
        vehicle: String,
        vehicleNumber: String,
        drivingLicense: String,
        city: String
    ): Pair<Boolean, String> {
        if (name.isBlank()) return Pair(false, "Please enter your full name.")
        if (phone.length < 10) return Pair(false, "Please enter a valid 10-digit mobile number.")
        if (vehicleNumber.isBlank()) return Pair(false, "Please enter your vehicle registration number.")
        if (drivingLicense.isBlank()) return Pair(false, "Please enter your driving license number.")

        val newPartnerId = "dp_" + UUID.randomUUID().toString().take(8)
        val newPartner = DeliveryPartner(
            id = newPartnerId,
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            password = if (password.isNotBlank()) password.trim() else "1234",
            rating = 5.0,
            vehicle = vehicle.ifBlank { "Motorcycle / Bike" },
            vehicleNumber = vehicleNumber.trim().uppercase(),
            drivingLicense = drivingLicense.trim().uppercase(),
            totalDeliveries = 0,
            earningsToday = 0.0,
            isOnline = true,
            activeCity = city.ifBlank { "Bangalore" }
        )

        _registeredPartners.add(newPartner)
        _partnersStateFlow.value = _registeredPartners.toList()
        _currentPartner.value = newPartner
        saveSession(newPartner)
        return Pair(true, "Rider registered successfully! You are now ONLINE.")
    }

    fun toggleDutyStatus(): Boolean {
        val current = _currentPartner.value ?: return false
        val updated = current.copy(isOnline = !current.isOnline)
        _currentPartner.value = updated
        saveSession(updated)
        return updated.isOnline
    }

    fun addTripEarnings(amount: Double) {
        val current = _currentPartner.value ?: return
        val updated = current.copy(
            earningsToday = current.earningsToday + amount,
            totalDeliveries = current.totalDeliveries + 1
        )
        _currentPartner.value = updated
        saveSession(updated)
    }

    fun logout() {
        prefs.edit()
            .remove("KEY_CURRENT_PARTNER_ID")
            .putBoolean("KEY_EXPLICIT_LOGOUT", true)
            .apply()
        _currentPartner.value = null
    }

    private fun saveSession(partner: DeliveryPartner) {
        val savedIds = (prefs.getStringSet("KEY_SAVED_PARTNER_IDS", emptySet()) ?: emptySet()).toMutableSet()
        savedIds.add(partner.id)

        prefs.edit()
            .putBoolean("KEY_EXPLICIT_LOGOUT", false)
            .putStringSet("KEY_SAVED_PARTNER_IDS", savedIds)
            .putString("KEY_CURRENT_PARTNER_ID", partner.id)
            .putString("KEY_PARTNER_NAME_${partner.id}", partner.name)
            .putString("KEY_PARTNER_PHONE_${partner.id}", partner.phone)
            .putString("KEY_PARTNER_EMAIL_${partner.id}", partner.email)
            .putString("KEY_PARTNER_PWD_${partner.id}", partner.password)
            .putFloat("KEY_PARTNER_RATING_${partner.id}", partner.rating.toFloat())
            .putString("KEY_PARTNER_VEHICLE_${partner.id}", partner.vehicle)
            .putString("KEY_PARTNER_VEHICLE_NUM_${partner.id}", partner.vehicleNumber)
            .putString("KEY_PARTNER_DL_${partner.id}", partner.drivingLicense)
            .putInt("KEY_PARTNER_DELIVERIES_${partner.id}", partner.totalDeliveries)
            .putFloat("KEY_PARTNER_EARNINGS_${partner.id}", partner.earningsToday.toFloat())
            .putBoolean("KEY_PARTNER_ONLINE_${partner.id}", partner.isOnline)
            .putString("KEY_PARTNER_CITY_${partner.id}", partner.activeCity)
            .apply()
    }
}
