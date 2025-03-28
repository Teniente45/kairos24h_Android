package com.miapp.iDEMO_kairos24h

import android.location.LocationManager
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.app.DatePickerDialog
import java.util.Calendar
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.core.app.ActivityCompat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL.mostrarHorarios
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// 🚨 Verificar si el dispositivo usa una VPN
private fun isUsingVPN(): Boolean {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().any {
            it.name.equals("tun0", ignoreCase = true) || it.name.equals("ppp0", ignoreCase = true)
        }
    } catch (e: Exception) {
        false
    }
}

// 🚨 Detectar si el usuario est´á conectado a Internet
fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}

// 🚨 Detectar si el usuario tiene activadas ubicaciones falsas
private fun isMockLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false

    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        Log.e("Seguridad", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
        return false
    }

    return try {
        locationManager.getProviders(true).any { provider ->
            val location = locationManager.getLastKnownLocation(provider)
            location?.isFromMockProvider == true
        }
    } catch (e: SecurityException) {
        Log.e("Seguridad", "Error al verificar ubicación simulada: ${e.message}")
        false
    }
}


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
                onLogout = { navigateToLogin() }
            )
        }
        startActivitySimulationTimer()
    }

    // Obtniene la fecha y hora de internte, para evitar que la hora y fecha local del dispositivo esté alterada
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
            null
        }
    }

    // Inicia un temporizador para simular actividad en la WebView cada 2 horas
    private fun startActivitySimulationTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("Fichar", "Simulando actividad en WebView después de 2 horas de inactividad")
                simularActividadWebView()
                handler.postDelayed(this, sessionTimeoutMillis) // Repite la acción cada 2 horas
            }
        }, sessionTimeoutMillis)
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
        webView?.loadUrl(WebViewURL.LOGIN) ?:
        Log.e("Fichar", "Error: WebView no inicializado en onStop()")
    }

    // Se ejecuta cuando la actividad se reanuda. ComBuildURL.crearFichaje las credenciales y la actividad reciente
    override fun onResume() {
        super.onResume()
        // Recupera las credenciales almacenadas
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)
        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            webView?.loadUrl(WebViewURL.LOGIN)
        } else {
            val currentTime = System.currentTimeMillis()
            Log.d("Fichar", "onResume: Tiempo actual: $currentTime, Última interacción: $lastInteractionTime")

            // Si han pasado más de 30 segundos desde la última interacción, redirige al login
            if (currentTime - lastInteractionTime > 30000) {
                Log.d("Fichar", "onResume: Ha pasado más de 30 segundos desde la última interacción. Redirigiendo a Login.")
                webView?.loadUrl(WebViewURL.LOGIN)
            } else {
                Log.d("Fichar", "onResume: Detectada actividad reciente, simulando movimiento del mouse.")
                simularActividadWebView()
            }
        }
    }

    private fun simularActividadWebView(intervalo: Long = 5000) {
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
        handler.postDelayed({ simularActividadWebView(intervalo) }, intervalo)
    }

    // Redirige al usuario a la pantalla de login y limpia la actividad actual
    private fun navigateToLogin() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("usuario")
            remove("password")
            apply()
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

// Funcion para obtener los fichajes del dia
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
        null
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(
    usuario: String,
    password: String,
    onLogout: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var showCuadroParaFichar by remember { mutableStateOf(true) }
    var fichajes by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageIndex by remember { mutableStateOf(0) }
    var fichajeAlertTipo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        isLoading = true
        delay(1500)
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xFFE2E4E5))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Usuario e imagen
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { imageIndex = (imageIndex + 1) % imageList.size }
                ) {
                    Icon(
                        painter = painterResource(id = imageList[imageIndex]),
                        contentDescription = "Usuario",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                }
                Text(
                    text = cUsuario,
                    color = Color(0xFF7599B6),
                    fontSize = 18.sp
                )
            }

            // Cerrar sesión
            IconButton(onClick = onLogout) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cerrar32),
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(30.dp),
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

            // Cuadro para fichar
            if (showCuadroParaFichar) {
                CuadroParaFichar(
                    isVisible = showCuadroParaFichar,
                    onDismiss = { showCuadroParaFichar = false },
                    fichajes = fichajes,
                    onFichaje = { tipo ->
                        obtenerCoord(
                            context,
                            onLocationObtained = { lat, lon ->
                                if (lat == 0.0 || lon == 0.0) {
                                    Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
                                    fichajeAlertTipo = "Ubicación inválida"
                                    return@obtenerCoord
                                }
                                fichajeAlertTipo = tipo
                            },
                            onShowAlert = { alertTipo ->
                                fichajeAlertTipo = alertTipo
                            }
                        )
                    },
                    onShowAlert = { alertTipo ->
                        fichajeAlertTipo = alertTipo
                    },
                    modifier = Modifier.zIndex(2f),
                    webViewState = webViewState
                )
            }

            // Mostrar alerta
            fichajeAlertTipo?.let { tipo ->
                MensajeAlerta(
                    tipo = tipo,
                    onClose = { fichajeAlertTipo = null }
                )
            }

            // Navegación inferior
            BottomNavigationBar(
                onNavigate = { url ->
                    isLoading = true
                    showCuadroParaFichar = false
                    webViewState.value?.loadUrl(url)
                    scope.launch {
                        delay(1500)
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


//============================== CUADRO PARA FICHAR ======================================
@Composable
fun CuadroParaFichar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    fichajes: List<String>,
    onFichaje: (tipo: String) -> Unit,
    onShowAlert: (String) -> Unit, // ✅ Se agregó correctamente
    modifier: Modifier = Modifier,
    webViewState: MutableState<WebView?>
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
                    .fillMaxSize()
                    // Habilita el scroll vertical:
                    .verticalScroll(rememberScrollState())
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
                BotonesFichajeConPermisos(
                    onFichaje = onFichaje,
                    onShowAlert = onShowAlert,
                    webView = webViewState.value ?: return@CuadroParaFichar
                )
                val datos = rememberDatosHorario()
                RecuadroFichajesDia(fichajes, fecha = datos.fechaSeleccionada)
                AlertasDiarias()
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


data class DatosHorario(
    val fechaFormateada: String,
    val fechaSeleccionada: String,
    val xEmpleado: String,
    val urlHorario: String
)
/**
 * Con esta funcion tomaremos la fecha del servidor, el xEmpleado para poder usarlo en las funciones siguientes
 */
@Composable
fun rememberDatosHorario(): DatosHorario {
    val context = LocalContext.current

    val dateFormatterTexto = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    }
    val dateFormatterURL = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    var fechaFormateada by remember { mutableStateOf("Cargando...") }
    var fechaSeleccionada by remember { mutableStateOf("") }

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    LaunchedEffect(Unit) {
        val fechaServidor = (context as? Fichar)?.obtenerFechaHoraInternet()
        if (fechaServidor != null) {
            fechaFormateada = dateFormatterTexto.format(fechaServidor)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            fechaSeleccionada = dateFormatterURL.format(fechaServidor)
        } else {
            fechaFormateada = "Error al obtener fecha"
            fechaSeleccionada = "0000-00-00"
        }
    }

    val urlHorario = mostrarHorarios +
            "&xEntidad=3" +
            "&xEmpleado=$xEmpleado" +
            "&fecha=$fechaSeleccionada"

    return DatosHorario(
        fechaFormateada = fechaFormateada,
        fechaSeleccionada = fechaSeleccionada,
        xEmpleado = xEmpleado,
        urlHorario = urlHorario
    )
}


/**
 * Cambiar por variables const la fechaFormateada y la fechaParaURL
 */
@Composable
fun MiHorario() {
    val context = LocalContext.current
    val datos = rememberDatosHorario()

    // ✅ Usamos directamente la URL construida con la fecha del servidor
    val urlHorario = mostrarHorarios

    // Estado para mostrar el horario
    val horarioTexto by produceState(initialValue = "Cargando horario...") {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(urlHorario).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("MiHorario", "Respuesta completa del servidor:\n$responseBody")

                val cleanedBody = responseBody?.replace("\uFEFF", "")

                if (!response.isSuccessful || cleanedBody.isNullOrEmpty()) {
                    Log.e("MiHorario", "Error: ${response.code}")
                    "Error al obtener horario"
                } else {
                    try {
                        val json = JSONObject(cleanedBody)
                        val dataArray = json.getJSONArray("dataHorario")
                        if (dataArray.length() > 0) {
                            val item = dataArray.getJSONObject(0)
                            val horaIni = item.optInt("N_HORINI", 0)
                            val horaFin = item.optInt("N_HORFIN", 0)
                            Log.d("MiHorario", "Valor N_HORINI: $horaIni")
                            Log.d("MiHorario", "Valor N_HORFIN: $horaFin")

                            if (horaIni == 0 && horaFin == 0) {
                                "No se detectaron fichajes este día"
                            } else {
                                fun minutosAHora(minutos: Int): String {
                                    val horas = minutos / 60
                                    val mins = minutos % 60
                                    return String.format("%02d:%02d", horas, mins)
                                }
                                minutosAHora(horaIni) + " - " + minutosAHora(horaFin)
                            }
                        } else {
                            "No hay horario"
                        }
                    } catch (e: Exception) {
                        Log.e("MiHorario", "Error al parsear JSON: ${e.message}\nResponse body: $responseBody")
                        "Error al procesar horario"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MiHorario", "Excepción al obtener horario: ${e.message}")
            "Error de conexión"
        }
    }

    // Interfaz
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-40).dp)
            .border(width = 1.dp, color = Color(0xFFC0C0C0))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = datos.fechaFormateada,
            color = Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Text(
            text = horarioTexto,
            color = if (horarioTexto.contains("Error") || horarioTexto.contains("No hay")) Color.Red else Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BotonesFichajeConPermisos(
    onFichaje: (tipo: String) -> Unit,
    onShowAlert: (String) -> Unit,
    webView: WebView?
) {
    val context = LocalContext.current
    var pendingFichaje by remember { mutableStateOf<String?>(null) }

    // Launcher para solicitar el permiso de ubicación
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingFichaje?.let { tipo ->
                Log.d("Fichaje", "Permiso concedido. Procesando fichaje de: $tipo")
                if (webView != null) {
                    fichar(context, tipo, webView!!)
                } else {
                    Log.e("Fichaje", "webView es null. No se puede fichar.")
                }
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
            .padding(horizontal = 20.dp)
            .height(55.dp)
            .offset(y = (-20).dp)
            .clickable {
                when {
                    isUsingVPN() -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }

                    isMockLocationEnabled(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con ubicación simulada")
                        onShowAlert("POSIBLE UBI FALSA")
                        return@clickable
                    }

                    !isInternetAvailable(context) -> {
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }

                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED -> {
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                Log.d(
                    "Fichaje",
                    "Fichaje Entrada: Permiso concedido. Procesando fichaje de ENTRADA"
                )
                webView?.let { fichar(context, "ENTRADA", it) }
                onFichaje("ENTRADA")
            },
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF7599B6))
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
                    .height(40.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fichaje Entrada",
                color = Color(0xFF7599B6),
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
            .padding(horizontal = 20.dp)
            .offset(y = (-40).dp)
            .height(55.dp)
            .clickable {
                when {
                    isUsingVPN() -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }

                    isMockLocationEnabled(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con ubicación simulada")
                        onShowAlert("POSIBLE UBI FALSA")
                        return@clickable
                    }

                    !isInternetAvailable(context) -> {
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }

                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED -> {
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                Log.d("Fichaje", "Fichaje Salida: Permiso concedido. Procesando fichaje de SALIDA")
                webView?.let { fichar(context, "SALIDA", it) }
                onFichaje("SALIDA")
            },
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF7599B6))
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
                    .height(40.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fichaje Salida",
                color = Color(0xFF7599B6),
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

/**
 * continuar aqui
 */
@Composable
fun RecuadroFichajesDia(fichajes: List<String>, fecha: String) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val fechaSeleccionadaState = remember { mutableStateOf(fecha) }
    val fechaSeleccionada = fechaSeleccionadaState.value

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val nuevaFecha = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                fechaSeleccionadaState.value = dateFormatter.format(nuevaFecha.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val urlFichajes = BuildURL.mostrarFichajes +
            "&xEmpleado=$xEmpleado" +
            "&fecha=$fechaSeleccionada"

    Log.d("RecuadroFichajesDia", "Invocando URL: $urlFichajes")

    val fichajesActuales by produceState<List<String>>(initialValue = emptyList(), key1 = fechaSeleccionada) {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(urlFichajes).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("RecuadroFichajesDia", "Respuesta del servidor:\n$responseBody")

                val cleanedBody = responseBody?.replace("\uFEFF", "")

                if (!response.isSuccessful || cleanedBody.isNullOrEmpty()) {
                    Log.e("RecuadroFichajesDia", "Error: ${response.code}")
                    emptyList()
                } else {
                    try {
                        val jsonArray = JSONArray(cleanedBody)
                        var primerFicEnt: String? = null
                        var ultimoFicSal: String? = null

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val ficEnt = item.optString("xFicEnt", null)
                            val ficSal = item.optString("xFicSal", null)

                            if (!ficEnt.isNullOrEmpty()) {
                                if (primerFicEnt == null || ficEnt < primerFicEnt) {
                                    primerFicEnt = ficEnt
                                }
                            }
                            if (!ficSal.isNullOrEmpty()) {
                                if (ultimoFicSal == null || ficSal > ultimoFicSal) {
                                    ultimoFicSal = ficSal
                                }
                            }
                        }

                        buildList {
                            if (primerFicEnt != null) add("Entrada: $primerFicEnt")
                            if (ultimoFicSal != null) add("Salida: $ultimoFicSal")
                        }
                    } catch (e: Exception) {
                        Log.e("RecuadroFichajesDia", "Error al parsear JSON: ${e.message}")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RecuadroFichajesDia", "Excepción al obtener fichajes: ${e.message}")
            emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fichajes Día",
            color = Color(0xFF7599B6),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-20).dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendario),
                    contentDescription = "Seleccionar fecha"
                )
            }

            val fechaFormateadaCorta = try {
                val sdfEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfSalida = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = sdfEntrada.parse(fechaSeleccionada)
                sdfSalida.format(date ?: Date())
            } catch (e: Exception) {
                fechaSeleccionada
            }

            Text(
                text = "Fecha: $fechaFormateadaCorta",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val fechaServidor = (context as? Fichar)?.obtenerFechaHoraInternet()
                    fechaServidor?.let {
                        val nuevaFecha = dateFormatter.format(it)
                        withContext(Dispatchers.Main) {
                            fechaSeleccionadaState.value = nuevaFecha
                        }
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.reload),
                    contentDescription = "Usar fecha del servidor"
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fichajesActuales.isNotEmpty()) {
                fichajesActuales.forEach { fichaje ->
                    Text(text = fichaje, color = Color(0xFF7599B6), fontSize = 18.sp)
                }
            } else {
                Text(text = "No hay fichajes hoy", color = Color.Gray, fontSize = 18.sp)
            }
        }
    }
}
/**
 * continuar aqui
 */
@Composable
fun AlertasDiarias() {
    // Control para expandir/colapsar el detalle de avisos
    var expanded by remember { mutableStateOf(false) }

    // Card principal que imita el "jumbotron" con borde y fondo blanco
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // Encabezado que imita la barra "Avisos / Alertas"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7599B6)) // color aproximado #7599B6
                    .clickable {
                        // Si quisieras permitir que hacer click en toda la barra
                        // despliegue/colapse, puedes usar expanded = !expanded
                    }
                    .padding(8.dp)
            ) {
                Text(
                    text = "Avisos / Alertas",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    // Botón "Mostrar Avisos/Alertas"
                    IconButton(
                        onClick = {
                            // Lógica para mostrarExplotacion(1, true, 'AVISO', ...).
                            // Aquí pones tu función o acción correspondiente.
                        }
                    ) {
                        // Usa tu icono o imagen. Ej. painterResource(R.drawable.mostrar20)
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Mostrar Avisos",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón "Nuevo Aviso"
                    IconButton(
                        onClick = {
                            // Lógica para createExplotacion('AVISO', '').
                        }
                    ) {
                        // Usa tu icono o imagen. Ej. painterResource(R.drawable.nuevo20)
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nuevo Aviso",
                            tint = Color.White
                        )
                    }
                }
            }

            // Cuerpo de la tarjeta (similar al panel-body del HTML)
            Column(modifier = Modifier.padding(top = 8.dp)) {

                // Fila que muestra "Solicitudes pendientes de tramitar"
                // y que al hacer click despliega la zona de detalle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray)
                        .clickable {
                            expanded = !expanded
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de "más" o "menos" para expandir
                    Icon(
                        imageVector = if (expanded) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = "Expandir / Colapsar",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF7599B6)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Solicitudes pendientes de tramitar.",
                        fontSize = 14.sp,
                        color = Color(0xFF7599B6)
                    )

                    // Icono a la derecha para "redireccionar"
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Redireccionar",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                // Llamada a urlDireccionar(...) o la función que corresponda
                            },
                        tint = Color(0xFF7599B6)
                    )
                }

                // Zona colapsable que imita el <textarea readonly="readonly">
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Si deseas un aspecto de "textarea":
                        OutlinedTextField(
                            value = "Solicitudes pendientes de tramitar",
                            onValueChange = { /* sin cambio, es solo lectura */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            readOnly = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MensajeAlerta(
    tipo: String = "ENTRADA",
    onClose: () -> Unit
) {
    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm'h'", Locale.getDefault()).format(Date())

    val mensaje = when (tipo.uppercase()) {
        "ENTRADA" -> "Fichaje de Entrada realizado correctamente"
        "SALIDA" -> "Fichaje de Salida realizado correctamente"
        "PROBLEMA GPS" -> "No se detecta la geolocalización gps. Por favor, active la geolocalización gps para poder fichar."
        "PROBLEMA INTERNET" -> "El dispositivo no está conectado a la red. Revise su conexión a Internet."
        "POSIBLE UBI FALSA" -> "Se detectó una posible ubicación falsa. Reinicie su geolocalización gps y vuelva a intentarlo en unos minutos"
        "VPN DETECTADA" -> "VPN detectada. Desactive la VPN para continuar y vuelva a intentarlo en unos minutos."
        else -> "Fichaje de $tipo realizado correctamente"
    }

    // 🎨 Color por tipo
    val colorFondo = when (tipo.uppercase()) {
        "ENTRADA" -> Color(0xFF124672) // Azul oscuro
        "SALIDA" -> Color(0xFFd7ebfa)  // Azul claro
        else -> Color(0xFFFF0101)      // Rojo para errores
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 🎨 Encabezado con color y texto según tipo
                val textoEncabezado = when (tipo.uppercase()) {
                    "ENTRADA" -> "ENTRADA"
                    "SALIDA" -> "SALIDA"
                    else -> "ERROR DE FICHAJE"
                }

                val colorTextoEncabezado = when (tipo.uppercase()) {
                    "SALIDA" -> Color(0xFF124672)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorFondo)
                        .padding(8.dp)
                ) {
                    Text(
                        text = textoEncabezado,
                        color = colorTextoEncabezado,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 📝 Mensaje del fichaje
                Text(
                    text = mensaje,
                    color = Color.Black,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🕒 Fecha + hora con hora en negrita y más grande
                val partes = currentDateTime.split(" ")
                val fechaSolo = partes.getOrNull(0) ?: ""
                val horaSolo = partes.getOrNull(1) ?: ""

                Text(
                    text = buildAnnotatedString {
                        append("$fechaSolo ")
                        withStyle(
                            style = androidx.compose.ui.text.SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        ) {
                            append(horaSolo)
                        }
                    },
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔘 Botón Cerrar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorFondo)
                    ) {
                        val colorTextoBoton = when (tipo.uppercase()) {
                            "SALIDA" -> Color(0xFF124672)
                            else -> Color.White
                        }

                        Text(
                            text = "Cerrar",
                            fontSize = 18.sp,
                            color = colorTextoBoton
                        )
                    }
                }
            }
        }
    }
}

//============================================== FICHAJE DE LA APP =====================================
private fun fichar(context: Context, tipo: String, webView: WebView) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
        Toast.makeText(context, "Debe aceptar los permisos de GPS para poder fichar.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        obtenerCoord(
            context,
            onLocationObtained = { lat, lon ->
                if (lat == 0.0 || lon == 0.0) {
                    Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
                    return@obtenerCoord
                }

                val (_, _, xEmpleado) = AuthManager.getUserCredentials(context)
                if (xEmpleado.isNullOrEmpty()) {
                    Log.e("Fichar", "xEmpleado no está disponible")
                    return@obtenerCoord
                }

                /**
                 * Partir en variables
                 */
                val urlFichaje = BuildURL.crearFichaje +
                        "&xEmpleado=$xEmpleado" +
                        "&cDomTipFic=$tipo" +
                        "&tGpsLat=$lat" +
                        "&tGpsLon=$lon"

                Log.d("Fichar", "URL que se va a enviar desde WebView: $urlFichaje")
                webView.evaluateJavascript("window.location.href = '$urlFichaje';", null)
            },
            onShowAlert = { alertTipo ->
                Log.e("Fichar", "Alerta: $alertTipo")
            }
        )
    } catch (e: SecurityException) {
        Log.e("Fichar", "Error de seguridad al acceder a la ubicación: ${e.message}")
    }
}

fun obtenerCoord(
    context: Context,
    onLocationObtained: (lat: Double, lon: Double) -> Unit,
    onShowAlert: (String) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 🔍 Verificar si los permisos de ubicación están concedidos
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        Log.e("Fichar", "No se cuenta con los permisos de ubicación.")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
        return
    }

    // 🔍 Verificar si el GPS está activado
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        Log.e("Fichar", "GPS desactivado.")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
        return
    }

    // 🔍 Intentar obtener la última ubicación conocida
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location == null) {
            Log.e("Fichar", "No se pudo obtener la ubicación.")
            onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
            return@addOnSuccessListener
        }

        if (location.isFromMockProvider) {
            Log.e("Fichar", "Ubicación falsa detectada.")
            onShowAlert("POSIBLE UBI FALSA") // Muestra mensaje de alerta
            return@addOnSuccessListener
        }

        // ✅ Ubicación válida
        onLocationObtained(location.latitude, location.longitude)
    }.addOnFailureListener { e ->
        Log.e("Fichar", "Error obteniendo ubicación: ${e.message}")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
    }
}
//============================================== FICHAJE DE LA APP =====================================

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
            .padding(2.dp)
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
            Text(text = "Fichar", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
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
//============================== CUADRO PARA FICHAR ======================================

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

