package com.miapp.iDEMO_kairos24h.enlaces_internos

// Estás son las URL que se nos mostrarán en el WebView
    object WebViewURL {
    /**
     * Beiman = https://beimancpp.tucitamedica.es
     * Kairos24h = https://controlhorario.kairos24h.es
     * Máquina = http://localhost:8008/kairos24h
     */
    const val HOST = "https://democontrolhorario.kairos24h.es"
    const val ENTRY_POINT = "/index.php"
    const val URL_USADA = "$HOST$ENTRY_POINT"

    const val ACTION_LOGIN = "r=wsExterno/loginExterno"
    const val ACTION_FORGOTPASS = "r=site/solicitudRestablecerClave"
    const val ACTION_CONSULTAR = "r=explotacion/consultarExplotacion"

    const val LOGIN = "$URL_USADA?$ACTION_LOGIN"
    const val forgotPassword = "$URL_USADA?$ACTION_FORGOTPASS"

    const val Fichaje = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=FICHAJE"
    const val Incidencia = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=INCIDENCIA" +
            "&cOpcionVisual=INCBAN"
    const val Horarios = "$URL_USADA?" +
            "&cTipExp=HORARIO" +
            "&cModoVisual=HORMEN"
}

// Esta será la URL que construiremos cuando desde el login de nuestra APK introduzcamos el Usuario y la Contraseña
object BuildURL {
    /**
     * Beiman = https://beimancpp.tucitamedica.es
     * Kairos24h = https://controlhorario.kairos24h.es
     * Máquina = http://localhost:8008/kairos24h
     */
    /**
    https://democontrolhorario.kairos24h.es/index.php?r=wsExterno/consultarHorarioExterno&xGrupo=&xEntidad=1002&cKiosko=&cDomFicOri=APP
    http://localhost:8008/kairos24h/index.php?r=wsExterno/consultarFichajesExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19
    http://localhost:8008/kairos24h/index.php?r=wsExterno/consultarHorarioExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19
    http://localhost:8008/kairos24h/index.php?r=wsExterno/consultarAlertasExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19
    */
    /*==================================================================*/
    const val HOST = "https://democontrolhorario.kairos24h.es"
    const val ENTRY_POINT = "/index.php"
    const val ACTION_FICHAJE = "r=explotacion/creaFichaje"
    const val ACTION_LOGIN = "r=wsExterno/loginExterno"
    const val ACTION_CONSULTHORARIO = "r=wsExterno/consultarHorarioExterno"
    const val ACTION_CONSULTFIC_DIA = "r=wsExterno/consultarFichajesExterno"
    const val ACTION_CONSULT_ALERTAS = "r=wsExterno/consultarAlertasExterno"

    const val X_GRUPO = ""
    const val X_ENTIDAD = "1002"
    const val C_KIOSKO = ""
    const val C_FIC_ORI = "APP"

    const val staticParams =
            "&xGrupo=$X_GRUPO" +
            "&xEntidad=$X_ENTIDAD" +
            "&cKiosko=$C_KIOSKO" +
            "&cDomFicOri=$C_FIC_ORI"

    const val URL_USADA = "$HOST$ENTRY_POINT?"
    const val urlServidor = ""
    /*==================================================================*/

    /*--------------------------------------------------------*/
    const val LOGIN = URL_USADA + ACTION_LOGIN
    const val crearFichaje = URL_USADA + ACTION_FICHAJE + staticParams

    const val mostrarHorarios = URL_USADA + ACTION_CONSULTHORARIO + staticParams +
                "&xEmpleado="

    const val mostrarFichajes = URL_USADA + ACTION_CONSULTFIC_DIA + staticParams +
                "&xEmpleado="

    const val mostrarAlertas = ACTION_CONSULT_ALERTAS + staticParams +
                "&xEmpleado="

    // horqrio
    // http://localhost:8008/kairos24h/index.php?r=wsExterno/consultarHorarioExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19
}
