package com.miapp.kairos24h.sesionesYSeguridad

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log

object GPSUtils {

    // Obtiene la latitud desde el GPS o proveedor de red
    @SuppressLint("MissingPermission")
    fun obtenerLatitud(context: Context): Double {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            location?.latitude ?: 0.0
        } catch (e: Exception) {
            Log.e("GPSUtils", "Error al obtener latitud: ${e.message}")
            0.0
        }
    }

    // Obtiene la longitud desde el GPS o proveedor de red
    @SuppressLint("MissingPermission")
    fun obtenerLongitud(context: Context): Double {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            location?.longitude ?: 0.0
        } catch (e: Exception) {
            Log.e("GPSUtils", "Error al obtener longitud: ${e.message}")
            0.0
        }
    }
}