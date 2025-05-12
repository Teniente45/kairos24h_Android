@file:Suppress("DEPRECATION")

package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
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
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Seguridad", "Error al verificar VPN: ${e.message}")
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
    fun isMockLocationEnabled(): Boolean {
        // El consumidor debe llamar a detectarUbicacionReal en un entorno suspendido.
        return false
    }

    /**
     * Verifica si la ubicación activa es real (no simulada) usando FusedLocationProviderClient.
     * Debe llamarse desde una corrutina.
     */
    suspend fun detectarUbicacionReal(context: Context): ResultadoUbicacion {
        try {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                Log.e("Seguridad", "Permiso ACCESS_FINE_LOCATION no concedido")
                return ResultadoUbicacion.GPS_DESACTIVADO
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.e("Seguridad", "GPS desactivado por el usuario")
                return ResultadoUbicacion.GPS_DESACTIVADO
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 0
                fastestInterval = 0
                numUpdates = 1
            }

            val location = kotlinx.coroutines.withTimeoutOrNull(1000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val callback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            Log.d("Seguridad", "onLocationResult recibido")
                            cont.resume(result.lastLocation)
                            fusedClient.removeLocationUpdates(this)
                        }

                        override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                            Log.d("Seguridad", "onLocationAvailability: ${availability.isLocationAvailable}")
                            if (!availability.isLocationAvailable) {
                                cont.resume(null)
                                fusedClient.removeLocationUpdates(this)
                            }
                        }
                    }

                    fusedClient.requestLocationUpdates(locationRequest, callback, null)
                }
            } ?: suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }

            if (location != null) {
                return if (location.isFromMockProvider) {
                    Log.w("Seguridad", "Ubicación simulada detectada")
                    ResultadoUbicacion.UBICACION_SIMULADA
                } else {
                    Log.i("Seguridad", "Ubicación válida detectada (posiblemente de respaldo)")
                    ResultadoUbicacion.OK
                }
            }

            Log.e("Seguridad", "No se pudo obtener ubicación ni siquiera con respaldo")
            return ResultadoUbicacion.GPS_DESACTIVADO

        } catch (e: Exception) {
            Log.e("Seguridad", "Error al obtener ubicación: ${e.message}")
            return ResultadoUbicacion.GPS_DESACTIVADO
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun checkSecurity(
        context: Context,
        lComGPS: String,
        lComIP: String,
        lBotonesFichajeMovil: String,
        onShowAlert: (String) -> Unit
    ): Boolean {
        val validarGPS = lComGPS == "S"
        val validarIP = lComIP == "S"
        val mostrarBotones = lBotonesFichajeMovil == "S"

        Log.d("Seguridad", "Validaciones: GPS=$validarGPS, IP=$validarIP, Botones=$mostrarBotones")

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
                ResultadoUbicacion.UBICACION_SIMULADA,
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