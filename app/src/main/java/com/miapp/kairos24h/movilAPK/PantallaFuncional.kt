/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

package com.miapp.kairos24h.movilAPK

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.miapp.kairos24h.enlaces_internos.BuildURLmovil
import com.miapp.kairos24h.enlaces_internos.ImagenesMovil
import com.miapp.kairos24h.R
import com.miapp.kairos24h.sesionesYSeguridad.AuthManager
import com.miapp.kairos24h.sesionesYSeguridad.ManejoDeSesion
import com.miapp.kairos24h.sesionesYSeguridad.SeguridadUtils
import com.miapp.kairos24h.sesionesYSeguridad.SeguridadUtils.ResultadoUbicacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


//============================== CUADRO PARA FICHAR ======================================
@Composable
fun CuadroParaFichar(
    isVisibleState: MutableState<Boolean>,
    fichajes: List<String>,
    onFichaje: (String) -> Unit,
    onShowAlert: (String) -> Unit,
    webViewState: MutableState<WebView?>,
    mostrarBotonesFichaje: Boolean // ← NUEVO PARÁMETRO
) {
    // Mover refreshTrigger fuera del if para que se ejecute siempre
    val refreshTrigger = remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Añadir observer de ON_RESUME para refrescar el trigger
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger.longValue = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (isVisibleState.value) {
        Box(
            modifier = Modifier
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
                Logo_empresa_cliente()
                MiHorario()
                if (mostrarBotonesFichaje) {
                    BotonesFichajeConPermisos(
                        onFichaje = onFichaje,
                        onShowAlert = onShowAlert,
                        webView = webViewState.value ?: return@CuadroParaFichar,
                        refreshTrigger = refreshTrigger // 7. Pasar refreshTrigger
                    )
                }
                // rememberDatosHorario()  // Eliminado porque ya no se necesita
                RecuadroFichajesDia(refreshTrigger) // 4. Pasar refreshTrigger a RecuadroFichajesDia
                AlertasDiarias(
                    onAbrirWebView = { url -> webViewState.value?.loadUrl(url) },
                    hideCuadroParaFichar = { isVisibleState.value = false },
                    refreshTrigger = refreshTrigger
                )
                Logo_empresa_desarrolladora()
            }
        }
    }
}

@Composable
fun Logo_empresa_cliente() {
    Box(
        modifier = ImagenesMovil.logoBoxModifier,
        contentAlignment = Alignment.Center
    ) {
        ImagenesMovil.LogoClienteRemoto()
    }
}

@Composable
fun Logo_empresa_desarrolladora() {
    Box(
        modifier = ImagenesMovil.logoBoxModifierDev,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = ImagenesMovil.lodoDesarrolladora),
            contentDescription = "logo de la desarrolladora",
            modifier = ImagenesMovil.logoModifierDev
        )
    }
}


@Composable
fun MiHorario() {
    // Obtener contexto y formateadores de fecha
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var urlHorario by remember { mutableStateOf("") }
    var fechaFormateada by remember { mutableStateOf("Cargando...") }

    LaunchedEffect(Unit) {
        val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
        if (fechaServidor != null) {
            val fecha = dateFormatter.format(fechaServidor)
            urlHorario = BuildURLmovil.getMostrarHorarios(context) + "&fecha=$fecha"
            val dateFormatterTexto = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            fechaFormateada = dateFormatterTexto.format(fechaServidor)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
        }
    }

    // Muestra en el log la URL que se va a usar para consultar el horario del usuario
    Log.d("MiHorario", "URL solicitada: $urlHorario")

    // Estado para mostrar el horario
    val horarioTexto by produceState(initialValue = "Cargando horario...", key1 = urlHorario) {
        if (urlHorario.isBlank()) {
            value = "Cargando horario..."
            return@produceState
        }
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(urlHorario).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                // Muestra la respuesta completa del servidor tras pedir el horario
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
                            // Muestra el valor obtenido del campo N_HORINI en el JSON
                            Log.d("MiHorario", "Valor N_HORINI: $horaIni")
                            // Muestra el valor obtenido del campo N_HORFIN en el JSON
                            Log.d("MiHorario", "Valor N_HORFIN: $horaFin")

                            if (horaIni == 0 && horaFin == 0) {
                                "No Horario"
                            } else {
                                @SuppressLint("DefaultLocale")
                                fun minutosAHora(minutos: Int): String {
                                    val horas = minutos / 60
                                    val mins = minutos % 60
                                    return String.format(Locale.getDefault(), "%02d:%02d", horas, mins)
                                }
                                minutosAHora(horaIni) + " - " + minutosAHora(horaFin)
                            }
                        } else {
                            "No Horario"
                        }
                    } catch (e: Exception) {
                        // Informa si hubo un error al parsear el JSON del horario
                        Log.e("MiHorario", "Error al parsear JSON: ${e.message}\nResponse body: $responseBody")
                        "Error al procesar horario"
                    }
                }
            }
        } catch (e: Exception) {
            // Informa si hubo un error general al obtener el horario
            Log.e("MiHorario", "Excepción al obtener horario: ${e.message}")
            "Error de conexión"
        }
    }

    // Interfaz
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-10).dp)
            .padding(bottom = 20.dp)
            .border(width = 1.dp, color = Color(0xFFC0C0C0))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = fechaFormateada,
            color = Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 22.sp
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
    webView: WebView?,
    refreshTrigger: MutableState<Long> // 5. Añadir parámetro refreshTrigger
) {
    // Añadir al principio de BotonesFichajeConPermisos
    var ultimoFichajeTimestamp by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    var pendingFichaje by remember { mutableStateOf<String?>(null) }

    // Launcher para solicitar el permiso de ubicación
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingFichaje?.let { tipo ->
                // Informa que se ha concedido el permiso para fichar, se indica el tipo (ENTRADA/SALIDA)
                Log.d("Fichaje", "Permiso concedido. Procesando fichaje de: $tipo")
                if (webView != null) {
                    fichar(context, tipo, webView)
                } else {
                    // Informa que el WebView es null y por eso no se puede proceder con el fichaje
                    Log.e("Fichaje", "webView es null. No se puede fichar.")
                }
                onFichaje(tipo)
            }
        } else {
            // Informa que se ha denegado el permiso de ubicación
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
                    SeguridadUtils.isUsingVPN(context) -> {
                        // Informa que se ha intentado fichar con VPN activa
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }
                    !SeguridadUtils.isInternetAvailable(context) -> {
                        // Informa que no hay conexión a internet en el momento del fichaje
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }
                    !SeguridadUtils.hasLocationPermission(context) -> {
                        // Informa que no se tiene permiso de ubicación GPS
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                // Lanzar la comprobación real de ubicación simulada
                CoroutineScope(Dispatchers.Main).launch {
                    when (SeguridadUtils.detectarUbicacionReal(context)) {
                        ResultadoUbicacion.GPS_DESACTIVADO -> {
                            // Informa que el GPS está desactivado
                            Log.e("Seguridad", "GPS desactivado")
                            onShowAlert("PROBLEMA GPS")
                            return@launch
                        }
                        ResultadoUbicacion.UBICACION_SIMULADA -> {
                            // Informa que se detectó una ubicación simulada
                            Log.e("Seguridad", "Ubicación simulada detectada")
                            onShowAlert("POSIBLE UBI FALSA")
                            return@launch
                        }
                        ResultadoUbicacion.OK -> {
                            // continuar con fichaje
                        }
                    }
                    // --- Prevención de fichaje duplicado ---
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimoFichajeTimestamp < 5000) {
                        // Previene fichajes duplicados en corto intervalo de tiempo
                        Log.w("Fichaje", "Fichaje repetido ignorado")
                        return@launch
                    }
                    ultimoFichajeTimestamp = ahora
                    // --- Fin prevención ---
                    // Informa que se está procesando el fichaje de ENTRADA
                    Log.d(
                        "Fichaje",
                        "Fichaje Entrada: Permiso concedido. Procesando fichaje de ENTRADA"
                    )
                    webView?.let { fichar(context, "ENTRADA", it) }
                    onFichaje("ENTRADA")
                    refreshTrigger.value = System.currentTimeMillis() // 6. Actualizar refreshTrigger tras fichaje
                    // Retardo y actualización adicional tras 1 segundo
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        refreshTrigger.value = System.currentTimeMillis()
                    }
                }
            },
        color = Color(0xFF7599B6),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF0E4879))
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
                text = buildAnnotatedString {
                    append("Fichaje ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Entrada")
                    }
                },
                color = Color.White,
                fontSize = 25.sp,
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
                    SeguridadUtils.isUsingVPN(context) -> {
                        // Informa que se ha intentado fichar con VPN activa
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }
                    !SeguridadUtils.isInternetAvailable(context) -> {
                        // Informa que no hay conexión a internet en el momento del fichaje
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }
                    !SeguridadUtils.hasLocationPermission(context) -> {
                        // Informa que no se tiene permiso de ubicación GPS
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                // Lanzar la comprobación real de ubicación simulada
                CoroutineScope(Dispatchers.Main).launch {
                    when (SeguridadUtils.detectarUbicacionReal(context)) {
                        ResultadoUbicacion.GPS_DESACTIVADO -> {
                            // Informa que el GPS está desactivado
                            Log.e("Seguridad", "GPS desactivado")
                            onShowAlert("PROBLEMA GPS")
                            return@launch
                        }
                        ResultadoUbicacion.UBICACION_SIMULADA -> {
                            // Informa que se detectó una ubicación simulada
                            Log.e("Seguridad", "Ubicación simulada detectada")
                            onShowAlert("POSIBLE UBI FALSA")
                            return@launch
                        }
                        ResultadoUbicacion.OK -> {
                            // continuar con fichaje
                        }
                    }
                    // --- Prevención de fichaje duplicado ---
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimoFichajeTimestamp < 5000) {
                        // Previene fichajes duplicados en corto intervalo de tiempo
                        Log.w("Fichaje", "Fichaje repetido ignorado")
                        return@launch
                    }
                    ultimoFichajeTimestamp = ahora
                    // --- Fin prevención ---
                    // Informa que se está procesando el fichaje de SALIDA
                    Log.d("Fichaje", "Fichaje Salida: Permiso concedido. Procesando fichaje de SALIDA")
                    webView?.let { fichar(context, "SALIDA", it) }
                    onFichaje("SALIDA")
                    refreshTrigger.value = System.currentTimeMillis() // 6. Actualizar refreshTrigger tras fichaje
                    // Retardo y actualización adicional tras 1 segundo
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        refreshTrigger.value = System.currentTimeMillis()
                    }
                }
            },
        color = Color(0xFF7599B6),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF0E4879))
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
                text = buildAnnotatedString {
                    append("Fichaje ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Salida")
                    }
                },
                color = Color.White,
                fontSize = 25.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

data class FichajeVisual(val entrada: String, val salida: String, val lcumEnt: String, val lcumSal: String)

@Composable
fun RecuadroFichajesDia(refreshTrigger: State<Long>) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // --- NUEVO: Variables para horario ---
    val horaInicioHorario = remember { mutableIntStateOf(0) }
    val horaFinHorario = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
        if (fechaServidor != null) {
            val urlHorario = BuildURLmovil.getMostrarHorarios(context) + "&fecha=${dateFormatter.format(fechaServidor)}"
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(urlHorario).build()
                    val response = client.newCall(request).execute()
                    val jsonBody = response.body?.string()?.replace("\uFEFF", "")
                    val json = JSONObject(jsonBody ?: "")
                    val dataArray = json.getJSONArray("dataHorario")
                    if (dataArray.length() > 0) {
                        val item = dataArray.getJSONObject(0)
                        horaInicioHorario.intValue = item.optInt("N_HORINI", 0)
                        horaFinHorario.intValue = item.optInt("N_HORFIN", 0)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Necesario para la URL, mantener la lógica de fechaSeleccionada
    val fechaSeleccionada = remember { mutableStateOf("") }
    LaunchedEffect(refreshTrigger.value) {
        if (fechaSeleccionada.value.isEmpty()) {
            val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
            if (fechaServidor != null) {
                val formateador = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                fechaSeleccionada.value = formateador.format(fechaServidor)
            } else {
                fechaSeleccionada.value = "0000-00-00"
            }
        }
    }

    // Muestra la fecha que se está usando para consultar los fichajes del día
    Log.d("RecuadroFichajesDia", "Fecha usada para la petición: ${fechaSeleccionada.value}")

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    val fichajesTexto by produceState(
        initialValue = emptyList<FichajeVisual>(),
        key1 = Triple(fechaSeleccionada.value, xEmpleado, refreshTrigger.value)
    ) {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val urlFichajes = BuildURLmovil.getMostrarFichajes(context) + "&fecha=${fechaSeleccionada.value}"
                // Muestra la URL completa que se usa para obtener los fichajes
                Log.d("RecuadroFichajesDia", "URL completa invocada: $urlFichajes")
                val request = Request.Builder().url(urlFichajes).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()?.replace("\uFEFF", "")
                // Muestra la respuesta del servidor con los fichajes recibidos
                Log.d("RecuadroFichajesDia", "Respuesta desde consultarFichajeExterno (URL: ${response.request.url}): $responseBody")

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("RecuadroFichajesDia", "Error: ${response.code}")
                    emptyList()
                } else {
                    try {
                        val json = JSONObject(responseBody)
                        val fichajesArray = json.getJSONArray("dataFichajes")

                        buildList {
                            for (i in 0 until fichajesArray.length()) {
                                val item = fichajesArray.getJSONObject(i)
                                val nMinEntStr = item.optString("nMinEnt", "").trim()
                                val nMinSalStr = item.optString("nMinSal", "").trim()
                                val nMinEnt = nMinEntStr.toIntOrNull()
                                val nMinSal = nMinSalStr.toIntOrNull()
                                val lcumEnt = if (item.has("lCumEnt")) item.getBoolean("lCumEnt").toString() else ""
                                val lcumSal = if (item.has("lCumSal")) item.getBoolean("lCumSal").toString() else ""

                                // Registro de valores individuales
                                // Muestra los valores obtenidos para cada fichaje (entrada, salida y cumplimiento)
                                Log.d("RecuadroFichajesDia", "Fichaje $i → nMinEnt: $nMinEnt, nMinSal: $nMinSal, LCUMENT: $lcumEnt, LCUMSAL: $lcumSal")

                                fun minutosAHora(minutos: Int?): String {
                                    return if (minutos != null) {
                                        val horas = minutos / 60
                                        val mins = minutos % 60
                                        String.format(Locale.getDefault(), "%02d:%02d", horas, mins)
                                    } else {
                                        "??"
                                    }
                                }
                                val horaEntrada = minutosAHora(nMinEnt)
                                val horaSalida = minutosAHora(nMinSal)
                                add(FichajeVisual(horaEntrada, horaSalida, lcumEnt, lcumSal))
                            }
                        }
                    } catch (e: Exception) {
                        // Informa que hubo un error al parsear el JSON de los fichajes
                        Log.e("RecuadroFichajesDia", "Error al parsear JSON: ${e.message}")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            // Informa de una excepción general al obtener los fichajes
            Log.e("RecuadroFichajesDia", "Excepción al obtener fichajes: ${e.message}")
            emptyList()
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val nuevaFecha = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            fechaSeleccionada.value = dateFormatter.format(nuevaFecha.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fichajes Día",
            color = Color(0xFF7599B6),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-20).dp)
        )

        val sdfEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val iconColor = Color(0xFF7599B6)

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
                    contentDescription = "Seleccionar fecha",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = {
                val actual = sdfEntrada.parse(fechaSeleccionada.value)
                val anterior = Calendar.getInstance().apply {
                    time = actual ?: Date()
                    add(Calendar.DAY_OF_MONTH, -1)
                }
                fechaSeleccionada.value = sdfEntrada.format(anterior.time)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.hacia_atras),
                    contentDescription = "Día anterior",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            Text(
                text = try {
                    val date = sdfEntrada.parse(fechaSeleccionada.value)
                    sdfSalida.format(date ?: Date())
                } catch (_: Exception) {
                    fechaSeleccionada.value
                },
                color = Color.Gray,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val actual = sdfEntrada.parse(fechaSeleccionada.value)
                val siguiente = Calendar.getInstance().apply {
                    time = actual ?: Date()
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                fechaSeleccionada.value = sdfEntrada.format(siguiente.time)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.hacia_delante),
                    contentDescription = "Día siguiente",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
                    fechaServidor?.let {
                        val nuevaFecha = dateFormatter.format(it)
                        withContext(Dispatchers.Main) {
                            fechaSeleccionada.value = nuevaFecha
                        }
                    }
                }
            }) {
                Image(
                    painter = painterResource(id = R.drawable.reload),
                    contentDescription = "Fecha actual",
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp)
                .background(Color.White)
                .padding(10.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fichajesTexto.isNotEmpty()) {
                fichajesTexto.forEach { fichaje ->
                    val colorEntrada = when (fichaje.lcumEnt) {
                        "false" -> Color.Red
                        "true" -> Color(0xFF449B1B)
                        else -> Color(0xFF7599B6)
                    }
                    val colorSalida = when (fichaje.lcumSal) {
                        "false" -> Color.Red
                        "true" -> Color(0xFF449B1B)
                        else -> Color(0xFF7599B6)
                    }
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(
                            text = "${fichaje.entrada} - ",
                            fontSize = 23.sp,
                            color = colorEntrada
                        )
                        Text(
                            text = fichaje.salida,
                            fontSize = 23.sp,
                            color = colorSalida
                        )
                    }
                }
            } else {
                Text(
                    text = "No hay fichajes hoy",
                    fontSize = 23.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// --- Nueva clase de datos para avisos ---
data class AvisoItem(val titulo: String, val detalle: String, val url: String?)

@Composable
fun AlertasDiarias(
    onAbrirWebView: (String) -> Unit,
    hideCuadroParaFichar: () -> Unit,
    refreshTrigger: MutableState<Long>
) {
    val context = LocalContext.current
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    // Forzar fetch inicial
    LaunchedEffect(Unit) {
        refreshTrigger.value = System.currentTimeMillis()
    }

    // Nuevo: State para avisos y scope estable
    val coroutineScope = rememberCoroutineScope()
    val avisosState = remember { mutableStateOf<List<AvisoItem>>(emptyList()) }

    LaunchedEffect(refreshTrigger.value) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val urlAlertas = BuildURLmovil.getMostrarAlertas(context)
                Log.d("AlertasDiarias", "URL de alertas: $urlAlertas")
                val client = OkHttpClient()
                // Usar siempre el dominio correcto definido en BuildURL.getHost(context)
                val dominio = BuildURLmovil.getHost(context)
                val cookie = CookieManager.getInstance()
                    .getCookie(dominio) ?: ""
                val request = Request.Builder()
                    .url(urlAlertas)
                    .addHeader("Cookie", cookie)
                    .build()
                val response = client.newCall(request).execute()
                val jsonBody = response.body?.string()
                Log.d("JSONAlertas", "JSON crudo recibido: $jsonBody")
                val json = JSONObject(jsonBody ?: "")
                val dataArray = json.optJSONArray("dataAvisos")
                if (dataArray != null) {
                    Log.d("JSONAlertas", "dataAvisos length = ${dataArray.length()}")
                    if (dataArray.length() > 0) {
                        val nuevaLista = mutableListOf<AvisoItem>()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val dAviso = item.optString("D_AVISO", "Sin aviso")
                            val tAviso = item.optString("T_AVISO", "")
                            val tUrl = item.optString("T_URL", "").takeIf { it.isNotBlank() && it != "null" }
                            Log.d("JSONAlertas", "[$i] D_AVISO: $dAviso")
                            Log.d("JSONAlertas", "[$i] T_AVISO: $tAviso")
                            Log.d("JSONAlertas", "[$i] T_URL: $tUrl")
                            nuevaLista.add(AvisoItem(dAviso, tAviso, tUrl))
                        }
                        avisosState.value = nuevaLista
                    } else {
                        Log.d("JSONAlertas", "Array 'dataAvisos' está presente pero vacío")
                        avisosState.value = listOf(AvisoItem("No hay alertas disponibles", "", null))
                    }
                } else {
                    Log.d("JSONAlertas", "Array 'dataAvisos' es null")
                    avisosState.value = listOf(AvisoItem("No hay alertas disponibles", "", null))
                }
            } catch (e: Exception) {
                Log.e("AlertasDiarias", "Error obteniendo alertas: ${e.message}")
                avisosState.value = listOf(AvisoItem("Error al cargar alertas", "", null))
            }
        }
    }

    // Refresco automático cada 10 minutos
    LaunchedEffect(true) {
        while (true) {
            delay(10 * 60 * 1000)
            refreshTrigger.value = System.currentTimeMillis()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Avisos / Alertas",
                    color = Color(0xFF7599B6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (avisosState.value.isEmpty()) {
                    Text(
                        text = "Cargando alertas...",
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                avisosState.value.forEachIndexed { index, aviso ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.LightGray)
                                .clickable {
                                    expandedStates[index] = expandedStates[index] != true
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expandedStates[index] == true) Icons.Default.Remove else Icons.Default.Add,
                                contentDescription = "Expandir",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF7599B6)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = aviso.titulo,
                                fontSize = 18.sp,
                                color = Color(0xFF7599B6),
                                modifier = Modifier.weight(1f)
                            )
                            if (!aviso.url.isNullOrEmpty()) {
                                val context = LocalContext.current
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Redireccionar",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onAbrirWebView(BuildURLmovil.getHost(context).trimEnd('/') + "/" + aviso.url.trimStart('/'))
                                                delay(1000)
                                                hideCuadroParaFichar()
                                            }
                                        },
                                    tint = Color(0xFF7599B6)
                                )
                            }
                        }
                        AnimatedVisibility(visible = expandedStates[index] == true) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                OutlinedTextField(
                                    value = aviso.detalle,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp),
                                    readOnly = true,
                                    label = { Text("Detalle del aviso") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// Mensaje de alerta cuando se le da a uno de los botones de fichar
@Composable
fun MensajeAlerta(
    tipo: String = "ENTRADA",
    onClose: () -> Unit
) {
    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm'h'", Locale.getDefault()).format(Date())

    val mensaje = when (tipo.uppercase()) {
        "ENTRADA" -> "Fichaje de Entrada realizado correctamente"
        "SALIDA" -> "Fichaje de Salida realizado correctamente"
        "PROBLEMA GPS" -> "No se detecta la geolocalización gps. Por favor, active la geolocalización gps para poder fichar y vuelvalo a intentar en unos segundos."
        "PROBLEMA INTERNET" -> "El dispositivo no está conectado a la red. Revise su conexión a Internet."
        "POSIBLE UBI FALSA" -> "Se detectó una posible ubicación falsa. Reinicie su geolocalización gps y vuelva a intentarlo en unos minutos"
        "VPN DETECTADA" -> "VPN detectada. Desactive la VPN para continuar y vuelva a intentarlo en unos minutos."
        else -> "Fichaje de $tipo realizado correctamente"
    }

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

                Text(
                    text = mensaje,
                    color = Color.Black,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                val partes = currentDateTime.split(" ")
                val fechaSolo = partes.getOrNull(0) ?: ""
                val horaSolo = partes.getOrNull(1) ?: ""

                Text(
                    text = buildAnnotatedString {
                        append("$fechaSolo ")
                        withStyle(
                            style = SpanStyle(
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


