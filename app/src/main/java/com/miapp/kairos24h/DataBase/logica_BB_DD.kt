package com.example.relojfichajeskairos24h

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException

// Clase SQLiteHelper para gestionar la base de datos local fichajes_pendientes
class FichajesSQLiteHelper(context: Context) : SQLiteOpenHelper(context, "fichajes_pendientes", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        // Crea la tabla 'l_informados' para almacenar todos los fichajes
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS l_informados (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cEmpCppExt TEXT,
                xFichaje TEXT,
                cTipFic TEXT,
                fFichaje TEXT,
                hFichaje TEXT,
                L_INFORMADO TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='l_informados'",
                null
            )
            val exists = cursor.count > 0
            cursor.close()

            if (exists) {
                db.execSQL("ALTER TABLE l_informados ADD COLUMN cEmpCppExt TEXT")
                Log.d("modificacionBBDD", "Columna cEmpCppExt añadida a l_informados")
            } else {
                onCreate(db)
                Log.d("modificacionBBDD", "Tabla l_informados no existía, creada desde onUpgrade")
            }
        }
    }

    // Inserta un fichaje en la tabla l_informados
    fun insertarFichajeDesdeJson(json: JSONObject, codigoEmpleado: String) {
        val lInformado = json.optString("L_INFORMADO", "")
        val xFichaje = json.optString("xFichaje", "")

        Log.d("SQLite", "Insertando en tabla l_informados: L_INFORMADO=$lInformado, xFichaje=$xFichaje")

        if (xFichaje.isNotEmpty()) {
            val values = ContentValues().apply {
                put("cEmpCppExt", codigoEmpleado)
                put("xFichaje", xFichaje)
                put("cTipFic", json.optString("cTipFic", ""))
                put("fFichaje", json.optString("fFichaje", ""))
                put("hFichaje", json.optString("hFichaje", ""))
                put("L_INFORMADO", lInformado)
            }

            writableDatabase.insert("l_informados", null, values)
        }
    }
}

// Cliente HTTP y handler para ejecución periódica
private val client = OkHttpClient()
private val handler = Handler(Looper.getMainLooper())
private const val INTERVALO_REINTENTO = 10_000L // Intervalo de reintento: 10 segundos

// Inicia un bucle que cada 10 segundos reintenta enviar los fichajes no informados al servidor
fun iniciarReintentosAutomaticos(context: Context) {
    val dbHelper = FichajesSQLiteHelper(context)

    val tarea = object : Runnable {
        override fun run() {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM l_informados WHERE L_INFORMADO = 'N'", null)

            while (cursor.moveToNext()) {
                // Extrae cada columna del registro pendiente
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val cEmpCppExt = cursor.getString(cursor.getColumnIndexOrThrow("cEmpCppExt"))
                val xFichaje = cursor.getString(cursor.getColumnIndexOrThrow("xFichaje"))
                val cTipFic = cursor.getString(cursor.getColumnIndexOrThrow("cTipFic"))
                val fFichaje = cursor.getString(cursor.getColumnIndexOrThrow("fFichaje"))
                val hFichaje = cursor.getString(cursor.getColumnIndexOrThrow("hFichaje"))

                Log.d("ReintentoFichaje", "Preparando reenvío de fichaje con ID=$id")

                // Construye la URL de fichaje usando la plantilla y los datos del registro
                val url = BuildURL.HOST + BuildURL.ACTION +
                        "&cEmpCppExt=$cEmpCppExt&xFichaje=$xFichaje&cTipFic=$cTipFic&fFichaje=$fFichaje&hFichaje=$hFichaje"

                Log.d("ReintentoFichaje", "Invocando URL: $url")

                val request = Request.Builder().url(url).build()

                // Ejecuta la petición HTTP en segundo plano
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Si falla la conexión, no se hace nada (se reintentará después)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.body?.string()?.let { body ->
                            Log.d("ReintentoFichaje", "Respuesta recibida: $body")
                            try {
                                val json = JSONObject(body)
                                val lInformado = json.optString("L_INFORMADO", "")
                                // Si el servidor marca el fichaje como informado (L_INFORMADO = "S"), se actualiza el registro
                                if (lInformado == "S") {
                                    Log.d("ReintentoFichaje", "L_INFORMADO = S → Actualizando ID=$id a informado")

                                    val update = ContentValues().apply {
                                        put("L_INFORMADO", "S")
                                    }
                                    dbHelper.writableDatabase.update("l_informados", update, "id = ?", arrayOf(id.toString()))
                                }
                            } catch (_: Exception) {
                                // Si falla el parseo, se ignora y se intentará de nuevo
                            }
                        }
                    }
                })
            }

            cursor.close()
            // Reprograma la ejecución para dentro de 10 segundos
            handler.postDelayed(this, INTERVALO_REINTENTO)
        }
    }

    // Inicia el primer ciclo de reintento
    handler.postDelayed(tarea, INTERVALO_REINTENTO)
}

// Exporta toda la tabla l_informados a un archivo .csv accesible desde almacenamiento externo privado
// Ruta aproximada en el dispositivo: /storage/emulated/0/Android/data/com.example.relojfichajeskairos24h/files/l_informados.csv
// El archivo puede abrirse con Excel
fun exportarInformados(context: Context): File? {
    return exportarTablaAArchivoExterno(context)
}

// Función auxiliar que realiza la exportación de la tabla l_informados
private fun exportarTablaAArchivoExterno(context: Context): File? {
    val tabla = "l_informados"
    return try {
        val dbHelper = FichajesSQLiteHelper(context)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $tabla", null)

        val registros = StringBuilder()
        registros.append("ID,cEmpCppExt,xFichaje,cTipFic,fFichaje,hFichaje,L_INFORMADO\n")

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val cEmpCppExt = cursor.getString(cursor.getColumnIndexOrThrow("cEmpCppExt"))
            val xFichaje = cursor.getString(cursor.getColumnIndexOrThrow("xFichaje"))
            val cTipFic = cursor.getString(cursor.getColumnIndexOrThrow("cTipFic"))
            val fFichaje = cursor.getString(cursor.getColumnIndexOrThrow("fFichaje"))
            val hFichaje = cursor.getString(cursor.getColumnIndexOrThrow("hFichaje"))
            val lInformado = cursor.getString(cursor.getColumnIndexOrThrow("L_INFORMADO"))

            registros.append("$id,$cEmpCppExt,$xFichaje,$cTipFic,$fFichaje,$hFichaje,$lInformado\n")
        }

        cursor.close()

        val exportDir = context.getExternalFilesDir(null)
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val archivo = File(exportDir, "${tabla}_$fecha.csv")
        archivo.writeText(registros.toString())

        Log.d("EXPORTACION", "Archivo generado en: ${archivo.absolutePath}")
        archivo
    } catch (e: Exception) {
        Log.e("EXPORTACION", "Error al exportar la tabla $tabla: ${e.message}")
        null
    }
}