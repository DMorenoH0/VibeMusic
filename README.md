# VibeMusic

Reproductor de música para Android con interfaz Compose, biblioteca local y un
buscador de catálogo que reproduce **vistas previas oficiales de 30 segundos**
de Deezer. Incluye una sección **YouTube** que busca vídeos con YouTube Data
API v3 y los reproduce dentro de la app con el reproductor oficial embebido
(YouTube IFrame Player API en un WebView). No extrae audio de plataformas de
streaming ni descarga canciones completas de catálogos protegidos.

## Funciones

### Navegación
- Barra inferior fija de cuatro apartados: **Home**, **Samples**, **Explore** y
  **Library**.
- Estilo minimalista sobre `#171717`: iconos rellenos en blanco para el
  apartado activo, delineados en gris claro para el resto, texto pequeño y
  transición de color y escala de 180 ms.
- Altura compacta, cuatro elementos de igual ancho y respeto de los insets de
  navegación por gestos o botones de Android.
- La barra permanece visible durante la navegación principal; las pantallas de
  detalle (reproductor, cola, ajustes, búsqueda y YouTube) se abren encima y se
  cierran con atrás o con su botón de volver.

### Reproducción y biblioteca
- Reproducción en segundo plano con Media3 ExoPlayer y MediaSession.
- Notificación multimedia y controles en pantalla de bloqueo.
- Reproducción, pausa, anterior, siguiente, aleatorio, repetir y velocidad.
- Cola de reproducción con salto, eliminación y vaciado.
- Escaneo real de audio local con MediaStore y permisos de Android 13+.
- Importación mediante el selector de archivos (SAF).
- Descargas de enlaces directos HTTPS (MP3, M4A, OGG, FLAC, WAV).
- Persistencia con Room: favoritos, listas, descargas e historial.
- Temporizador de sueño.

### Buscador local y online
- Búsqueda por título, artista y álbum en la biblioteca del dispositivo.
- Búsqueda en el catálogo público de Deezer con portada, artista, álbum y
  duración.
- Sugerencias e historial de búsquedas borrable.
- Acciones por resultado: reproducir vista previa, reproducir a continuación,
  ver álbum y ver artista.
- Pantallas de álbum y artista online con listas reales de Deezer.
- Estados de carga, vacío y error, con textos en español.
- Las vistas previas **no** se guardan en favoritos, listas ni historial.

### Samples
- Pestaña **Samples** con las seis instrumentales originales incluidas en la
  app, filtros por género y «Reproducir todo».
- Se instalan como archivos locales: reproducción completa, sin conexión y en
  segundo plano, con la misma cola y notificación que el resto de la música.

### YouTube
- Sección **YouTube** accesible desde **Explore** (no ocupa una quinta
  pestaña).
- Búsqueda por título, artista o álbum con YouTube Data API v3.
- Resultados con miniatura, canal y duración real (ISO-8601 de `videos.list`).
- La consulta solo se envía al pulsar Buscar o el botón del teclado; escribir
  no gasta cuota. Cada consulta correcta se guarda en caché en memoria.
- Al pulsar un resultado se abre una pantalla de reproducción propia con el
  reproductor oficial de YouTube integrado mediante la IFrame Player API en un
  WebView. Se conservan los controles, la marca, los anuncios y las
  restricciones de YouTube; no se abre YouTube ni Chrome.
- La pantalla muestra el reproductor arriba, título, canal, duración, fecha,
  descripción y un botón «Volver a los resultados». La navegación y los enlaces
  del reproductor se quedan dentro del WebView.
- Si el vídeo no existe, es privado o no permite reproducción integrada, la app
  reemplaza el reproductor por un mensaje explicativo en español.
- El vídeo se pausa al salir de la app y el WebView se destruye al volver a los
  resultados; la música local (Media3) sigue funcionando aparte.
- Estados de carga, error (cuota agotada, clave no válida, API deshabilitada,
  red), resultados vacíos y configuración pendiente.
- **No** se implementa extracción de audio, conversión a MP3, descarga de
  vídeos, reproducción en segundo plano por extracción ni bloqueo de anuncios.
- Si no se define la clave, la sección muestra «Configuración pendiente» y no
  realiza ninguna petición.

### Bibliotecas separadas
- **Dispositivo**: solo canciones encontradas al escanear el almacenamiento.
- **Descargas**: solo archivos descargados desde VibeMusic, con fecha de
  descarga.
- **Importados**: copias privadas creadas al importar archivos.
- **Álbumes**, **Listas** y **Favoritos**.
- Eliminar una descarga de la app no borra archivos ajenos a ella.

### CSV
- Exportación de una lista a CSV con título, artista, álbum, duración e ISRC.
- Importación con detección automática de delimitador (coma, punto y coma o
  tabulador) y de columnas.
- Reasignación manual de columnas, vista previa, emparejamiento real con la
  biblioteca y aviso de canciones no reconocidas y duplicadas.
- Compatibilidad **parcial** con exportaciones de TuneMyMusic: depende de las
  columnas que incluya cada archivo.

### Actualizaciones
- Comprobación automática cada 6 horas y comprobación manual desde Ajustes.
- Solo se tienen en cuenta versiones estables publicadas en
  `DMorenoH0/VibeMusic`.
- Aviso con versión instalada, versión nueva y novedades.
- La descarga la inicia el usuario y la instalación se confirma en el diálogo
  oficial de Android; nunca se instala en segundo plano.
- Verificación SHA-256 cuando el release publica el hash.
- El aviso puede descartarse y no vuelve a mostrarse para esa versión.

### Temas
- Dark AMOLED, Midnight, Ocean, Purple, Crimson, Emerald y Light.
- Vista previa, aplicación inmediata y persistencia.
- Solo cambian los colores de la interfaz; las portadas no se alteran.

### Privacidad y cuenta
- Documentos legales dentro de la app: privacidad, términos y cookies.
- «Eliminar mis datos» distingue datos locales, archivos musicales, cuenta de
  Google y servicios externos.
- No hay publicidad, analítica ni rastreadores.
- `allowBackup="false"`: los datos locales no se copian a la nube.
- Inicio de sesión con Google **opcional** mediante Credential Manager. Solo se
  guarda el perfil (identificador, nombre y correo); el token no se almacena.
  Sin un ID de cliente OAuth configurado, la app lo indica y no envía datos.

## Requisitos

- Android 7.0 (API 24) o superior.
- JDK 21.
- Android SDK con compileSdk 35 y build-tools 36.0.0.
- Gradle 8.11.1 (wrapper incluido).

## Compilación

```bash
./gradlew assembleDebug
```

APK de depuración:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Firma de release

La compilación de release usa un keystore solo si se define
`VIBE_KEYSTORE_PATH` junto con `VIBE_KEYSTORE_PASSWORD`, `VIBE_KEY_ALIAS` y
`VIBE_KEY_PASSWORD`. El workflow de GitHub Actions reconstruye el keystore desde
los secretos `VIBE_KEYSTORE_BASE64`, `VIBE_KEYSTORE_PASSWORD`, `VIBE_KEY_ALIAS`
y `VIBE_KEY_PASSWORD`. Sin esos secretos se publica un APK debug.

### Google Sign-In

Define `VIBE_GOOGLE_WEB_CLIENT_ID` (propiedad de Gradle o variable de entorno)
con el ID de cliente OAuth de tipo «Web». Sin ese valor la sección de cuenta
muestra «Configuración pendiente» y no contacta con Google.

### YouTube Data API v3

La clave nunca se guarda en el repositorio. El script de Gradle la lee, por
orden de prioridad, de:

1. `-PvibeYouTubeApiKey=…` (propiedad de Gradle),
2. la variable de entorno `VIBE_YOUTUBE_API_KEY`,
3. `VIBE_YOUTUBE_API_KEY` en `local.properties` (ignorado por Git).

En GitHub Actions se define como secreto del repositorio
(`Settings → Secrets and variables → Actions → VIBE_YOUTUBE_API_KEY`). Si el
secreto no existe, la app compila igual y la pestaña YouTube muestra
«Configuración pendiente» sin enviar ninguna petición.

Pasos para crear la clave:

1. En Google Cloud Console crea un proyecto y habilita **YouTube Data API v3**.
2. Crea una clave de API en «Credenciales».
3. Restríngela a la app Android con el nombre de paquete `com.vibemusic.app` y
   la huella SHA-1 del certificado de release.
4. Restringe las API permitidas a YouTube Data API v3.
5. No la subas al repositorio ni la pegues en issues o capturas.

Cuota: la API gratuita incluye 10.000 unidades al día. Cada búsqueda nueva usa
`search.list` (100 unidades) y `videos.list` (1 unidad). Por eso la app solo
busca cuando el usuario lo pide y cachea los resultados en memoria. La clave
viaja en el APK porque la API se consume desde el cliente; su protección real
son las restricciones de paquete y firma en Google Cloud.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Cubren el parser/exportador CSV, el comparador de versiones y el parser de
releases de GitHub, los parsers de Deezer, el parser del perfil de Google y el
cliente de YouTube (URLs, identificadores, combinación de `search.list` +
`videos.list`, duraciones ISO-8601 y errores de cuota o clave).

En el emulador API 37 se han probado además la barra inferior de cuatro
apartados (a 411 dp y a 360 dp de ancho), Samples, el buscador online de Deezer,
la sección YouTube (estado sin clave y error real de clave no válida), la
reproducción local completa con notificación y cola, el reproductor, ajustes, el
diálogo de borrado de datos y la actualización 1.1.0 → 1.2.0 sin desinstalar.

En 1.2.1 se verificó en el mismo emulador el reproductor embebido de YouTube con
un vídeo de prueba: carga de la IFrame API dentro del WebView, vídeo visible en
16:9, audio sin silenciar, controles oficiales, pausa y reanudación, pausa al
pasar la app a segundo plano, regreso a los resultados con el WebView destruido
y ausencia de apps externas en primer plano. También se comprobó el mensaje de
error con un identificador de vídeo no disponible.

La búsqueda de YouTube con resultados reales y la pulsación de un resultado real
no se han podido ejecutar porque no hay una clave válida de YouTube Data API en
este entorno; la app no incluye ninguna clave. El resto del flujo (apertura de
la pantalla interna, carga del vídeo por ID, reproducción, pausa, errores y
vuelta a los resultados) sí se ha ejecutado.

## Arquitectura

- `data/`: Room, MediaStore, descargas, ajustes, CSV, Deezer, GitHub, Google y
  YouTube Data API v3 (`data/youtube`).
- `playback/`: servicio Media3, controlador y sesión multimedia.
- `ui/`: pantallas Compose (incluidas `SamplesScreen`, `YouTubeScreen` y
  `YouTubePlayerScreen` con su WebView), componentes, tema y diálogos.
- `assets/youtube_player.html`: plantilla auditable del reproductor oficial
  (IFrame Player API, `playsinline`, sin extracción de audio).
- `viewmodel/`: estado de la interfaz y acciones del usuario.
- `.github/workflows/release-apk.yml`: tests, firma opcional, SHA-256 y release.

## Estado de verificación

La versión 1.0.x se instaló en un vivo v40 con Android 16 y sus funciones
básicas funcionaban. Las versiones 1.1.0 y 1.2.0 se han probado en un emulador
API 37:

- 1.1.0: buscador online, álbum y artista, vistas previas, CSV, temas,
  actualización manual, borrado de datos y reproducción local con notificación
  y cola.
- 1.2.0: barra inferior de cuatro apartados en 411 dp y 360 dp, Samples,
  reproducción local completa, notificación multimedia, cola, reproductor,
  buscador de Deezer, sección YouTube sin clave, petición real a la API con una
  clave inválida, apertura de `vnd.youtube:` y del enlace `https`, ajustes,
  documentos legales y diálogo de borrado de datos.
- 1.2.1: reproductor de YouTube embebido (WebView + IFrame API), vídeo visible
  16:9, audio sin silenciar, controles oficiales, pausa/reanudación, pausa al
  pasar a segundo plano, mensaje de error con vídeo no disponible, vuelta a los
  resultados y destrucción del WebView. La reproducción local (Media3) se pausa
  al abrir el reproductor de YouTube y no se ve afectada.
- Actualización: se instaló el APK publicado de 1.1.0 y después el de 1.2.0
  firmado con el mismo certificado, sin desinstalar y conservando los datos.
  El APK que publica CI tiene el mismo keystore estable.

La búsqueda de YouTube con resultados reales no se ha podido verificar porque
no hay una clave válida en este entorno. La app no incluye ninguna clave.

## Limitaciones

- La búsqueda de YouTube necesita una clave válida de YouTube Data API v3
  configurada como `VIBE_YOUTUBE_API_KEY`. Sin ella, la sección muestra
  «Configuración pendiente» y no envía peticiones; el resto de la app funciona
  igual. La cuota gratuita es de 10.000 unidades/día y cada búsqueda consume
  unas 101.
- Los vídeos de YouTube se reproducen con el reproductor oficial embebido, con
  sus anuncios y restricciones. No hay extracción de audio, descarga, MP3 ni
  reproducción en segundo plano de YouTube.
- El reproductor embebido depende de que YouTube permita la inserción y de la
  versión de Android System WebView del dispositivo. Si YouTube cambia sus
  requisitos de origen, la app muestra el error correspondiente en lugar de
  abrir una app externa.
- El buscador online reproduce únicamente vistas previas de 30 segundos; no hay
  reproducción completa ni descarga de canciones de Deezer.
- Las funciones nuevas se han probado en emulador API 37; no se han probado en
  un teléfono físico (incluido el Vivo V40 SE).
- Las descargas solo aceptan URLs HTTPS directas; no hay scraping ni extracción
  de audio de YouTube.
- No se ofrece un catálogo comercial ilimitado.
- El inicio de sesión con Google requiere configurar un ID de cliente OAuth.
- La firma de la versión 1.0.2 se generó con un keystore debug efímero del
  runner de CI y su clave privada no existe en el repositorio. Por eso las
  versiones 1.1.0 y 1.2.0 usan una firma estable distinta y requieren
  desinstalar una vez la 1.0.2 antes de instalar la nueva. Desde 1.1.0 las
  actualizaciones conservan la misma firma.
