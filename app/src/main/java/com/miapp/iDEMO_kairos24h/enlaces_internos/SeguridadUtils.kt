package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.NetworkInterface

object SeguridadUtils {

    fun isUsingVPN(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces().toList().any {
                it.name.equals("tun0", ignoreCase = true) || it.name.equals("ppp0", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    fun isMockLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e("Seguridad", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
            return false
        }

        return try {
            locationManager.getProviders(true).any { provider ->
                val location = locationManager.getLastKnownLocation(provider)
                location?.isFromMockProvider == true
            }
        } catch (e: SecurityException) {
            Log.e("Seguridad", "Error al verificar ubicación simulada: ${e.message}")
            false
        }
    }
}