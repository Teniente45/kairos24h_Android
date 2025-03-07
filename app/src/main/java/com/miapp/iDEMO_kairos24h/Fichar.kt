package com.miapp.iDEMO_kairos24h
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.FichEntrada
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.FichSalida
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.urlServidor
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray


class Fichar : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    // 2 horas en milisegundos
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L
    private var lastInteractionTime = System.currentTimeMillis()
    // Variable para almacenar el WebView creado en Compose
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)

        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin()
            return
        }

        // Usamos las credenciales del Intent, o las almacenadas
        val usuario = intent.getStringExtra("usuario") ?: storedUser
        val password = intent.getStringExtra("password") ?: storedPassword

        setContent {
            FicharScreen(
                usuario = usuario,
                password = password,
                fichajesUrl = urlServidor
            )
        }
        startActivitySimulationTimer()
    }

    // Inicia un temporizador para simular actividad en la WebView cada 2 horas
    private fun startActivitySimulationTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("Fichar", "Simulando actividad en WebView después de 2 horas de inactividad")
                simulateActivityInWebView() // Llama a la función que simula actividad en la WebView
                handler.postDelayed(this, sessionTimeoutMillis) // Repite la acción cada 2 horas
            }
        }, sessionTimeoutMillis)
    }
    // Simula actividad en la WebView moviendo el cursor en la página
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
    // Se ejecuta cuando la actividad entra en pausa, guardando el tiempo de la última interacción
    override fun onPause() {
        super.onPause()
        lastInteractionTime = System.currentTimeMillis()
        Log.d("Fichar", "onPause: Tiempo de última interacción guardado: $lastInteractionTime")
    }
    // Se ejecuta cuando la actividad se detiene, redirigiendo al usuario a la pantalla de login
    override fun onStop() {
        super.onStop()
        Log.d("Fichar", "onStop: La actividad se detuvo, redirigiendo a Login.")
        navigateToLogin()
    }
    // Se ejecuta cuando la actividad se reanuda. Comprueba las credenciales y la actividad reciente
    override fun onResume() {
        super.onResume()
        // Recupera las credenciales almacenadas
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)
        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin() // Si no hay credenciales, redirige al login
        } else {
            val currentTime = System.currentTimeMillis()
            Log.d("Fichar", "onResume: Tiempo actual: $currentTime, Última interacción: $lastInteractionTime")

            // Si han pasado más de 30 segundos desde la última interacción, redirige al login
            if (currentTime - lastInteractionTime > 30000) {
                Log.d("Fichar", "onResume: Ha pasado más de un segundo desde la última interacción. Redirigiendo a Login.")
                navigateToLogin()
            } else {
                Log.d("Fichar", "onResume: Detectada actividad reciente, simulando movimiento del mouse.")
                simulateMouseMovementInWebView() // Simula movimiento del mouse en la WebView
            }
        }
    }
    // Simula movimiento del mouse en la WebView para evitar la expiración de sesión
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
                handler.postDelayed(this, 5000) // Repite la acción cada 5 segundos
            }
        }, 5000)
    }
    // Redirige al usuario a la pantalla de login y limpia la actividad actual
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza la actividad actual para evitar volver atrás
    }
}

// 🔥 Borra las credenciales almacenadas en SharedPreferences
private fun clearCredentials(context: Context) {
    val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        remove("usuario")
        remove("password")
        apply()
    }
}
// 🔥 Elimina las cookies y credenciales antes de redirigir al usuario al login
private fun clearCookiesAndClearCredentials(view: WebView?) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
    clearCredentials(view?.context ?: return) // Borra credenciales si la vista es válida

    // Redirige al usuario a la pantalla de inicio de sesión
    view.context?.let {
        val intent = Intent(it, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        it.startActivity(intent)
    }
}

suspend fun obtenerFichajesDesdeServidor(url: String): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                return@withContext emptyList() // Si falla, devuelve lista vacía
            }

            // 🔥 Convertimos la respuesta JSON en una lista de Strings
            val jsonArray = JSONArray(responseBody)
            List(jsonArray.length()) { index -> jsonArray.getString(index) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // En caso de error, devolvemos lista vacía
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(usuario: String, password: String, fichajesUrl: String) {
    var isLoading by remember { mutableStateOf(true) }
    var showCuadroParaFichar by remember { mutableStateOf(true) } // 🔥 Controla si se muestra el cuadro para fichar
    var fichajes by remember { mutableStateOf<List<String>>(emptyList()) }

    // ✅ Guardamos la referencia del WebView
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    // 🔥 Cargar fichajes desde la URL del servidor
    LaunchedEffect(fichajesUrl) {
        fichajes = obtenerFichajesDesdeServidor(fichajesUrl)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🔥 WebView con menor z-index (al fondo)
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    val handler = Handler(Looper.getMainLooper())
                    var loginTimeoutRunnable: Runnable? = null

                    settings.javaScriptEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            if (url == WebViewURL.LOGIN) {
                                loginTimeoutRunnable = Runnable {
                                    clearCookiesAndClearCredentials(view)
                                }
                                handler.postDelayed(loginTimeoutRunnable!!, 4000)
                            } else {
                                if (loginTimeoutRunnable != null) {
                                    handler.removeCallbacks(loginTimeoutRunnable!!)
                                    loginTimeoutRunnable = null
                                }
                            }

                            // 🔥 Inyectar JavaScript para autocompletar usuario y contraseña
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    document.getElementsByName('LoginForm[username]')[0].value = '$usuario';
                                    document.getElementsByName('LoginForm[password]')[0].value = '$password';
                                    document.querySelector('form').submit();
                                })();
                                """.trimIndent(),
                                null
                            )
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url.toString()
                            if (url.contains("site/logout")) {
                                clearCookiesAndClearCredentials(view)
                                return true
                            }
                            return super.shouldOverrideUrlLoading(view, request)
                        }
                    }
                    loadUrl(WebViewURL.LOGIN)

                    // ✅ Guardamos la referencia del WebView
                    webViewState.value = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f) // 🔥 Capa más baja (fondo)
        )

        // 🔥 Cuadro para Fichar (Visible al inicio, sobre WebView, pero debajo de la barra de navegación)
        CuadroParaFichar(
            isVisible = showCuadroParaFichar,
            onDismiss = { showCuadroParaFichar = false },
            fichajes = fichajes, // 🔥 Pasamos los fichajes obtenidos del servidor
            modifier = Modifier.zIndex(2f)
        )

        // 🔥 Barra de navegación (Siempre visible en la parte superior)
        BottomNavigationBar(
            onNavigate = { url -> webViewState.value?.loadUrl(url) },
            onToggleFichar = { showCuadroParaFichar = !showCuadroParaFichar },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(3f)
        )

        // 🔥 Animación de carga
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

suspend fun enviarFichaje(url: String) {
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d("Fichaje", "Fichaje registrado con éxito en: $url")
            } else {
                Log.e("Fichaje", "Error en fichaje: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("Fichaje", "Error en la conexión: ${e.message}")
        }
    }
}


@Composable
fun CuadroParaFichar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    fichajes: List<String>,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // 🔥 Botón Verde "Fichaje Entrada"
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            enviarFichaje(FichEntrada) // 🔥 Usa la variable en lugar de la URL directa
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Color verde
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text("Fichaje Entrada", color = Color.White)
                }

                // 🔥 Botón Rojo "Fichaje Salida"
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            enviarFichaje(FichSalida) // 🔥 Usa la variable en lugar de la URL directa
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Color rojo
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text("Fichaje Salida", color = Color.White)
                }

                // 🔥 Contenedor para "Fichajes del Día"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(width = 2.dp, color = Color.Blue)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Fichajes del Día",
                            color = Color.Blue,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 🔥 Lista de fichajes
                        fichajes.forEach { fichaje ->
                            Text(text = fichaje, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    onNavigate: (String) -> Unit,
    onToggleFichar: () -> Unit, // ✅ Se añade esta función para alternar el cuadro de fichar
    modifier: Modifier = Modifier
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E4E5))
            .padding(8.dp)
            .zIndex(3f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {
                    isChecked = !isChecked
                    onToggleFichar() // ✅ Ahora alterna la visibilidad del cuadro de fichar
                }
            ) {
                Icon(
                    painter = painterResource(id = if (isChecked) R.drawable.ic_home32 else R.drawable.ic_home32_2),
                    contentDescription = "Fichar",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = "Fichar",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        NavigationButton("Fichajes", R.drawable.ic_fichajes32) { onNavigate(WebViewURL.Fichaje) }
        NavigationButton("Incidencias", R.drawable.ic_incidencia32) { onNavigate(WebViewURL.Incidencia) }
        NavigationButton("Horarios", R.drawable.ic_horario32) { onNavigate(WebViewURL.Horarios) }
    }
}


// 🔥 Botón de navegación con ícono e interacción
// ✅ Se añade `onClick` como parámetro para los botones de navegación
@Composable
fun NavigationButton(text: String, iconResId: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = text,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
        }
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}








