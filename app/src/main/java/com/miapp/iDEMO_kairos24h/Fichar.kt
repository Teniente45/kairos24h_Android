package com.miapp.iDEMO_kairos24h

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.prueba
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.urlServidor
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class Fichar : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    // 2 horas en milisegundos
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L
    private var lastInteractionTime = System.currentTimeMillis()
    // Variable para almacenar el WebView creado en Compose
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Fichar", "onCreate iniciado")

        setContent {
            Log.d("Fichar", "setContent ejecutándose")
            Text("Hola, esto es una prueba") // Esto debería aparecer en pantalla
        }
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
// Funcion para obtener los fichajes del dia
suspend fun obtenerFichajesDesdeServidor(url: String): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("Fichaje", "Error en la respuesta: ${response.code} - ${response.message}")
                    return@withContext emptyList()
                }
                // Convertimos la respuesta JSON en una lista de Strings
                val jsonArray = JSONArray(responseBody)
                List(jsonArray.length()) { index -> jsonArray.getString(index) }
            }
        } catch (e: Exception) {
            Log.e("Fichaje", "Error al obtener fichajes: ${e.message}", e)
            emptyList()
        }
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(usuario: String, password: String, fichajesUrl: String) {
    var isLoading by remember { mutableStateOf(true) }
    var showCuadroParaFichar by remember { mutableStateOf(true) }
    var fichajes by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageIndex by remember { mutableStateOf(0) }
    // Estado para almacenar el tipo de fichaje realizado (null = sin alerta)
    var fichajeAlertTipo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Lista de imágenes para el usuario
    val imageList = listOf(
        R.drawable.cliente32,
        R.drawable.cliente32_2,
        R.drawable.cliente32_3,
        R.drawable.cliente32_4,
        R.drawable.cliente32_5,
        R.drawable.cliente32_6
    )

    val webViewState = remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val cUsuario = sharedPreferences.getString("usuario", "Usuario") ?: "Usuario"

    // Se muestra la pantalla de carga durante 4 segundos
    LaunchedEffect(Unit) {
        isLoading = true
        delay(3000)
        isLoading = false
    }

    // Cargar fichajes desde el servidor
    LaunchedEffect(fichajesUrl) {
        fichajes = obtenerFichajesDesdeServidor(fichajesUrl)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E4E5))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor izquierdo (Imagen intercambiable + Usuario)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { imageIndex = (imageIndex + 1) % imageList.size }
                ) {
                    Icon(
                        painter = painterResource(id = imageList[imageIndex]),
                        contentDescription = "Usuario",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                }
                Text(
                    text = cUsuario,
                    color = Color(0xFF7599B6),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // Botón de cierre de sesión
            IconButton(onClick = { clearCookiesAndClearCredentials(webViewState.value) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cerrar32),
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // WebView de fondo
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Autocompletamos el formulario de login
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
                        }
                        loadUrl(WebViewURL.LOGIN)
                        webViewState.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )

            // Pantalla de carga
            LoadingScreen(isLoading = isLoading)

            // Cuadro para fichar (se pasa el callback onFichaje)
            if (showCuadroParaFichar) {
                CuadroParaFichar(
                    isVisible = showCuadroParaFichar,
                    onDismiss = { showCuadroParaFichar = false },
                    fichajes = fichajes,
                    onFichaje = { tipo ->
                        fichajeAlertTipo = tipo
                    },
                    modifier = Modifier.zIndex(2f)
                )
            }

            // Mostrar alerta solo si se ha realizado un fichaje
            fichajeAlertTipo?.let { tipo ->
                MensajeAlerta(
                    tipo = tipo,
                    onClose = { fichajeAlertTipo = null }
                )
            }

            // Barra de navegación inferior
            BottomNavigationBar(
                onNavigate = { url ->
                    isLoading = true
                    showCuadroParaFichar = false // Oculta el cuadro de fichar
                    webViewState.value?.loadUrl(url)
                    scope.launch {
                        delay(3000)
                        isLoading = false
                    }
                },
                onToggleFichar = { showCuadroParaFichar = !showCuadroParaFichar },
                hideCuadroParaFichar = { showCuadroParaFichar = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(3f)
            )
        }
    }
}


// MIRAR BIEN
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
    onFichaje: (tipo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(2f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Mostrar lista de fichajes si existe
                if (fichajes.isNotEmpty()) {
                    Text(text = "Fichajes del Día", color = Color.Blue)
                    fichajes.forEach { fichaje ->
                        Text(text = fichaje, color = Color.DarkGray)
                    }
                }
                Logo_empresa()
                MiHorario()
                BotonesFichajeConPermisos(onFichaje = onFichaje)
                RecuadroFichajesDia()
            }
        }
    }
}


@Composable
fun Logo_empresa() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-10).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_i3data),
            contentDescription = "Logo i3data",
            contentScale = ContentScale.Fit, // Ajusta la imagen para que se vea completa
            modifier = Modifier
                .width(140.dp)
                .height(100.dp)
        )
    }
}

@Composable
fun MiHorario() {
    // Calculamos la fecha actual y la formateamos en español
    val currentDateString = remember {
        val currentDate = Date()
        // Formato: día de la semana, día de mes de mes de año (por ejemplo: "Lunes, 10 de Marzo de 2025")
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        // La primera letra del día se puede poner en mayúscula si es necesario:
        sdf.format(currentDate).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-40).dp)
            .border(width = 1.dp, color = Color(0xFFC0C0C0))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primer renglón: fecha actual centrada
        Text(
            text = currentDateString,
            color = Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        // Segundo renglón: "No Horario" centrado (se puede personalizar según la lógica)
        // Se tendrá que meter aqui la lógics para que pille los fichajes del servidor
        Text(
            text = "No Horario",
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BotonesFichajeConPermisos(onFichaje: (tipo: String) -> Unit) {
    val context = LocalContext.current
    var pendingFichaje by remember { mutableStateOf<String?>(null) }

    // Launcher para solicitar el permiso de ubicación
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingFichaje?.let { tipo ->
                Log.d("Fichaje", "Permiso concedido. Procesando fichaje de: $tipo")
                fichar(context, tipo)
                onFichaje(tipo)
            }
        } else {
            Log.d("Fichaje", "Permiso denegado para ACCESS_FINE_LOCATION")
        }
        pendingFichaje = null
    }

    // BOTÓN ENTRADA
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .height(70.dp)
            .offset(y = (-20).dp)
            .clickable {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    Log.d("Fichaje", "Fichaje Entrada: Permiso ya concedido. Procesando fichaje de ENTRADA")
                    fichar(context, "ENTRADA")
                    onFichaje("ENTRADA")
                } else {
                    Log.d("Fichaje", "Fichaje Entrada: No tiene permiso. Solicitando permiso...")
                    pendingFichaje = "ENTRADA"
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
        color = Color(0xFF4CAF50), // Verde para entrada
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.fichajeetrada32),
                contentDescription = "Imagen Fichaje Entrada",
                modifier = Modifier
                    .padding(start = 15.dp)
                    .height(50.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fichaje Entrada",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // BOTÓN SALIDA
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .offset(y = (-20).dp)
            .height(70.dp)
            .clickable {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    Log.d("Fichaje", "Fichaje Salida: Permiso ya concedido. Procesando fichaje de SALIDA")
                    fichar(context, "SALIDA")
                    onFichaje("SALIDA")
                } else {
                    Log.d("Fichaje", "Fichaje Salida: No tiene permiso. Solicitando permiso...")
                    pendingFichaje = "SALIDA"
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
        color = Color(0xFFD51010), // Rojo para salida
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.fichajesalida32),
                contentDescription = "Imagen Fichaje Salida",
                modifier = Modifier
                    .padding(start = 15.dp)
                    .height(50.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fichaje Salida",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun RecuadroFichajesDia() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fichajes Día",
            color = Color(0xFF7599B6),
            fontSize = 20.sp, // "medium"
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "?",
                color = Color(0xFF7599B6),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "?",
                color = Color(0xFF7599B6),
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun MensajeAlerta(
    tipo: String = "ENTRADA",
    onClose: () -> Unit = {}
) {
    // Se obtiene la fecha y hora actual formateada
    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm'h'", Locale.getDefault()).format(Date())

    // Se define el mensaje en función del tipo de fichaje
    val mensaje = when(tipo.uppercase()) {
        "ENTRADA" -> "Fichaje de Entrada realizado correctamente"
        "SALIDA" -> "Fichaje de Salida realizado correctamente"
        else -> "Fichaje de $tipo realizado correctamente"
    }

    // Usamos un Dialog para mostrar el contenido centrado en la pantalla
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        // Puedes usar Surface o Card para darle estilo al cuadro
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Encabezado del cuadro
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7599B6))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Información",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Mensaje de fichaje
                Text(
                    text = mensaje,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Fecha y hora actual
                Text(
                    text = currentDateTime,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Botón para cerrar la alerta
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7599B6))
                    ) {
                        Text(text = "Cerrar")
                    }

                }
            }
        }
    }
}


private fun fichar(context: Context, tipo: String) {
    // 1) Obtener xEmpleado (suponiendo que AuthManager devuelve (usuario, password, xEmpleado))
    val (_, _, xEmpleado) = AuthManager.getUserCredentials(context)
    if (xEmpleado.isNullOrBlank()) {
        Log.e("Fichar", "xEmpleado es nulo o vacío")
        return
    }

    // 2) Chequea permiso para evitar SecurityException
    val hasPermission = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
        Toast.makeText(context, "Debe de aceptar los permisos de localización GPS para poder fichar en la aplicación" , Toast.LENGTH_SHORT).show()
        return
    }

    // 3) Obtener la última ubicación
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude

            // 4) Construir la fecha y hora actual en formato datetime
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fechaFichaje = sdf.format(Date())

            // 5) Construir la URL usando el servidor base, xEmpleado, el tipo (ENTRADA o SALIDA) y la fecha/hora
            val urlFichaje = prueba +
                    "&cTipFic=$tipo" +
                    "&tGpsLat=$lat" +
                    "&tGpsLon=$lon"

            /*
            val urlFichaje = urlServidor +
                    "&cEmpCppExt=$xEmpleado" +
                    "&cTipFic=$tipo" +
                    "&fFichaje=$fechaFichaje" +
                    "&tGpsLat=$lat" +
                    "&tGpsLon=$lon"

             */

            Log.d("Fichar", "URL que se va a enviar: $urlFichaje")

            // 6) Llama a enviarFichaje(...) en un hilo IO
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                enviarFichaje(urlFichaje)
            }
        } else {
            Toast.makeText(
                context,
                "Active su GPS y revise su cobertura",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}




@Composable
fun BottomNavigationBar(
    onNavigate: (String) -> Unit,
    onToggleFichar: () -> Unit,
    modifier: Modifier = Modifier,
    hideCuadroParaFichar: () -> Unit // 🔥 Nueva función para ocultar el cuadro
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
                    onToggleFichar() // ✅ Alterna la visibilidad del cuadro de fichar
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home32_2),
                    contentDescription = "Fichar",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
            Text(text = "Fichar", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }

        // 🔥 Modificamos las funciones de navegación para ocultar el cuadro
        NavigationButton("Fichajes", R.drawable.ic_fichajes32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.Fichaje)
        }
        NavigationButton("Incidencias", R.drawable.ic_incidencia32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.Incidencia)
        }
        NavigationButton("Horarios", R.drawable.ic_horario32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.Horarios)
        }
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

@Composable
fun LoadingScreen(isLoading: Boolean) {
    if (isLoading) {
        val context = LocalContext.current
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.version_2)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Loading GIF",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}












