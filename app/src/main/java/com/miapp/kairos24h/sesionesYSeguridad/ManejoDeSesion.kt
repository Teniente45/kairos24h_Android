/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

package com.miapp.kairos24h.sesionesYSeguridad

import android.util.Log
import android.webkit.WebView
import android.os.Handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ManejoDeSesion {

    // Marca el estado de la app como en pausa. Ideal para logging y control de sesión.
    fun onPause() {
        Log.d("ManejoDeSesion", "Aplicación en pausa")
    }

    // Marca la app como detenida y simula inactividad dentro del WebView mediante JavaScript.
    fun onStop(webView: WebView?) {
        Log.d("ManejoDeSesion", "Aplicación detenida")
        webView?.evaluateJavascript(
            """
            (function() {
                console.log("Simulación de inactividad - onStop");
            })();
            """.trimIndent(),
            null
        )
    }

    // Marca la reanudación de la app y simula reactivación en el WebView con un evento JavaScript.
    fun onResume(webView: WebView?) {
        Log.d("ManejoDeSesion", "Aplicación reanudada")
        webView?.evaluateJavascript(
            """
            (function() {
                console.log("Simulación de reactivación - onResume");
            })();
            """.trimIndent(),
            null
        )
    }

    // Obtiene la fecha y hora actual desde la cabecera HTTP de una web externa (Google).
    suspend fun obtenerFechaHoraInternet(): Date? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url("https://www.google.com").build()
            val response = client.newCall(request).execute()
            val dateHeader = response.header("Date")
            dateHeader?.let {
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
                sdf.parse(it)
            }
        } catch (e: Exception) {
            Log.e("FechaInternet", "Error al obtener fecha: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Inicia un temporizador que simula actividad periódica en el WebView para evitar cierre de sesión por inactividad.
    fun startActivitySimulationTimer(handler: Handler, webView: WebView?, sessionTimeoutMillis: Long) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("ManejoDeSesion", "Simulando actividad en WebView después de $sessionTimeoutMillis ms de inactividad")
                webView?.evaluateJavascript(
                    """
                (function() {
                    var event = new MouseEvent('mousemove', {
                        bubbles: true,
                        cancelable: true,
                        view: window,
                        clientX: Math.random() * window.innerWidth,
                        clientY: Math.random() * window.innerHeight
                    });
                    document.body.dispatchEvent(event);
                })();
                """.trimIndent(),
                    null
                )
                handler.postDelayed(this, sessionTimeoutMillis)
            }
        }, sessionTimeoutMillis)
    }
}