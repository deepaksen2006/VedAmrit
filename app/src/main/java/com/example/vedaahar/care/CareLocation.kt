package com.example.vedaahar.care

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** One foreground fix only; no background tracking or stale last-known-location fallback. */
suspend fun currentCareLocation(context: Context): CarePoint? = withTimeoutOrNull(15_000) {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return@withTimeoutOrNull null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = try {
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { (fine || it != LocationManager.GPS_PROVIDER) && manager.isProviderEnabled(it) }
    } catch (_: SecurityException) { return@withTimeoutOrNull null }
    if (providers.isEmpty()) return@withTimeoutOrNull null
    suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(CarePoint(location.latitude, location.longitude).takeIf { it.isValid })
            }
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Legacy Android callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        try {
            providers.forEach { provider ->
                @Suppress("MissingPermission", "DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
            manager.removeUpdates(listener)
            if (continuation.isActive) continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            manager.removeUpdates(listener)
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
