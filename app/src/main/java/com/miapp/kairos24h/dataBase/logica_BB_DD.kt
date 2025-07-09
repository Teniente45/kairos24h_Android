/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

// ==========================
// GUÍA DE LOGS EN ESTE ARCHIVO
// ==========================
// DEBUG_SQLITE
// - Muestra todos los registros actuales en la tabla `tablet_pendientes`.
// - También muestra errores al intentar enviar fichajes desde SQLite.
//
// DEBUG_URL
// - Muestra la URL que se ha construido con los datos de un fichaje pendiente para ser enviada al servidor.
//
// DEBUG_ENVIO
// - Informa que un fichaje se ha enviado correctamente al servidor.
//
// DEBUG_SQLITE (error)
// - Captura y muestra la excepción lanzada si ocurre un error durante el envío de un fichaje.
//
// ==========================

package com.miapp.kairos24h.dataBase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Crear base de datos
@Suppress("DEPRECATION")
class FichajesSQLiteHelper(context: Context) : SQLiteOpenHelper(
    context,
    "tablet_empleados_pendientes",
    null,
    1
) {

    // Crear la tabla para los fichajes hechos offline
    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS tablet_pendientes (
                xEntidad TEXT,
                cKiosko TEXT,
                cEmpCppExt TEXT,
                cTipFic TEXT CHECK(cTipFic IN ('ENTRADA', 'SALIDA')),
                fFichajeOffline TEXT,
                hFichaje TEXT,
                lGpsLat REAL,
                lGpsLon REAL,
                code1 TEXT CHECK(code1 IN ('SI', 'NO')) DEFAULT 'NO'
            );
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No hay lógica de actualización por ahora
    }

    fun insertarFichajePendiente(
        xEntidad: String,
        cKiosko: String,
        cEmpCppExt: String,
        cTipFic: String,
        fFichajeOffline: String,
        hFichaje: String,
        lGpsLat: Double,
        lGpsLon: Double,
        code1: String = "SI"
    ) {
        // Todos los fichajes se guardan en SQLite, incluso si se hacen online.
        // La clave de deduplicación debe estar en controlar el flujo de guardado según el resultado del servidor.
        val db = writableDatabase
        val insertQuery = """
            INSERT INTO tablet_pendientes (
                xEntidad, cKiosko, cEmpCppExt, cTipFic, fFichajeOffline, hFichaje, lGpsLat, lGpsLon, code1
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val statement = db.compileStatement(insertQuery)
        statement.bindString(1, xEntidad)
        statement.bindString(2, cKiosko)
        statement.bindString(3, cEmpCppExt)
        statement.bindString(4, cTipFic)
        statement.bindString(5, fFichajeOffline)
        statement.bindString(6, hFichaje)
        statement.bindDouble(7, lGpsLat)
        statement.bindDouble(8, lGpsLon)
        statement.bindString(9, code1)

        statement.executeInsert()
        statement.close()
        db.close()
    }


    fun enviarFichajesPendientes(context: Context) {
        val fichajes = obtenerFichajesPendientesNoInformados()
        val client = okhttp3.OkHttpClient()
        val db = writableDatabase

        for (fichaje in fichajes) {
            val host = com.miapp.kairos24h.enlaces_internos.BuildURLmovil.getHost(context)
            val url = "$host/index.php?r=wsExterno/crearFichajeExterno" +
                "&xEntidad=${fichaje.xEntidad}" +
                "&cKiosko=${fichaje.cKiosko}" +
                "&cEmpCppExt=${fichaje.cEmpCppExt}" +
                "&cTipFic=${fichaje.cTipFic}" +
                "&fFichaje=${fichaje.fFichajeOffline}" +
                "&hFichaje=${fichaje.hFichaje}" +
                "&tGpsLat=${fichaje.lGpsLat}" +
                "&tGpsLon=${fichaje.lGpsLon}" +
                "&cFicOri=PUEFIC"

            android.util.Log.d("DEBUG_URL", "URL formada: $url")

            val request = okhttp3.Request.Builder().url(url).build()

            Thread {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()?.trim()
                    android.util.Log.d("DEBUG_SQLITE", "Respuesta del servidor: '$responseBody'")

                    if (!responseBody.isNullOrBlank() && responseBody.startsWith("{")) {
                        val responseJson = org.json.JSONObject(responseBody)
                        val code = responseJson.optString("code", "")
                        if (code == "1") {
                            android.util.Log.d("DEBUG_SQLITE", "Fichaje con ID ${fichaje.id} confirmado con éxito por el servidor.")
                        } else {
                            eliminarFichaje(db, fichaje.id)
                            android.util.Log.d("DEBUG_SQLITE", "Fichaje con ID ${fichaje.id} eliminado por respuesta inválida")
                        }
                    } else {
                        // Consideramos que si no es JSON, pero no está vacío, el servidor respondió éxito
                        actualizarEstadoFichaje(db, fichaje.id)
                        android.util.Log.d("DEBUG_SQLITE", "Fichaje con ID ${fichaje.id} enviado con respuesta no JSON: '$responseBody'")
                        return@Thread
                    }
                } catch (e: Exception) {
                    val values = android.content.ContentValues().apply {
                        put("code1", "NO")
                    }
                    db.update("tablet_pendientes", values, "rowid=?", arrayOf(fichaje.id.toString()))
                    android.util.Log.e("DEBUG_SQLITE", "Excepción al enviar fichaje con ID ${fichaje.id}. Marcado como NO.", e)
                }
            }.start()
        }
    }

    fun actualizarEstadoFichaje(db: SQLiteDatabase, id: Long) {
        val values = android.content.ContentValues().apply {
            put("code1", "SI")
        }
        db.update("tablet_pendientes", values, "rowid=?", arrayOf(id.toString()))
    }

    fun registrarNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                enviarFichajesPendientes(context)
            }
        }

        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
    fun enviarFichajesPendientesSiHayInternet(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val isConnected = connectivityManager.activeNetworkInfo?.isConnected == true

        if (isConnected) {
            enviarFichajesPendientes(context)
        }
    }

    fun obtenerFichajesPendientesNoInformados(): List<FichajePendiente> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT rowid, * FROM tablet_pendientes WHERE code1 = 'NO'", null)
        val lista = mutableListOf<FichajePendiente>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("rowid"))
            val xEntidad = cursor.getString(cursor.getColumnIndexOrThrow("xEntidad"))
            val cKiosko = cursor.getString(cursor.getColumnIndexOrThrow("cKiosko"))
            val cEmpCppExt = cursor.getString(cursor.getColumnIndexOrThrow("cEmpCppExt"))
            val cTipFic = cursor.getString(cursor.getColumnIndexOrThrow("cTipFic"))
            val fFichaje = cursor.getString(cursor.getColumnIndexOrThrow("fFichajeOffline"))
            val hFichaje = cursor.getString(cursor.getColumnIndexOrThrow("hFichaje"))
            val lGpsLat = cursor.getDouble(cursor.getColumnIndexOrThrow("lGpsLat"))
            val lGpsLon = cursor.getDouble(cursor.getColumnIndexOrThrow("lGpsLon"))
            val code1 = cursor.getString(cursor.getColumnIndexOrThrow("code1"))

            lista.add(
                FichajePendiente(
                    id = id,
                    xEntidad = xEntidad,
                    cKiosko = cKiosko,
                    cEmpCppExt = cEmpCppExt,
                    cTipFic = cTipFic,
                    fFichajeOffline = fFichaje,
                    hFichaje = hFichaje,
                    lGpsLat = lGpsLat,
                    lGpsLon = lGpsLon,
                    code1 = code1
                )
            )
        }

        android.util.Log.d("DEBUG_SQLITE", "REGISTROS TODAVIA PENDIENTES POR ENVIAR:")
        for ((index, fichaje) in lista.withIndex()) {
            android.util.Log.d("DEBUG_SQLITE", "$index: $fichaje")
        }

        cursor.close()
        db.close()
        return lista
    }

    fun mostrarTodosLosFichajes() {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT rowid, * FROM tablet_pendientes", null)
        val lista = mutableListOf<FichajePendiente>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("rowid"))
            val xEntidad = cursor.getString(cursor.getColumnIndexOrThrow("xEntidad"))
            val cKiosko = cursor.getString(cursor.getColumnIndexOrThrow("cKiosko"))
            val cEmpCppExt = cursor.getString(cursor.getColumnIndexOrThrow("cEmpCppExt"))
            val cTipFic = cursor.getString(cursor.getColumnIndexOrThrow("cTipFic"))
            val fFichaje = cursor.getString(cursor.getColumnIndexOrThrow("fFichajeOffline"))
            val hFichaje = cursor.getString(cursor.getColumnIndexOrThrow("hFichaje"))
            val lGpsLat = cursor.getDouble(cursor.getColumnIndexOrThrow("lGpsLat"))
            val lGpsLon = cursor.getDouble(cursor.getColumnIndexOrThrow("lGpsLon"))
            val code1 = cursor.getString(cursor.getColumnIndexOrThrow("code1"))

            lista.add(
                FichajePendiente(
                    id = id,
                    xEntidad = xEntidad,
                    cKiosko = cKiosko,
                    cEmpCppExt = cEmpCppExt,
                    cTipFic = cTipFic,
                    fFichajeOffline = fFichaje,
                    hFichaje = hFichaje,
                    lGpsLat = lGpsLat,
                    lGpsLon = lGpsLon,
                    code1 = code1
                )
            )
        }

        android.util.Log.d("DEBUG_SQLITE", "TODOS LOS REGISTROS ENVIADOS CORRECTAMENTE AL SERVIDOR:")
        for ((index, fichaje) in lista.withIndex()) {
            android.util.Log.d("DEBUG_SQLITE", "$index: $fichaje")
        }

        cursor.close()
        db.close()
    }

    fun eliminarFichaje(db: SQLiteDatabase, id: Long) {
        db.delete("tablet_pendientes", "rowid=?", arrayOf(id.toString()))
    }
}

data class FichajePendiente(
    val id: Long,
    val xEntidad: String,
    val cKiosko: String,
    val cEmpCppExt: String,
    val cTipFic: String,
    val fFichajeOffline: String,
    val hFichaje: String,
    val lGpsLat: Double,
    val lGpsLon: Double,
    val code1: String
)