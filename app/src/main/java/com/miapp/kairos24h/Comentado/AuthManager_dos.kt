/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

/**
@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.miapp.kairos24h.sesionesYSeguridad

import android.content.Context
import android.util.Log
import com.miapp.kairos24h.Comentado.WebViewURL
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UserCredentials(
    val usuario: String,
    val password: String,
    val xEmpleado: String?,
    val lComGPS: String,
    val lComIP: String,
    val lBotonesFichajeMovil: String,
    val xEntidad: String?,
    val sEmpleado: String,
    val tUrlCPP: String,
    val tLogo: String,
    val cTipEmp: String
)

object AuthManager {

    // Obtener las credenciales del usuario desde SharedPreferences
    fun getUserCredentials(context: Context): UserCredentials {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val usuario = sharedPreferences.getString("usuario", "") ?: ""
        val password = sharedPreferences.getString("password", "") ?: ""
        val xEmpleado = sharedPreferences.getString("xEmpleado", null)
        val lComGPS = sharedPreferences.getString("lComGPS", "N") ?: "N"
        val lComIP = sharedPreferences.getString("lComIP", "N") ?: "N"
        val lBotonesFichajeMovil = sharedPreferences.getString("lBotonesFichajeMovil", "N") ?: "N"
        val xEntidad = sharedPreferences.getString("xEntidad", null)
        val sEmpleado = sharedPreferences.getString("sEmpleado", "") ?: ""
        val tUrlCPP = sharedPreferences.getString("tUrlCPP", "") ?: ""
        val tLogo = sharedPreferences.getString("tLogo", "") ?: ""
        val cTipEmp = sharedPreferences.getString("cTipEmp", "") ?: ""

        Log.d(
            "getUserCredentials",
            "usuario=$usuario, password=$password, xEmpleado=$xEmpleado, lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil, xEntidad=$xEntidad, sEmpleado=$sEmpleado, tUrlCPP=$tUrlCPP, tLogo=$tLogo, cTipEmp=$cTipEmp"
        )

        return UserCredentials(
            usuario,
            password,
            xEmpleado,
            lComGPS,
            lComIP,
            lBotonesFichajeMovil,
            xEntidad,
            sEmpleado,
            tUrlCPP,
            tLogo,
            cTipEmp
        )
    }


    // Guardar las credenciales del usuario en SharedPreferences, junto con xEmpleado y otros flags
    fun saveUserCredentials(
        context: Context,
        usuario: String,
        password: String,
        xEmpleado: String?,
        lComGPS: String,
        lComIP: String,
        lBotonesFichajeMovil: String,
        xEntidad: String?,
        sEmpleado: String,
        tUrlCPP: String,
        tLogo: String,
        cTipEmp: String
    ) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("usuario", usuario)
            putString("password", password)
            if (xEmpleado != null) {
                putString("xEmpleado", xEmpleado)
            }
            putString("lComGPS", lComGPS)
            putString("lComIP", lComIP)
            putString("lBotonesFichajeMovil", lBotonesFichajeMovil)
            if (xEntidad != null) {
                putString("xEntidad", xEntidad)
            }
            putString("sEmpleado", sEmpleado)
            putString("tUrlCPP", tUrlCPP)
            putString("tLogo", tLogo)
            putString("cTipEmp", cTipEmp)
            apply()
        }
        Log.d(
            "saveUserCredentials",
            "usuario=$usuario, password=$password, xEmpleado=$xEmpleado, lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil, xEntidad=$xEntidad, sEmpleado=$sEmpleado, tUrlCPP=$tUrlCPP, tLogo=$tLogo, cTipEmp=$cTipEmp"
        )
    }

    // Método para realizar el login y obtener el xEmpleado y otros flags
    fun authenticateUser(context: Context, usuario: String, password: String): Pair<Boolean, UserCredentials?> {
        val client = OkHttpClient()

        fun intentarLogin(context: Context, host: String): Pair<Boolean, UserCredentials?> {
            val url = WebViewURL.getLoginAPK() +
                    "&cUsuario=$usuario" +
                    "&tPassword=$password"

            Log.d("AuthManager", "Intentando login en: $host")

            val request = Request.Builder().url(url).build()

            return try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("AuthManager", "Response Body: $responseBody")

                    if (!responseBody.isNullOrEmpty()) {
                        val jsonResponse = JSONObject(responseBody)
                        val code = jsonResponse.optInt("code", -1)
                        val xEmpleado = jsonResponse.optString("xEmpleado", null)

                        if (code == 1 && xEmpleado != null) {
                            val credentials = UserCredentials(
                                usuario = usuario,
                                password = password,
                                xEmpleado = xEmpleado,
                                lComGPS = jsonResponse.optString("lComGPS", "S"),
                                lComIP = jsonResponse.optString("lComIP", "S"),
                                lBotonesFichajeMovil = jsonResponse.optString("lBotonesFichajeMovil", "S"),
                                xEntidad = jsonResponse.optString("xEntidad", null),
                                sEmpleado = jsonResponse.optString("sEmpleado", ""),
                                tUrlCPP = jsonResponse.optString("tUrlCPP", ""),
                                tLogo = jsonResponse.optString("tLogo", ""),
                                cTipEmp = jsonResponse.optString("cTipEmp", "")
                            )
                            return Pair(true, credentials)
                        } else {
                            return Pair(false, null)
                        }
                    } else {
                        return Pair(false, null)
                    }
                } else {
                    Log.w("AuthManager", "Respuesta no exitosa: ${response.code}")
                    return Pair(false, null)
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Error de login en $host", e)
                return Pair(false, null)
            }
        }

        val resultadoHost1 = intentarLogin(context, WebViewURL.HOST_1)
        if (resultadoHost1.first) {
            WebViewURL.HOST = WebViewURL.HOST_1
            return resultadoHost1
        }

        val resultadoHost2 = intentarLogin(context, WebViewURL.HOST_2)
        if (resultadoHost2.first) {
            WebViewURL.HOST = WebViewURL.HOST_2
            return resultadoHost2
        }

        return Pair(false, null)
    }


    // Borrar completamente los datos de usuario almacenados en SharedPreferences
    fun clearAllUserData(context: Context) {
        val userPrefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userPrefs.edit().clear().apply()

        val appPrefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        appPrefs.edit().clear().apply()
    }
}
*/