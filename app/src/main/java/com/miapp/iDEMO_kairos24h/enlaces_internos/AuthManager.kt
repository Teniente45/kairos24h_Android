package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UserCredentials(
    val usuario: String,
    val password: String,
    val xEmpleado: String?,
    val lComGPS: Boolean,
    val lComIP: Boolean,
    val lBotonesFichajeMovil: Boolean
)

object AuthManager {

    // Obtener las credenciales del usuario desde SharedPreferences
    fun getUserCredentials(context: Context): UserCredentials {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val usuario = sharedPreferences.getString("usuario", "") ?: ""
        val password = sharedPreferences.getString("password", "") ?: ""
        val xEmpleado = sharedPreferences.getString("xEmpleado", null)
        val lComGPS = sharedPreferences.getBoolean("lComGPS", false)
        val lComIP = sharedPreferences.getBoolean("lComIP", false)
        val lBotonesFichajeMovil = sharedPreferences.getBoolean("lBotonesFichajeMovil", false)
        Log.d("getUserCredentials", "Estas son las getUserCredentials que te devuelvo: usuario=$usuario, password=$password, xEmpleado=$xEmpleado, lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil")
        return UserCredentials(usuario, password, xEmpleado, lComGPS, lComIP, lBotonesFichajeMovil)
    }


    // Guardar las credenciales del usuario en SharedPreferences, junto con xEmpleado y otros flags
    fun saveUserCredentials(
        context: Context,
        usuario: String,
        password: String,
        xEmpleado: String?,
        lComGPS: Boolean,
        lComIP: Boolean,
        lBotonesFichajeMovil: Boolean
    ) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("usuario", usuario)
            putString("password", password)
            if (xEmpleado != null) {
                putString("xEmpleado", xEmpleado)
            }
            putBoolean("lComGPS", lComGPS)
            putBoolean("lComIP", lComIP)
            putBoolean("lBotonesFichajeMovil", lBotonesFichajeMovil)
            apply()
        }
        Log.d("saveUserCredentials", "Estas son tus saveUserCredentials: usuario=$usuario, password=$password, xEmpleado=$xEmpleado, lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil")
    }

    // Método para realizar el login y obtener el xEmpleado y otros flags
    fun authenticateUser(context: Context, usuario: String, password: String, s: String): Pair<Boolean, UserCredentials?> {
        val client = OkHttpClient()
        // Se usa cUsuario y tPassword en la URL
        val url = BuildURL.LOGIN +
                "&cUsuario=$usuario" +
                "&tPassword=$password"

        Log.d("AuthManager", "URL: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("AuthManager", "Response Body: $responseBody")
                val jsonResponse = JSONObject(responseBody)
                val code = jsonResponse.optInt("code", -1)
                val xEmpleado = jsonResponse.optString("xEmpleado", null) // Extraer xEmpleado del JSON
                if (code == 1) {
                    val lComGPS = jsonResponse.optBoolean("lComGPS", false)
                    val lComIP = jsonResponse.optBoolean("lComIP", false)
                    val lBotonesFichajeMovil = jsonResponse.optBoolean("lBotonesFichajeMovil", false)
                    val credentials = UserCredentials(usuario, password, xEmpleado, lComGPS, lComIP, lBotonesFichajeMovil)
                    Pair(true, credentials)
                } else {
                    Pair(false, null)
                }
            } else {
                Log.d("AuthManager", "Request failed with status: ${response.code}")
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error de autenticación: ", e)
            Pair(false, null)
        }
    }
}
