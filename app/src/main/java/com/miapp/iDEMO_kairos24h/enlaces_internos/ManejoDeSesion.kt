package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.content.Context
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

    /**
     * Llama cuando la actividad entra en pausa.
     */
    fun onPause() {
        Log.d("ManejoDeSesion", "Aplicación en pausa")
    }

    /**
     * Llama cuando la actividad se detiene.
     * @param webView instancia del WebView en uso para ejecutar scripts de simulación.
     */
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

    /**
     * Llama cuando la actividad se reanuda.
     * @param context contexto actual de la aplicación.
     * @param webView instancia del WebView en uso para ejecutar scripts de reactivación.
     */
    fun onResume(context: Context, webView: WebView?) {
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

    /**
     * Obtiene la fecha y hora del servidor de Google.
     * @return instancia de [Date] si se pudo obtener correctamente, o null si ocurrió un error.
     */
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