/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

package com.miapp.iDEMO_kairos24h

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.ImagenesApp
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificamos si es el primer arranque
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)
        if (isFirstRun) {
            // Limpiamos las credenciales para que se muestre siempre la pantalla de login en el primer arranque
            clearCredentials()
            prefs.edit { putBoolean("first_run", false) }
        }

        // Obtenemos las credenciales almacenadas (usuario, password y xEmpleado)
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)

        if (storedUser.isNotEmpty() && storedPassword.isNotEmpty()) {
            // Si existen credenciales, redirigimos a Fichar directamente
            navigateToFichar(storedUser, storedPassword)
        } else {
            // Si no existen credenciales, mostramos la pantalla de inicio de sesión
            setContent {
                MaterialTheme {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            DisplayLogo(
                                onSubmit = { usuario: String, password: String ->
                                    if (!isInternetAvailable(this@MainActivity)) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Compruebe su conexión a Internet",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@DisplayLogo
                                    }

                                    // Si el usuario y la contraseña no están vacíos, iniciamos el proceso de autenticación
                                    if (usuario.isNotEmpty() && password.isNotEmpty()) {
                                        // Ejecuta la autenticación en un hilo de fondo usando coroutine
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val (success, xEmpleado) = AuthManager.authenticateUser(
                                                    usuario,
                                                    password
                                                )
                                                runOnUiThread {
                                                    // Si la autenticación es exitosa y se obtiene xEmpleado, se guardan las credenciales y se navega a la pantalla de fichaje
                                                    if (success && xEmpleado != null) {
                                                        AuthManager.saveUserCredentials(
                                                            this@MainActivity,
                                                            xEmpleado.usuario,
                                                            xEmpleado.password,
                                                            xEmpleado.xEmpleado,
                                                            xEmpleado.lComGPS,
                                                            xEmpleado.lComIP,
                                                            xEmpleado.lBotonesFichajeMovil,
                                                            xEmpleado.xEntidad
                                                        )
                                                        navController.navigate("fichar/${xEmpleado.usuario}/${xEmpleado.password}")
                                                    } else {
                                                        // Si no se autentica correctamente, se muestra un mensaje de error al usuario
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Usuario o contraseña incorrectos",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Captura errores de red u otros problemas de autenticación y los muestra como un Toast
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Error de autenticación: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        // Si los campos de usuario o contraseña están vacíos, se muestra un aviso al usuario
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Por favor, completa ambos campos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onForgotPassword = {
                                    val url = WebViewURL.FORGOT_PASSWORD
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    startActivity(intent)
                                }
                            )
                        }
                        composable("fichar/{usuario}/{password}") { backStackEntry ->
                            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
                            val password = backStackEntry.arguments?.getString("password") ?: ""

                            FicharScreen(
                                usuario = usuario,
                                password = password,
                                onLogout = { navigateToLogin() }
                            ) // 🔥 Se pasa un valor vacío
                        }
                    }
                }
            }
        }
    }

    // Redirige al usuario a la pantalla de login y limpia las preferencias
    private fun navigateToLogin() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // Inicia la actividad Fichar pasando usuario y contraseña como extras
    private fun navigateToFichar(usuario: String, password: String) {
        val intent = Intent(this, Fichar::class.java)
        intent.putExtra("usuario", usuario)
        intent.putExtra("password", password)
        startActivity(intent)
        finish() // Finaliza la actividad actual para evitar volver atrás
    }


    // Borra credenciales almacenadas para forzar nuevo login
    private fun clearCredentials() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("usuario")
            remove("password")
            remove("xEmpleado")
            apply()
        }
    }
    // Verifica si el dispositivo tiene conexión a Internet activa, ya sea por WiFi o datos móviles.
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}

// Composable que representa la pantalla de login, mostrando el formulario de acceso, gestión de permisos y logo de la app.
@Composable
fun DisplayLogo(
    onSubmit: (String, String) -> Unit,
    onForgotPassword: () -> Unit
) {
    // Estado que almacena el texto ingresado en el campo de usuario
    val usuario = remember { mutableStateOf("") }
    // Estado que almacena el texto ingresado en el campo de contraseña
    val password = remember { mutableStateOf("") }

    // Controla si el usuario acepta guardar sus datos localmente
    var isChecked by remember { mutableStateOf(false) }
    // Controla si la contraseña se muestra en texto plano o se oculta
    var passwordVisible by remember { mutableStateOf(false) }
    // Mensaje de error a mostrar si los datos de acceso son incorrectos
    val errorMessage by remember { mutableStateOf("") }
    // Obtiene el contexto actual de la aplicación
    val context = LocalContext.current
    // Indica si el usuario ha concedido el permiso de ubicación
    var isLocationChecked by remember { mutableStateOf(false) }
    // Lleva la cuenta de las veces que el permiso de ubicación ha sido denegado
    var locationPermissionDeniedCount by remember { mutableIntStateOf(0) }
    // Lanza la solicitud de permiso de ubicación y maneja su resultado
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationChecked = isGranted
        if (!isGranted) {
            locationPermissionDeniedCount++
            if (locationPermissionDeniedCount >= 3) {
                Toast.makeText(context, "Debe habilitar los permisos de GPS manualmente en los ajustes.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Debe aceptar los permisos de GPS para continuar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Contenedor principal del contenido de login, desplazado hacia arriba
        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset(y = (-40).dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Imagen del logo de la empresa o aplicación
                Image(
                    painter = painterResource(id = ImagenesApp.logoCliente),
                    contentDescription = "logo del cliente",
                    modifier = ImagenesApp.logoModifier
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo de texto para el nombre de usuario
                OutlinedTextField(
                    value = usuario.value,
                    onValueChange = { newValue ->
                        usuario.value = newValue.filter { it != ' ' } // Elimina espacios en blanco
                    },
                    label = { Text("Usuario") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo de texto para la contraseña
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { newValue ->
                        password.value = newValue.filter { it != ' ' } // Elimina espacios en blanco
                    },
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )

                // Opción para mostrar u ocultar la contraseña introducida
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it }
                    )
                    Text(
                        text = "Mostrar contraseña",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Opción para permitir guardar los datos de acceso localmente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )
                    Text(
                        text = "Doy mi consentimiento para guardar mis datos localmente en mi dispositivo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Muestra un mensaje de error si existe
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Solicita al usuario que acepte el permiso de ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = isLocationChecked,
                        onCheckedChange = { checked ->
                            if (checked) {
                                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                isLocationChecked = false
                            }
                        }
                    )
                    Text(
                        text = "Acepto que la app acceda a la ubicación donde ficho",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Botón para enviar los datos de acceso
                Button(
                    onClick = {
                        val trimmedUsuario = usuario.value.trim()
                        val trimmedPassword = password.value.trim()

                        // Si los campos están completos, codifica las credenciales y las envía
                        if (trimmedUsuario.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            val encodedUsuario = URLEncoder.encode(trimmedUsuario, StandardCharsets.UTF_8.toString())
                            val encodedPassword = URLEncoder.encode(trimmedPassword, StandardCharsets.UTF_8.toString())
                            onSubmit(encodedUsuario, encodedPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7599B6)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = usuario.value.isNotEmpty() && password.value.isNotEmpty() && isChecked && isLocationChecked
                ) {
                    Text("Acceso", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Texto interactivo para recuperar la contraseña
                Text(
                    text = "¿Olvidaste la contraseña?",
                    color = Color(0xFF7599B6),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onForgotPassword() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mensaje informativo sobre la trazabilidad y seguridad del sistema
                Text(
                    text = """
                        Para control de calidad y aumentar la seguridad de nuestro sistema, todos los accesos, acciones, consultas o cambios (Trazabilidad) que realice dentro de Kairos24h serán almacenados.
                        Les recordamos que la Empresa podrá auditar los medios técnicos que pone a disposición del Trabajador para el desempeño de sus funciones.
                    """.trimIndent(),
                    color = Color(0xFF447094),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// Vista previa para Android Studio
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        DisplayLogo(onSubmit = { _: String, _: String -> }, onForgotPassword = {})
    }
}

