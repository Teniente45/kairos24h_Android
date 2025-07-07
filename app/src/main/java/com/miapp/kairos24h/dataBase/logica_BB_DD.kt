package com.example.kairos24h

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TabletEmpleadosPendientesHelper(context: Context) : SQLiteOpenHelper(
    context,
    "tablet_empleados_pendientes",
    null,
    1
) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS teblet_pendientes (
                xEntidad TEXT,
                lEstado TEXT CHECK(lEstado IN ('Pendiente', 'No Pendiente')),
                cKiosko TEXT,
                f_fichaje DATE,
                lGpsLat REAL,
                lGpsLon REAL,
                xEmpleado TEXT
            );
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No hay lógica de actualización por ahora
    }
}