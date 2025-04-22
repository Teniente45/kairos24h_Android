package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.miapp.iDEMO_kairos24h.R
import com.miapp.iDEMO_kairos24h.fichar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            painter = painterResource(id = com.miapp.iDEMO_kairos24h.R.drawable.logo_i3data),
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
        val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
        if (fechaServidor != null) {
            fechaFormateada = dateFormatterTexto.format(fechaServidor)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            fechaSeleccionada = dateFormatterURL.format(fechaServidor)
        } else {
            fechaFormateada = "Error al obtener fecha"
            fechaSeleccionada = "0000-00-00"
        }
    }
    val urlHorario = BuildURL.mostrarHorarios +
            "&xEmpleado=$xEmpleado" +
            "&fecha=$fechaSeleccionada"



    return DatosHorario(
        fechaFormateada = fechaFormateada,
        fechaSeleccionada = fechaSeleccionada,
        xEmpleado = xEmpleado,
        urlHorario = urlHorario
    )
}


@Composable
fun MiHorario() {
    val context = LocalContext.current
    val datos = rememberDatosHorario()

    val urlHorario = datos.urlHorario

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
                    SeguridadUtils.isUsingVPN() -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }

                    SeguridadUtils.isMockLocationEnabled(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con ubicación simulada")
                        onShowAlert("POSIBLE UBI FALSA")
                        return@clickable
                    }

                    !SeguridadUtils.isInternetAvailable(context) -> {
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
                painter = painterResource(id = com.miapp.iDEMO_kairos24h.R.drawable.fichajeetrada32),
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
                    SeguridadUtils.isUsingVPN() -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }

                    SeguridadUtils.isMockLocationEnabled(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con ubicación simulada")
                        onShowAlert("POSIBLE UBI FALSA")
                        return@clickable
                    }

                    !SeguridadUtils.isInternetAvailable(context) -> {
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
                painter = painterResource(id = com.miapp.iDEMO_kairos24h.R.drawable.fichajesalida32),
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

@Composable
fun RecuadroFichajesDia(fichajes: List<String>, fecha: String) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val fechaSeleccionada = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
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

    // Log de la fecha usada para la petición
    Log.d("RecuadroFichajesDia", "Fecha usada para la petición: ${fechaSeleccionada.value}")

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    val fichajesTexto by produceState(initialValue = emptyList<String>(), key1 = fechaSeleccionada.value) {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val urlFichajes = BuildURL.mostrarFichajes +
                        "&xEmpleado=$xEmpleado" +
                        "&fecha=${fechaSeleccionada.value}"
                val request = Request.Builder().url(urlFichajes).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()?.replace("\uFEFF", "")

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("RecuadroFichajesDia", "Error: ${response.code}")
                    emptyList()
                } else {
                    try {
                        val json = JSONObject(responseBody)
                        val dataFichajes = json.getJSONObject("dataFichajes")
                        val fichajesArray = dataFichajes.getJSONArray("fichajes")
                        var menorEntrada: Int? = null
                        var mayorSalida: Int? = null

                        for (i in 0 until fichajesArray.length()) {
                            val item = fichajesArray.getJSONObject(i)
                            val nMinEntStr = item.optString("nMinEnt", "").trim()
                            val nMinSalStr = item.optString("nMinSal", "").trim()

                            val nMinEnt = nMinEntStr.toIntOrNull()
                            val nMinSal = nMinSalStr.toIntOrNull()

                            if (nMinEnt != null) {
                                if (menorEntrada == null || nMinEnt < menorEntrada) menorEntrada = nMinEnt
                            }

                            if (nMinSal != null) {
                                if (mayorSalida == null || nMinSal > mayorSalida) mayorSalida = nMinSal
                            }
                        }

                        fun minutosAHora(minutos: Int?): String {
                            return if (minutos != null) {
                                val horas = minutos / 60
                                val mins = minutos % 60
                                String.format("%02d:%02d", horas, mins)
                            } else {
                                "??"
                            }
                        }

                        buildList {
                            if (menorEntrada != null) add("Entrada: ${minutosAHora(menorEntrada)}")
                            if (mayorSalida != null) add("Salida: ${minutosAHora(mayorSalida)}")
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

    // --- Agregado: Declaración de datePickerDialog justo antes del Row de los botones del calendario ---
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
    // --- Fin agregado ---

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
                text = "Fecha: ${try {
                    val date = sdfEntrada.parse(fechaSeleccionada.value)
                    sdfSalida.format(date ?: Date())
                } catch (e: Exception) {
                    fechaSeleccionada.value
                }}",
                color = Color.Gray,
                fontSize = 16.sp,
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
                Icon(
                    painter = painterResource(id = R.drawable.reload),
                    contentDescription = "Usar fecha del servidor",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fichajesTexto.isNotEmpty()) {
                fichajesTexto.forEach { fichaje ->
                    Text(text = fichaje, color = Color(0xFF7599B6), fontSize = 18.sp)
                }
            } else {
                Text(text = "No hay fichajes hoy", color = Color.Gray, fontSize = 18.sp)
            }
        }
    }
}


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