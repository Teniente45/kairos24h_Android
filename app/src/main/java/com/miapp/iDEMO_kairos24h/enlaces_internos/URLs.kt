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
    const val urlServidor = "https://tuservidor.com/api/fichajes" //CAMBIAR POR URL PARA MOSTRAR LOS FICHAJES
    const val FichSalida = "www.caca.es"
    const val FichEntrada = "www.culo.es"
    /*==================================================================*/
    const val URL_USADA = URL_DEMO_Kairos24h
    /*--------------------------------------------------------*/
    const val LOGIN = URL_USADA + "r=wsExterno/"
    /*--------------------------------------------------------*/
}
