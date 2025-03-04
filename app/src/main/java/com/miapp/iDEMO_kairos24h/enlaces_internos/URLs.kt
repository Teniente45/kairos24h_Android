package com.miapp.iDEMO_kairos24h.enlaces_internos


// Estás son las URL que se nos mostrarán en el WebView
object WebViewURL {
    /*==================================================================*/
    const val PRODUCCION_BEIMAN = "https://beimancpp.tucitamedica.es/index.php?r=site/login&0%5BxEntidad%5D="
    const val forgotPassword_BEIMAN = "https://beimancpp.tucitamedica.es/index.php?r=site/solicitudRestablecerClave"
    /*==================================================================*/
    const val PRODUCCION_Kairos24h = "https://controlhorario.kairos24h.es/index.php?r=site/login&0%5BxEntidad%5D="
    const val DEMO_Kairos24h = "https://democontrolhorario.kairos24h.es/index.php?r=site/login&0%5BxEntidad%5D="
    const val forgotPassword_Kairos24h = "https://www.controlhorario.kairos24h.es/index.php?r=site/solicitudRestablecerClave"
    /*==================================================================*/

    /*--------------------------------------------------------*/
    const val LOGIN_URL = DEMO_Kairos24h
    const val forgotPassword = forgotPassword_Kairos24h
    /*--------------------------------------------------------*/
}

// Esta será la URL que construiremos cuando desde el login de nuestra APK introduzcamos el Usuario y la Contraseña
object BuildURL {
    /*==================================================================*/
    const val PRODUCCION_BEIMAN = "https://beimancpp.tucitamedica.es/index.php?r=wsExterno/"
    /*==================================================================*/
    const val PRODUCCION_Kairos24h = "https://controlhorario.kairos24h.es/index.php?r=wsExterno/"
    const val DEMO_Kairos24h = "https://democontrolhorario.kairos24h.es/index.php?r=wsExterno/"
    /*==================================================================*/

    /*--------------------------------------------------------*/
    const val LOGIN_URL = DEMO_Kairos24h
    /*--------------------------------------------------------*/
}
