/*******************************
 * LOGS DE FICHAR.KT
 *******************************/

/**
 *  // Log de depuración que indica que se ha iniciado el método onCreate
 *  Log.d("Fichar", "onCreate iniciado")
 *
 *  // Error si no se tiene el permiso de GPS requerido para fichar
 *  Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
 *
 *  // Error si las coordenadas GPS son 0.0, indicando ubicación no válida
 *  Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
 *
 *  // Muestra en el log la URL de fichaje que se va a ejecutar en el WebView
 *  Log.d("Fichar", "URL que se va a enviar desde WebView: $urlFichaje")
 *
 *  // Error que registra el tipo de alerta recibida durante el proceso de fichaje
 *  Log.e("Fichar", "Alerta: $alertTipo")
 *
 *  // Error capturado si hay una excepción de seguridad al intentar obtener coordenadas
 *  Log.e("Fichar", "Error de seguridad al acceder a la ubicación: ${e.message}")
 *
 *  // Muestra los valores de configuración de seguridad obtenidos del AuthManager
 *  Log.d("Seguridad", "lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil")
 *
 *  // Advertencia si el fichaje está deshabilitado por GPS
 *  Log.w("Seguridad", "El fichaje está deshabilitado por GPS: lComGPS=$lComGPS")
 *
 *  // Advertencia si el fichaje está deshabilitado por IP
 *  Log.w("Seguridad", "El fichaje está deshabilitado por IP: lComIP=$lComIP")
 *
 *  // Advertencia si los botones de fichaje están deshabilitados
 *  Log.w("Seguridad", "Los botones de fichaje están deshabilitados: lBotonesFichajeMovil=$lBotonesFichajeMovil")
 *
 *  // Error si el GPS del dispositivo está desactivado
 *  Log.e("Fichar", "GPS desactivado.")
 *
 *  // Error si la ubicación no se pudo recuperar desde el GPS
 *  Log.e("Fichar", "No se pudo obtener la ubicación.")
 *
 *  // Error si se detecta que la ubicación es simulada (mock location)
 *  Log.e("Fichar", "Ubicación falsa detectada.")
 *
 *  // Error si ocurre una excepción al obtener la ubicación del dispositivo
 *  Log.e("Fichar", "Error obteniendo ubicación: ${e.message}")
 */

/*******************************
 * LOGS DE PANTALLAFUNCIONAL.KT
 *******************************/

/**
 * // Muestra en el log la URL que se va a usar para consultar el horario del usuario
 * Log.d("MiHorario", "URL solicitada: $urlHorario")
 *
 * // Muestra la respuesta completa del servidor tras pedir el horario
 * Log.d("MiHorario", "Respuesta completa del servidor:\n$responseBody")
 *
 * // Muestra el valor obtenido del campo N_HORINI en el JSON
 * Log.d("MiHorario", "Valor N_HORINI: $horaIni")
 *
 * // Muestra el valor obtenido del campo N_HORFIN en el JSON
 * Log.d("MiHorario", "Valor N_HORFIN: $horaFin")
 *
 * // Informa si hubo un error al parsear el JSON del horario
 * Log.e("MiHorario", "Error al parsear JSON: ${e.message}\nResponse body: $responseBody")
 *
 * // Error al obtener horario
 * Log.e("MiHorario", "Error al obtener horario: ${e.message}")
 *
 * // Informa que se ha concedido el permiso para fichar, se indica el tipo (ENTRADA/SALIDA)
 * Log.d("Fichaje", "Permiso concedido. Procesando fichaje de: $tipo")
 *
 * // Informa que el WebView es null y por eso no se puede proceder con el fichaje
 * Log.e("Fichaje", "webView es null. No se puede fichar.")
 *
 * // Informa que se ha denegado el permiso de ubicación
 * Log.d("Fichaje", "Permiso denegado para ACCESS_FINE_LOCATION")
 *
 * // Informa que se ha intentado fichar con VPN activa
 * Log.e("Seguridad", "Intento de fichaje con VPN activa")
 *
 * // Informa que no hay conexión a internet en el momento del fichaje
 * Log.e("Fichar", "No hay conexión a Internet")
 *
 * // Informa que no se tiene permiso de ubicación GPS
 * Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
 *
 * // Informa que el GPS está desactivado
 * Log.e("Seguridad", "GPS desactivado")
 *
 * // Informa que se detectó una ubicación simulada
 * Log.e("Seguridad", "Ubicación simulada detectada")
 *
 * // Previene fichajes duplicados en corto intervalo de tiempo
 * Log.w("Fichaje", "Fichaje repetido ignorado")
 *
 * // Informa que se está procesando el fichaje de ENTRADA
 * Log.d("Fichaje", "Fichaje Entrada: Permiso concedido. Procesando fichaje de ENTRADA")
 *
 * // Informa que se está procesando el fichaje de SALIDA
 * Log.d("Fichaje", "Fichaje Salida: Permiso concedido. Procesando fichaje de SALIDA")
 *
 * // Muestra la fecha que se está usando para consultar los fichajes del día
 * Log.d("RecuadroFichajesDia", "Fecha usada para la petición: ${fechaSeleccionada.value}")
 *
 * // Muestra la URL completa que se usa para obtener los fichajes
 * Log.d("RecuadroFichajesDia", "URL completa invocada: $urlFichajes")
 *
 * // Muestra la respuesta del servidor con los fichajes recibidos
 * Log.d("RecuadroFichajesDia", "Respuesta desde consultarFichajeExterno (URL: ${response.request.url}): $responseBody")
 *
 * // Muestra los valores obtenidos para cada fichaje (entrada, salida y cumplimiento)
 * Log.d("RecuadroFichajesDia", "Fichaje $i → nMinEnt: $nMinEnt, nMinSal: $nMinSal, LCUMENT: $lcumEnt, LCUMSAL: $lcumSal")
 *
 * // Informa que hubo un error al parsear el JSON de los fichajes
 * Log.e("RecuadroFichajesDia", "Error al parsear JSON: ${e.message}")
 *
 * // Error al obtener fichajes
 * Log.e("RecuadroFichajesDia", "Error al obtener fichajes: ${e.message}")
 *
 * // Muestra la URL que se utilizará para obtener alertas diarias
 * Log.d("AlertasDiarias", "URL de alertas: $urlAlertas")
 *
 * // Muestra el contenido del campo D_AVISO de cada alerta. Posición $i
 * Log.d("JSONAlertas", "D_AVISO: $dAviso")
 *
 * // Muestra el contenido del campo T_AVISO de cada alerta. Posición $i
 * Log.d("JSONAlertas", "T_AVISO: $tAviso")
 *
 * // Muestra el contenido del campo T_URL de cada alerta (si existe). Posición $i
 * Log.d("JSONAlertas", "T_URL: $tUrl")
 *
 * // Informa que el array de alertas vino vacío o nulo
 * Log.d("JSONAlertas", "Array 'dataAvisos' vacío o nulo")
 *
 * // Informa si hubo un error general al obtener las alertas
 * Log.e("AlertasDiarias", "Error obteniendo alertas: ${e.message}")
 */