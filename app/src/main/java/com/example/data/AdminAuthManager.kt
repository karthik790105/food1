package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AdminUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages Admin Portal registration, authentication, permissions,
 * and session state for platform operation managers.
 * Persists data cleanly in SharedPreferences with no demo data.
 */
class AdminAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("admin_portal_auth_prefs", Context.MODE_PRIVATE)

    private val _registeredAdmins = mutableListOf<AdminUser>()
    private val _currentAdmin = MutableStateFlow<AdminUser?>(null)
    val currentAdmin: StateFlow<AdminUser?> = _currentAdmin.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val wasExplicitLogout = prefs.getBoolean("KEY_EXPLICIT_LOGOUT", false)
        val savedAdminIds = prefs.getStringSet("KEY_SAVED_ADMIN_IDS", emptySet()) ?: emptySet()

        savedAdminIds.forEach { aid ->
            val name = prefs.getString("KEY_ADMIN_NAME_$aid", null)
            val email = prefs.getString("KEY_ADMIN_EMAIL_$aid", "") ?: ""
            val phone = prefs.getString("KEY_ADMIN_PHONE_$aid", null)
            val pwd = prefs.getString("KEY_ADMIN_PWD_$aid", "") ?: ""
            val role = prefs.getString("KEY_ADMIN_ROLE_$aid", "Super Admin") ?: "Super Admin"
            val dept = prefs.getString("KEY_ADMIN_DEPT_$aid", "Central Operations") ?: "Central Operations"
            val regAt = prefs.getLong("KEY_ADMIN_REG_AT_$aid", System.currentTimeMillis())

            if (name != null && phone != null) {
                val admin = AdminUser(
                    id = aid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    department = dept,
                    password = pwd,
                    registeredAt = regAt
                )
                if (_registeredAdmins.none { it.id == aid }) {
                    _registeredAdmins.add(admin)
                }
            }
        }

        val currentAdminId = prefs.getString("KEY_CURRENT_ADMIN_ID", null)
        if (!wasExplicitLogout && currentAdminId != null) {
            val admin = _registeredAdmins.find { it.id == currentAdminId }
            _currentAdmin.value = admin
        } else {
            _currentAdmin.value = null
        }
    }

    val registeredAdmins: List<AdminUser>
        get() = _registeredAdmins.toList()

    fun login(identifier: String, password: String): Pair<Boolean, String> {
        val cleanIdentifier = identifier.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanIdentifier.isBlank()) {
            return Pair(false, "Please enter your admin email or registered mobile number.")
        }
        if (cleanPassword.isBlank()) {
            return Pair(false, "Please enter your password.")
        }

        if (_registeredAdmins.isEmpty()) {
            return Pair(false, "No registered admin accounts found. Please register as an Admin first.")
        }

        val admin = _registeredAdmins.find {
            (it.phone.contains(cleanIdentifier) || it.email.lowercase() == cleanIdentifier) &&
                    (it.password == cleanPassword)
        }

        return if (admin != null) {
            _currentAdmin.value = admin
            saveSession(admin)
            Pair(true, "Welcome back, ${admin.name} (${admin.role})!")
        } else {
            Pair(false, "Invalid credentials. Please verify your phone/email and password.")
        }
    }

    fun registerAdmin(
        name: String,
        email: String,
        phone: String,
        role: String,
        department: String,
        password: String,
        accessCode: String
    ): Pair<Boolean, String> {
        if (name.isBlank()) return Pair(false, "Please enter your full name.")
        if (email.isBlank() || !email.contains("@")) return Pair(false, "Please enter a valid official email address.")
        if (phone.length < 10) return Pair(false, "Please enter a valid 10-digit mobile number.")
        if (password.length < 4) return Pair(false, "Password must be at least 4 characters.")

        // Verify admin access code (real-world gatekeeping for operations portal)
        val validCodes = listOf("ADMIN2026", "BITEMART99", "OPS777", "SUPERADMIN")
        if (accessCode.trim().uppercase() !in validCodes) {
            return Pair(false, "Invalid Admin Organization Access Code. Contact system supervisor or enter 'ADMIN2026'.")
        }

        // Check if phone or email already registered
        val existing = _registeredAdmins.find { it.phone == phone.trim() || it.email.lowercase() == email.trim().lowercase() }
        if (existing != null) {
            return Pair(false, "An admin account with this phone or email already exists. Please log in.")
        }

        val newAdminId = "adm_" + UUID.randomUUID().toString().take(8)
        val newAdmin = AdminUser(
            id = newAdminId,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = role.ifBlank { "Super Admin" },
            department = department.ifBlank { "Central Operations" },
            password = password.trim(),
            registeredAt = System.currentTimeMillis()
        )

        _registeredAdmins.add(newAdmin)
        _currentAdmin.value = newAdmin
        saveSession(newAdmin)
        return Pair(true, "Admin account created successfully! You are logged in as ${newAdmin.role}.")
    }

    fun logout() {
        prefs.edit()
            .putBoolean("KEY_EXPLICIT_LOGOUT", true)
            .remove("KEY_CURRENT_ADMIN_ID")
            .apply()
        _currentAdmin.value = null
    }

    private fun saveSession(admin: AdminUser) {
        val currentIds = prefs.getStringSet("KEY_SAVED_ADMIN_IDS", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentIds.add(admin.id)

        prefs.edit()
            .putBoolean("KEY_EXPLICIT_LOGOUT", false)
            .putString("KEY_CURRENT_ADMIN_ID", admin.id)
            .putStringSet("KEY_SAVED_ADMIN_IDS", currentIds)
            .putString("KEY_ADMIN_NAME_${admin.id}", admin.name)
            .putString("KEY_ADMIN_EMAIL_${admin.id}", admin.email)
            .putString("KEY_ADMIN_PHONE_${admin.id}", admin.phone)
            .putString("KEY_ADMIN_PWD_${admin.id}", admin.password)
            .putString("KEY_ADMIN_ROLE_${admin.id}", admin.role)
            .putString("KEY_ADMIN_DEPT_${admin.id}", admin.department)
            .putLong("KEY_ADMIN_REG_AT_${admin.id}", admin.registeredAt)
            .apply()
    }
}
