package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.NetworkInterface

import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.location.Location

object SeguridadUtils {

    enum class ResultadoUbicacion {
        OK,
        GPS_DESACTIVADO,
        UBICACION_SIMULADA
    }

    fun isUsingVPN(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                } else {
                    false
                }
            } else {
                // En versiones más antiguas no lo podemos detectar con certeza, así que asumimos falso positivo.
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("Seguridad", "Error al verificar VPN: ${e.message}")
            false
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    /**
     * Esta función ahora simplemente devuelve false por defecto.
     * Para la detección real, debe llamarse a detectarUbicacionReal(context) desde una corrutina.
     */
    fun isMockLocationEnabled(context: Context): Boolean {
        // El consumidor debe llamar a detectarUbicacionReal en un entorno suspendido.
        return false
    }

    /**
     * Verifica si la ubicación activa es real (no simulada) usando FusedLocationProviderClient.
     * Debe llamarse desde una corrutina.
     */
    suspend fun detectarUbicacionReal(context: Context): ResultadoUbicacion {
        return try {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                android.util.Log.e("Seguridad", "Permiso ACCESS_FINE_LOCATION no concedido")
                return ResultadoUbicacion.UBICACION_SIMULADA
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                android.util.Log.e("Seguridad", "GPS desactivado")
                return ResultadoUbicacion.GPS_DESACTIVADO
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }

            if (location != null && !location.isFromMockProvider) {
                android.util.Log.i("Seguridad", "Ubicación válida detectada")
                ResultadoUbicacion.OK
            } else {
                android.util.Log.w("Seguridad", "Ubicación simulada o nula detectada")
                ResultadoUbicacion.UBICACION_SIMULADA
            }
        } catch (e: Exception) {
            android.util.Log.e("Seguridad", "Error al obtener ubicación en tiempo real: ${e.message}")
            ResultadoUbicacion.UBICACION_SIMULADA
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun checkSecurity(
        context: Context,
        validarGPS: Boolean,
        validarIP: Boolean,
        onShowAlert: (String) -> Unit
    ): Boolean {
        if (validarGPS) {
            if (!hasLocationPermission(context)) {
                Log.e("Seguridad", "GPS obligatorio, pero sin permiso")
                onShowAlert("PROBLEMA GPS")
                return false
            }

            when (detectarUbicacionReal(context)) {
                ResultadoUbicacion.GPS_DESACTIVADO -> {
                    Log.e("Seguridad", "GPS desactivado")
                    onShowAlert("PROBLEMA GPS")
                    return false
                }
                ResultadoUbicacion.UBICACION_SIMULADA -> {
                    Log.e("Seguridad", "Ubicación simulada detectada")
                    onShowAlert("UBICACIÓN SIMULADA")
                    return false
                }
                ResultadoUbicacion.OK -> { /* continuar */ }
            }
        }

        if (validarIP) {
            if (isUsingVPN(context)) {
                Log.e("Seguridad", "VPN detectada y uso de IP obligatorio")
                onShowAlert("VPN DETECTADA")
                return false
            }
        }

        return true
    }
}