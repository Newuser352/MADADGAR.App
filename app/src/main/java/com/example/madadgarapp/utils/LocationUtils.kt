package com.example.madadgarapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

/**
 * Utility class for location-related operations
 */
object LocationUtils {
    
    private const val TAG = "LocationUtils"
    
    /**
     * Data class to represent coordinates
     */
    data class Coordinates(
        val latitude: Double,
        val longitude: Double
    ) {
        override fun toString(): String = "$latitude,$longitude"
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location using FusedLocationProviderClient
     */
    suspend fun getCurrentLocation(context: Context): Coordinates? {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10000L // 10 seconds
                ).apply {
                    setMinUpdateDistanceMeters(10f)
                    setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    setWaitForAccurateLocation(true)
                }.build()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val coordinates = Coordinates(location.latitude, location.longitude)
                        Log.d(TAG, "Current location: $coordinates")
                        continuation.resume(coordinates)
                    } else {
                        Log.w(TAG, "Location is null")
                        continuation.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get current location", exception)
                    continuation.resumeWithException(exception)
                }
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when getting location", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     * @param lat1 First latitude
     * @param lon1 First longitude
     * @param lat2 Second latitude
     * @param lon2 Second longitude
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate distance between two Coordinates objects
     */
    fun calculateDistance(coord1: Coordinates, coord2: Coordinates): Double {
        return calculateDistance(coord1.latitude, coord1.longitude, coord2.latitude, coord2.longitude)
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
            else -> "${distanceKm.toInt()} km"
        }
    }
    
    /**
     * Check if coordinates are within a certain radius
     */
    fun isWithinRadius(
        center: Coordinates,
        point: Coordinates,
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(center, point)
        return distance <= radiusKm
    }
    
    /**
     * Parse coordinates from string format "lat,lng"
     */
    fun parseCoordinates(coordinateString: String?): Coordinates? {
        return try {
            if (coordinateString.isNullOrBlank()) return null
            
            val parts = coordinateString.split(",")
            if (parts.size != 2) return null
            
            val lat = parts[0].toDouble()
            val lng = parts[1].toDouble()
            
            Coordinates(lat, lng)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing coordinates: $coordinateString", e)
            null
        }
    }
    
    /**
     * Get address from coordinates (reverse geocoding)
     */
    fun getAddressFromCoordinates(
        context: Context,
        coordinates: Coordinates
    ): String {
        return try {
            val geocoder = android.location.Geocoder(context)
            val addresses = geocoder.getFromLocation(
                coordinates.latitude,
                coordinates.longitude,
                1
            )
            
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                buildString {
                    address.thoroughfare?.let { append("$it, ") }
                    address.locality?.let { append("$it, ") }
                    address.adminArea?.let { append(it) }
                }
            } else {
                "${coordinates.latitude}, ${coordinates.longitude}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address from coordinates", e)
            "${coordinates.latitude}, ${coordinates.longitude}"
        }
    }
    
    /**
     * Required permissions for location features
     */
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}
