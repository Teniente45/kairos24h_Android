package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.Toast


object ManejoDeSesion {
    private val handler = Handler(Looper.getMainLooper())
    private var lastInteractionTime = System.currentTimeMillis()

    fun verificarSeguridadAlIniciar(context: Context) {
        if (SeguridadUtils.isUsingVPN()) {
            Log.w("Seguridad", "El usuario está usando VPN")
        }

        if (!SeguridadUtils.isInternetAvailable(context)) {
            Toast.makeText(context, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
        }

        if (SeguridadUtils.isMockLocationEnabled(context)) {
            Toast.makeText(context, "Ubicación simulada detectada", Toast.LENGTH_LONG).show()
        }
    }

    fun onPause() {
        lastInteractionTime = System.currentTimeMillis()
        Log.d("Fichar", "onPause: Tiempo de última interacción guardado: $lastInteractionTime")
    }

    fun onStop(webView: WebView?) {
        Log.d("Fichar", "onStop: La actividad se detuvo, redirigiendo a Login.")
        webView?.loadUrl(WebViewURL.LOGIN)
            ?: Log.e("Fichar", "Error: WebView no inicializado en onStop()")
    }

    fun onResume(context: Context, webView: WebView?) {
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(context)
        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            webView?.loadUrl(WebViewURL.LOGIN)
        } else {
            val currentTime = System.currentTimeMillis()
            Log.d("Fichar", "onResume: Tiempo actual: $currentTime, Última interacción: $lastInteractionTime")
            if (currentTime - lastInteractionTime > 30000) {
                Log.d("Fichar", "onResume: Ha pasado más de 30 segundos desde la última interacción. Redirigiendo a Login.")
                webView?.loadUrl(WebViewURL.LOGIN)
            } else {
                Log.d("Fichar", "onResume: Detectada actividad reciente, simulando movimiento del mouse.")
                simularActividadWebView(webView)
            }
        }
    }

    private fun simularActividadWebView(webView: WebView?, intervalo: Long = 5000) {
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
        handler.postDelayed({ simularActividadWebView(webView, intervalo) }, intervalo)
    }
}