package com.miapp.iDEMO_kairos24h.enlaces_internos

// Estás son las URL que se nos mostrarán en el WebView
object WebViewURL {
    /*==================================================================*/
    const val URL_BEIMAN = "https://beimancpp.tucitamedica.es/index.php?"
    const val URL_Kairos24h = "https://controlhorario.kairos24h.es/index.php?"
    const val URL_DEMO_Kairos24h = "https://democontrolhorario.kairos24h.es/index.php?"
    /*==================================================================*/
    const val URL_USADA = URL_DEMO_Kairos24h
    /*--------------------------------------------------------*/
    const val LOGIN = URL_USADA + "r=site/login&0%5BxEntidad%5D="
    const val forgotPassword = URL_USADA + "r=site/solicitudRestablecerClave"
    const val Inicio = URL_USADA + "r=site/login&xEntidad=1002&cApp=APP_CPP"
    const val Fichaje = URL_USADA + "r=explotacion/consultarExplotacion&cTipExp=FICHAJE"
    const val Incidencia = URL_USADA + "r=explotacion/consultarExplotacion&cTipExp=INCIDENCIA&cOpcionVisual=INCBAN"
    const val Horarios = URL_USADA + "r=explotacion/consultarExplotacion&cTipExp=HORARIO&cModoVisual=HORMEN"
    /*--------------------------------------------------------*/
}

// Esta será la URL que construiremos cuando desde el login de nuestra APK introduzcamos el Usuario y la Contraseña
object BuildURL {

    /*==================================================================*/
    const val URL_BEIMAN = "https://beimancpp.tucitamedica.es/index.php?"
    const val URL_Kairos24h = "https://controlhorario.kairos24h.es/index.php?"
    const val URL_DEMO_Kairos24h = "https://democontrolhorario.kairos24h.es/index.php?"
    const val prueba = "http://192.168.25.67:8008/kairos24h/index.php?r=citaRedWeb/crearFichajeExterno&xGrupo=&xEntidad=3&cKiosko=&cFicOri=APP&cEmpCppExt=135&fFichaje="

    const val urlServidor = URL_DEMO_Kairos24h + "r=citaRedWeb/crearFichajeExterno&xGrupo=&xEntidad=1002&cKiosko=&cFicOri=APP"
    /*==================================================================*/
    const val URL_USADA = URL_DEMO_Kairos24h
    /*--------------------------------------------------------*/
    const val LOGIN = URL_USADA + "r=wsExterno/"
    const val crearFichaje = URL_USADA + "r=explotacion/creaFichaje"
}

// Aquí están las URLs que se encargarán de recoger los valores del servidor para los fichajes, horarios y alertas
object RecogedorURL {
    const val host = "http://192.168.25.67:8008/kairos24h/"
    const val action = "index.php?r=wsExterno/"
    const val PLogin = "loginExterno&cUsuario=comadmin&tPassword=i3data1"
    const val PFichaje = "consultarFichajesExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19"
    const val PHorario = "consultarHorarioExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19"
    const val PAlerta = "consultarAlertasExterno&xEntidad=3&xEmpleado=413&fecha=2025-03-19"

//====================================================
    const val Login = host + action + PLogin
    const val Fichaje = host + action + PFichaje
    const val Horario = host + action + PHorario
    const val Alerta = host + action + PAlerta
//====================================================

}




