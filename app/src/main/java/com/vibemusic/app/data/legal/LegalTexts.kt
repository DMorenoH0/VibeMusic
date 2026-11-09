package com.vibemusic.app.data.legal

data class LegalDocument(
    val title: String,
    val updated: String,
    val intro: String,
    val sections: List<Pair<String, String>>
)

object LegalTexts {

    val privacy = LegalDocument(
        title = "Política de privacidad",
        updated = "7 de noviembre de 2026",
        intro = "VibeMusic es un reproductor de música local con un buscador de catálogo mediante la API pública de Deezer y una sección independiente que busca vídeos con YouTube Data API v3. Esta política describe exactamente qué datos trata la aplicación tal como está implementada.",
        sections = listOf(
            "Datos que se guardan en tu dispositivo" to
                "VibeMusic guarda localmente: tus preferencias (tema visual, escaneo automático, solo Wi-Fi), el historial de búsquedas, el historial de reproducción (últimas 20 canciones), favoritos, listas de reproducción, metadatos de archivos descargados o importados, el estado del aviso de actualización y, si inicias sesión con Google, tu identificador de cuenta, nombre y correo. Toda esta información permanece en el almacenamiento privado de la app.",
            "Datos que no se recogen" to
                "No se recogen contactos, llamadas, SMS, ubicación, cámara, micrófono, identificadores publicitarios ni datos de uso. No hay analítica, publicidad, perfiles ni rastreo entre aplicaciones. VibeMusic no envía tus archivos musicales a ningún servidor.",
            "Búsquedas online" to
                "Cuando buscas en el catálogo, el texto de búsqueda se envía a la API pública de Deezer (api.deezer.com) para obtener resultados. Deezer recibe también la dirección IP y un identificador de cliente genérico (User-Agent) de la petición. No se envía tu identidad, tu cuenta de Google ni el contenido de tu biblioteca. Las portadas y las vistas previas de 30 segundos se descargan de los CDN de Deezer.",
            "Búsquedas en YouTube" to
                "La sección YouTube usa YouTube Data API v3 de Google. La consulta solo se envía cuando pulsas Buscar o el botón del teclado; escribir no genera peticiones. Google recibe el texto buscado, la dirección IP, el User-Agent y la clave de API de la aplicación. No se envían tu identidad, tu cuenta de Google, tu biblioteca ni tus listas. Las miniaturas se descargan de los servidores de imágenes de YouTube. Los resultados se guardan solo en memoria y se borran al cerrar la app o al usar «Eliminar mis datos». Al abrir un vídeo, la reproducción ocurre en la app o web oficial de YouTube, sujeta a las condiciones y cookies de Google/YouTube.",
            "Actualizaciones" to
                "La app consulta periódicamente la API pública de GitHub (api.github.com) para comprobar si existe una versión estable publicada en el repositorio DMorenoH0/VibeMusic. GitHub recibe la dirección IP y el User-Agent de la petición. No se envía ningún dato personal. La descarga del APK solo se inicia cuando tú lo pides.",
            "Inicio de sesión con Google" to
                "La función es opcional y solo está activa si se ha configurado un ID de cliente OAuth. En ese caso, la autenticación se realiza con Google Identity Services a través de Credential Manager. VibeMusic conserva únicamente el identificador de cuenta, el nombre y el correo devueltos por Google; el token de identidad se procesa en memoria y no se almacena. Puedes cerrar sesión y borrar esos datos desde Ajustes. VibeMusic no usa una cuenta propia ni sincroniza tu música en la nube.",
            "Servicios de terceros" to
                "Deezer (catálogo, portadas y vistas previas), YouTube/Google (búsqueda de vídeos con YouTube Data API v3 y reproducción en el reproductor oficial), GitHub (versiones y descarga del APK) y Google (inicio de sesión, solo si lo configuras). No se integran redes sociales, anunciantes ni plataformas de análisis.",
            "Permisos de Android" to
                "Internet y estado de red (búsqueda, carátulas, actualizaciones), lectura de audio del dispositivo (escaneo de tu música), notificaciones (controles de reproducción) e instalación de paquetes (solo para el APK de actualización que tú confirmes). Ningún permiso se usa para fines distintos.",
            "Conservación y seguridad" to
                "Los datos permanecen en tu dispositivo hasta que los borras o desinstalas la app. No hay servidores propios de VibeMusic. Las comunicaciones con Deezer, YouTube/Google y GitHub usan HTTPS. La verificación de integridad del APK usa el hash SHA-256 publicado por GitHub cuando está disponible.",
            "Tus derechos y cómo eliminar tus datos" to
                "Puedes borrar el historial de búsquedas, el historial de reproducción, las listas, los favoritos, las preferencias, la caché y, opcionalmente, las copias de audio gestionadas por la app desde Ajustes › Privacidad y datos › «Eliminar mis datos». También puedes desinstalar la aplicación. Los archivos musicales originales del dispositivo nunca se eliminan sin tu autorización expresa.",
            "Menores y cambios" to
                "VibeMusic no está dirigida específicamente a menores ni recopila datos de forma consciente. Si cambia el funcionamiento de la app, esta política se actualizará en el repositorio antes de publicar la nueva versión.",
            "Contacto" to
                "Puedes plantear dudas o solicitudes de privacidad en el repositorio github.com/DMorenoH0/VibeMusic (issues)."
        )
    )

    val terms = LegalDocument(
        title = "Términos de servicio",
        updated = "7 de noviembre de 2026",
        intro = "Al usar VibeMusic aceptas estos términos. Si no estás de acuerdo, no utilices la aplicación.",
        sections = listOf(
            "Objeto" to
                "VibeMusic es un reproductor de audio para Android que organiza música local, archivos importados y descargas directas autorizadas por el usuario. Incluye un buscador de catálogo con vistas previas oficiales de 30 segundos y una sección YouTube que busca vídeos mediante YouTube Data API v3 y los reproduce en el reproductor oficial.",
            "Uso permitido" to
                "Puedes usar la app para reproducir tu propia música y contenido que tengas derecho a utilizar. Eres responsable de los archivos que importas o descargas y de respetar los derechos de autor aplicables en tu país.",
            "Contenido de terceros" to
                "Los metadatos, portadas y vistas previas del buscador pertenecen a Deezer y a los titulares de los derechos. Los resultados y miniaturas de YouTube pertenecen a YouTube y a sus creadores. VibeMusic no aloja, no distribuye canciones completas, no extrae audio de vídeos, no convierte vídeos a MP3, no descarga contenido de YouTube y no elude medidas de protección ni anuncios. La disponibilidad de ambos catálogos puede cambiar o interrumpirse sin aviso.",
            "Reproducción en YouTube" to
                "Los vídeos se abren en la aplicación o el sitio oficial de YouTube. La reproducción se rige por las condiciones de YouTube y puede incluir anuncios, restricciones de edad, limitaciones geográficas o controles del titular. VibeMusic no ofrece reproducción en segundo plano de YouTube ni intenta evitar esas restricciones.",
            "Descargas y actualizaciones" to
                "La descarga de audio solo se realiza desde enlaces directos que tú proporcionas. Las actualizaciones se descargan únicamente cuando pulsas el botón correspondiente y la instalación se confirma en el diálogo oficial de Android. VibeMusic no instala software de forma silenciosa.",
            "Cuenta de Google" to
                "El inicio de sesión es opcional. Ninguna función local de reproducción, biblioteca, listas o búsqueda requiere una cuenta. Si lo configuras, el uso de Google se rige también por las condiciones de Google.",
            "Exactitud y disponibilidad" to
                "La app se ofrece «tal cual». Puede haber errores, cambios de API o interrupciones de red. No se garantiza que el catálogo online esté siempre disponible ni que una actualización concreta funcione en todos los dispositivos.",
            "Limitación de responsabilidad" to
                "En la medida permitida por la ley, el proyecto no se hace responsable de daños derivados del uso de la app, de archivos de terceros o de la pérdida de datos. Realiza copias de seguridad de tu música.",
            "Cambios" to
                "Estos términos pueden actualizarse junto con nuevas versiones. La versión vigente se muestra en la app y en el repositorio.",
            "Contacto" to
                "Para consultas sobre estos términos, abre un issue en github.com/DMorenoH0/VibeMusic."
        )
    )

    val cookies = LegalDocument(
        title = "Política de cookies",
        updated = "7 de noviembre de 2026",
        intro = "Esta política explica el uso de cookies y tecnologías similares en la aplicación Android VibeMusic.",
        sections = listOf(
            "La app no utiliza cookies" to
                "VibeMusic es una aplicación nativa de Android. No incorpora un navegador web ni utiliza cookies HTTP, cookies de sesión, píxeles de seguimiento o almacenamiento web para publicidad o analítica. La interfaz se construye con Jetpack Compose y no carga páginas web.",
            "Almacenamiento local" to
                "La app usa SharedPreferences y una base de datos Room para guardar tus preferencias, historial, favoritos y listas. Esta información es almacenamiento local de la aplicación, no una cookie, y no se comparte con terceros.",
            "Servicios externos" to
                "Las peticiones a la API de Deezer, a YouTube Data API v3 y a la API de GitHub son peticiones HTTPS sin estado; VibeMusic no acepta ni envía cookies en ellas. Si abres un vídeo en la app o web oficial de YouTube, esa aplicación externa puede usar cookies propias conforme a las políticas de Google. El inicio de sesión con Google, si se configura, lo gestiona Play Services/Credential Manager conforme a las políticas de Google.",
            "Cómo eliminar estos datos" to
                "Puedes borrar el almacenamiento local desde Ajustes › Privacidad y datos › «Eliminar mis datos», o desde los ajustes de Android borrando los datos de la aplicación. Desinstalar VibeMusic elimina también su almacenamiento privado.",
            "Cambios" to
                "Si en el futuro se integrara contenido web que use cookies, esta política se actualizaría y se solicitaría el consentimiento correspondiente antes de su uso."
        )
    )

    fun document(key: String): LegalDocument = when (key) {
        "terms" -> terms
        "cookies" -> cookies
        else -> privacy
    }
}
