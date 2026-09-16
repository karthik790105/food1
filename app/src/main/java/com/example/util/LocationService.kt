package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.math.roundToInt

data class CurrentLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val fullAddress: String,
    val area: String,
    val city: String,
    val postalCode: String
)

/**
 * Production-grade Google Play Services Location Integration.
 * Manages FusedLocationProviderClient queries, runtime permissions check,
 * reverse geocoding via Android Geocoder (with backward and modern API 33+ compatibility),
 * and dynamic distance/ETA/fee computations.
 */
class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        onSuccess: (CurrentLocationInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                reverseGeocode(location.latitude, location.longitude, onSuccess)
            } else {
                // Fallback to last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        reverseGeocode(lastLoc.latitude, lastLoc.longitude, onSuccess)
                    } else {
                        // Fallback default coordinates (Bengaluru Center)
                        reverseGeocode(12.9716, 77.5946, onSuccess)
                    }
                }.addOnFailureListener {
                    reverseGeocode(12.9716, 77.5946, onSuccess)
                }
            }
        }.addOnFailureListener { exception ->
            // Try last known
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                if (lastLoc != null) {
                    reverseGeocode(lastLoc.latitude, lastLoc.longitude, onSuccess)
                } else {
                    onError(exception.message ?: "Failed to acquire GPS location")
                }
            }.addOnFailureListener {
                onError(exception.message ?: "Failed to acquire GPS location")
            }
        }
    }

    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        onSuccess: (CurrentLocationInfo) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val info = parseAddressList(latitude, longitude, addresses)
                        onSuccess(info)
                    }

                    override fun onError(errorMessage: String?) {
                        onSuccess(createFallbackLocation(latitude, longitude))
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val info = parseAddressList(latitude, longitude, addresses)
                onSuccess(info)
            }
        } catch (e: Exception) {
            onSuccess(createFallbackLocation(latitude, longitude))
        }
    }

    private fun parseAddressList(
        latitude: Double,
        longitude: Double,
        addresses: List<Address>?
    ): CurrentLocationInfo {
        val address = addresses?.firstOrNull() ?: return createFallbackLocation(latitude, longitude)
        val area = address.subLocality ?: address.thoroughfare ?: address.locality ?: "Current Location"
        val city = address.locality ?: address.subAdminArea ?: "Bengaluru"
        val full = address.getAddressLine(0) ?: "$area, $city"
        val postal = address.postalCode ?: ""

        return CurrentLocationInfo(
            latitude = latitude,
            longitude = longitude,
            title = area,
            fullAddress = full,
            area = area,
            city = city,
            postalCode = postal
        )
    }

    private fun createFallbackLocation(latitude: Double, longitude: Double): CurrentLocationInfo {
        return CurrentLocationInfo(
            latitude = latitude,
            longitude = longitude,
            title = "Current Location",
            fullAddress = "Near Current GPS Coordinates (${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)})",
            area = "Current Area",
            city = "Bengaluru",
            postalCode = "560001"
        )
    }

    companion object {
        fun calculateDistanceKm(
            fromLat: Double,
            fromLng: Double,
            toLat: Double,
            toLng: Double
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
            val distanceInKm = results[0] / 1000.0
            return (distanceInKm * 10).roundToInt() / 10.0
        }

        fun calculateDeliveryTimeMin(distanceKm: Double): Int {
            val computed = 15 + (distanceKm * 4).roundToInt()
            return maxOf(15, computed)
        }

        fun calculateDeliveryFee(distanceKm: Double): Double {
            val fee = if (distanceKm <= 2.0) 25.0 else 25.0 + ((distanceKm - 2.0) * 8.0)
            return (fee * 10).roundToInt() / 10.0
        }
    }
}
