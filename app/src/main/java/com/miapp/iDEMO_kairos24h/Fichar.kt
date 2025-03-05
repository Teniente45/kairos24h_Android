package com.miapp.iDEMO_kairos24h

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.delay

class Fichar : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L
    private var lastInteractionTime = System.currentTimeMillis()
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (storedUser, storedPassword) = if (android.os.Build.VERSION.SDK_INT == 23) {
            AuthManager.getUserCredentialsPair(this)
        } else {
            val (user, pass, _) = AuthManager.getUserCredentials(this)
            Pair(user, pass)
        }

        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin()
            return
        }

        val usuario = intent.getStringExtra("usuario") ?: storedUser
        val password = intent.getStringExtra("password") ?: storedPassword

        setContent {
            FicharScreen(usuario = usuario, password = password)
        }

        startActivitySimulationTimer()
    }
    private fun startActivitySimulationTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("Fichar", "Simulando actividad en WebView después de 2 horas de inactividad")
                simulateActivityInWebView()
                handler.postDelayed(this, sessionTimeoutMillis)
            }
        }, sessionTimeoutMillis)
    }

    private fun simulateActivityInWebView() {
        webView?.evaluateJavascript(
            """
            (function() {
                var event = new MouseEvent('mousemove', {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    clientX: 1,
                    clientY: 1
                });
                document.body.dispatchEvent(event);
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onPause() {
        super.onPause()
        lastInteractionTime = System.currentTimeMillis()
        Log.d("Fichar", "onPause: Tiempo de última interacción guardado: $lastInteractionTime")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Fichar", "onStop: La actividad se detuvo, redirigiendo a Login.")
        navigateToLogin()
    }

    override fun onResume() {
        super.onResume()
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)
        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin()
        } else {
            val currentTime = System.currentTimeMillis()
            Log.d("Fichar", "onResume: Tiempo actual: $currentTime, Última interacción: $lastInteractionTime")
            if (currentTime - lastInteractionTime > 30000) {
                Log.d("Fichar", "onResume: Ha pasado más de un segundo desde la última interacción. Redirigiendo a Login.")
                navigateToLogin()
            } else {
                Log.d("Fichar", "onResume: Detectada actividad reciente, simulando movimiento del mouse.")
                simulateMouseMovementInWebView()
            }
        }
    }

    private fun simulateMouseMovementInWebView() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("Fichar", "Simulando movimiento del mouse para evitar la expiración de sesión.")
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
                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(usuario: String, password: String) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(6000)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        loadUrl(WebViewURL.LOGIN)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            BottomNavigationBar()
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        val imageView = ImageView(context)
                        Glide.with(context)
                            .load(R.drawable.version_2)
                            .apply(RequestOptions().placeholder(R.drawable.version_2))
                            .into(imageView)
                        imageView
                    },
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var isChecked by remember { mutableStateOf(false) }
        Switch(
            checked = isChecked,
            onCheckedChange = { isChecked = it }
        )

        NavigationButton("Fichaje", WebViewURL.Fichaje, R.drawable.icon_fichaje)
        NavigationButton("Incidencia", WebViewURL.Incidencia, R.drawable.icon_incidencia)
        NavigationButton("Horarios", WebViewURL.Horarios, R.drawable.icon_horarios)
    }
}

@Composable
fun NavigationButton(text: String, url: String, iconResId: Int) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = text,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(text)
    }
}
