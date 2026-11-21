# VibeMusic

Reproductor de música para Android con interfaz Compose y biblioteca local que
integra **Audius** como fuente principal de **canciones completas** mediante su
API pública oficial. Conserva un buscador secundario con **vistas previas
oficiales de 30 segundos** de Deezer y una sección **YouTube** que busca vídeos
con YouTube Data API v3 y los reproduce con el reproductor oficial embebido
(YouTube IFrame Player API en un WebView). No extrae audio de YouTube, Spotify,
Deezer ni otras plataformas, no elude condiciones de acceso y solo descarga
audio cuando el proveedor autoriza esa pista.

La versión **3.0.0** convierte el catálogo en una experiencia unificada: una sola
búsqueda devuelve canciones completas, artistas, álbumes y listas de Audius, con
pantallas de detalle propias y reproducción real; las vistas previas de Deezer
quedan como fuente secundaria etiquetada. Añade descargas de Audius con un solo
toque **solo si la API declara la pista descargable y sin condiciones
pendientes**, con portada, metadatos, progreso, velocidad, tamaño, ETA,
cancelación y reanudación, o un motivo claro cuando no está permitida. Mantiene
las cinco pestañas, las recomendaciones locales (**Para ti**), las descargas
reanudables, los perfiles de audio, el ecualizador con 12 presets y las listas de
2.x. La base de datos migra de 2 a 3 sin borrar favoritos, listas ni historial.

## Funciones

### Navegación
- Barra inferior fija de cinco apartados: **Inicio**, **Explorar**, **Buscar**,
  **Biblioteca** y **Para ti**.
- Mini reproductor persistente sobre la barra; el reproductor completo se abre
  desde cualquier pestaña con la portada, la cola y los controles.
- Estilo AMOLED oscuro con tarjetas redondeadas, degradados, portadas grandes y
  animaciones breves; temas Midnight, Ocean, Purple, Crimson, Emerald y Light.
- Las pantallas de detalle (reproductor, cola, ajustes, búsqueda, YouTube,
  descargas, Bluetooth, listas y álbumes) se abren encima y se cierran con atrás
  o con su botón de volver.
- Diseño adaptado a 1080×2400 y probado en emulador API 37 (Android 16).

### Reproducción y biblioteca
- Reproducción en segundo plano con Media3 ExoPlayer y MediaSession.
- Notificación multimedia y controles en pantalla de bloqueo.
- Reproducción, pausa, anterior, siguiente, aleatorio, repetir y velocidad.
- Cola de reproducción con salto, eliminación y vaciado.
- Escaneo real de audio local con MediaStore y permisos de Android 13+.
- Importación mediante el selector de archivos (SAF).
- Descargas de enlaces directos HTTPS (MP3, M4A, OGG, FLAC, WAV).
- Descarga con un solo toque desde el menú de una canción cuando la fuente está
  autorizada, con portada, metadatos y duración originales.
- Indicadores de descarga completada, en curso y fallida en las filas.
- Persistencia con Room: favoritos, listas, descargas e historial.
- Temporizador de sueño.

### Buscador unificado
- Una sola búsqueda agrega **Audius** (fuente principal) y **Deezer** (vistas
  previas), con secciones separadas y etiquetadas por origen.
- Audius devuelve canciones completas, artistas, álbumes y listas reales de su
  API v1 (`api.audius.co/v1`), sin clave para lecturas públicas.
- Pantallas de detalle de artista, álbum y lista de Audius con portada, biografía,
  seguidores, pistas, álbumes/listas relacionadas y reproducción completa.
- Búsqueda por título, artista y álbum en la biblioteca del dispositivo.
- Sugerencias e historial de búsquedas borrable.
- Acciones por resultado: reproducir, reproducir a continuación, añadir al final
  de la cola, ver álbum, ver artista, favoritos, listas y descarga autorizada.
- Favoritos, listas e historial aceptan pistas de Audius; las vistas previas de
  Deezer siguen sin persistirse.
- Estados de carga, vacío, error y reintento con textos en español.
- Caché en memoria con TTL para búsquedas, tendencias y detalles; los errores
  401/403/404/429/5xx se traducen a mensajes claros.

### Samples
- Pestaña **Samples** con las seis instrumentales originales incluidas en la
  app, filtros por género y «Reproducir todo».
- Se instalan como archivos locales: reproducción completa, sin conexión y en
  segundo plano, con la misma cola y notificación que el resto de la música.

### YouTube
- Sección **YouTube** accesible desde **Explorar**; no ocupa una pestaña fija.
- Búsqueda por título, artista, remezcla, versión extendida, directo, cover o
  videoclip con YouTube Data API v3.
- Resultados con miniatura, título, canal, duración real (ISO-8601 de
  `videos.list`) y fecha de publicación.
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
  red), resultados vacíos y «Conecta YouTube».
- La clave se puede pegar en la propia sección YouTube (se comprueba contra
  Google, se cifra con Android Keystore y se puede borrar), compilar con
  `VIBE_YOUTUBE_API_KEY` o delegar en un backend con `VIBE_YOUTUBE_PROXY_URL`.
- **No** se implementa extracción de audio, conversión a MP3, descarga de
  vídeos, reproducción en segundo plano por extracción ni bloqueo de anuncios.
- Sin clave guardada, clave compilada ni proxy, la sección muestra «Conecta
  YouTube» y no realiza ninguna petición.
- Cada resultado se puede añadir a una lista existente; la referencia se guarda
  como `youtube:<videoId>` con su miniatura y se abre en el reproductor oficial
  desde la lista, con vuelta a la lista de origen.

### Para ti (recomendaciones locales)
- Mezcla diaria, descubrimientos, artistas similares, favoritos, lo más
  escuchado, contenidos de tus listas y nuevos sonidos.
- Se calcula **solo en el dispositivo** con historial de escucha, favoritos,
  listas, saltos y preferencias. No hay peticiones de red ni analítica.
- La mezcla diaria es estable durante el día y cambia al día siguiente; las
  canciones saltadas recientemente se excluyen.
- El historial se puede borrar desde Ajustes → Privacidad sin tocar favoritos ni
  listas. Las recomendaciones se pueden desactivar por completo.

### Descargas autorizadas
- **Descargar canción** con un solo toque desde el menú de la canción. VibeMusic
  resuelve la fuente autorizada y toma título, artista, álbum, portada y
  duración sin pedir URL ni metadatos.
- Fuentes admitidas: las seis instrumentales propias incluidas en la app y las
  pistas de Audius (o de otro proveedor) que expongan un `downloadUrl` HTTPS con
  derechos de descarga. No hay scraping ni extracción de audio de YouTube,
  Spotify ni Deezer.
- En Audius se consultan los campos reales `is_downloadable`,
  `is_download_gated`, `access.download` y `download_conditions`. Solo se ofrece
  la descarga cuando el artista la autoriza; si requiere compra en USDC, seguir
  al artista, un NFT, un token u otra condición, se muestra «Descarga no
  disponible» con el motivo y no se intenta omitir.
- Las vistas previas de Deezer (licencia de reproducción), las referencias de
  YouTube y los archivos locales ya disponibles se muestran como **no
  disponibles para descarga offline**, con el motivo visible.
- El formulario manual sigue existiendo como opción avanzada «Añadir enlace
  autorizado» (MP3, M4A, AAC, OGG/Opus, WAV, FLAC) para enlaces HTTPS directos
  que el usuario tenga derecho a usar, con **calidad** (alta, media, baja; el
  archivo directo no se recodifica) y **conexión** (solo Wi-Fi o Wi-Fi y datos)
  con aviso de consumo.
- Progreso con portada, nombre, porcentaje, bytes, velocidad, tiempo restante y
  botón Cancelar; notificación del sistema mientras la app está en segundo plano.
- Servicio en primer plano con la parte descargada en caché; si la red se corta,
  se conserva y el siguiente intento usa `Range` cuando el servidor lo admite.
- Guardado en el almacenamiento musical (`Music/VibeMusic`) con el nombre
  original saneado, extensión y metadatos; en Android 10+ se publica en
  MediaStore. La portada se conserva como imagen real en el almacenamiento
  privado de la app.
- La biblioteca de descargas muestra portada, título, artista, duración, fecha,
  tamaño y permite reproducir, añadir a una lista o eliminar el archivo
  descargado (solo el que creó VibeMusic). Todo funciona sin Internet.
- Los duplicados se evitan por clave de fuente y por título/artista, incluso
  después de reiniciar.

### Bluetooth y ecualizador
- Detección de la salida de audio actual con las APIs públicas de Android
  (`AudioManager`/`AudioDeviceCallback`): tipo, nombre, salida del teléfono,
  auriculares o altavoz Bluetooth.
- Fabricante mostrado solo como pista inferida del nombre; Android no expone el
  modelo real y el usuario puede escribirlo a mano y guardarlo por dispositivo.
- 12 presets: Bass Boost, Deep Bass, Reggaetón, Dembow, Hip-Hop, Pop, Rock,
  Electrónica, Vocal, Balanced, Acoustic y Custom.
- Ecualizador gráfico de cinco bandas (subgraves, graves, medios, medios-altos,
  agudos) sobre la sesión global de audio, con guardar, restablecer y
  autoaplicación del perfil al conectar un dispositivo.
- Se explica en la propia pantalla cuando el fabricante no expone efectos: la UI
  no simula que el ecualizador funciona. El **códec** Bluetooth (SBC, AAC,
  aptX, LDAC…) solo está disponible para apps del sistema con permisos
  privilegiados, así que no se inventa ni se muestra un valor falso.

### Menú contextual de canción
- Disponible en búsqueda, YouTube, biblioteca local, descargas, listas,
  Canciones que te gustan y recomendaciones de «Para ti».
- **Añadir a lista**: muestra las listas existentes, permite crear una nueva y
  evita duplicados.
- **Quitar de esta lista**: solo aparece al abrir una canción desde una lista y
  no borra el archivo musical.
- **Eliminar de Canciones que te gustan**: quita el favorito sin tocar la
  biblioteca ni el almacenamiento.
- **Añadir a la cola**: agrega la canción al final sin interrumpir la
  reproducción actual.
- **Reproducir a continuación**, ver álbum, descargar o eliminar la descarga
  según el contexto y la fuente.
- Hoja inferior oscura y compacta, con portada, iconos coherentes y el estado de
  descarga de la canción.

### Listas
- Crear, renombrar, cambiar portada desde una imagen del dispositivo y eliminar.
- La portada elegida se copia al almacenamiento privado de la app, así que sigue
  visible tras reiniciar aunque el archivo original se mueva o se borre.
- Añadir canciones locales, descargas, muestras y referencias de YouTube sin
  duplicar archivos.
- Reordenar con subir/bajar, reproducir todo, aleatorio y repetir.
- Persisten en Room y sobreviven a reinicios y actualizaciones.

### Bibliotecas separadas
- **Dispositivo**: solo canciones encontradas al escanear el almacenamiento.
- **Descargas**: solo archivos descargados desde VibeMusic, con fecha, tamaño y
  portada cuando existe.
- **Importados**: copias privadas creadas al importar archivos.
- **Álbumes**, **Listas** y **Favoritos**.
- Eliminar una descarga de la app borra el archivo que creó VibeMusic y su fila
  de MediaStore; no toca archivos ajenos a la app.

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

La app admite tres formas de configurar la búsqueda, de la más segura a la más
sencilla:

1. **Servicio intermediario (recomendado)**: define `VIBE_YOUTUBE_PROXY_URL` al
   compilar (propiedad `vibeYouTubeProxyUrl`, variable de entorno o
   `local.properties`). El APK no contiene ninguna clave y las búsquedas se
   envían a tu backend. El endpoint recibe `?q=consulta` y debe responder
   `{"items":[{"id":"…","title":"…","channel":"…","thumbnail":"https://…",
   "durationMs":213000,"publishedAt":"…","description":"…"}]}`. La app acepta
   también `duration` en formato ISO-8601 y descarta miniaturas que no sean
   HTTPS.
2. **Clave introducida en el dispositivo**: desde la sección YouTube (icono de
   llave) se pega la clave. La app la comprueba con Google mediante `videos.list`
   (1 unidad de cuota) antes de guardarla cifrada con Android Keystore
   (AES-256/GCM). La clave guardada tiene prioridad sobre la clave compilada y
   se puede eliminar desde la misma pantalla.
3. **Clave compilada**: el script de Gradle la lee, por orden de prioridad, de
   `-PvibeYouTubeApiKey=…`, la variable de entorno `VIBE_YOUTUBE_API_KEY` o
   `VIBE_YOUTUBE_API_KEY` en `local.properties` (ignorado por Git). En GitHub
   Actions se define como secreto del repositorio
   (`Settings → Secrets and variables → Actions → VIBE_YOUTUBE_API_KEY`).

Si no hay clave guardada, clave compilada ni proxy, la sección YouTube muestra
«Conecta YouTube» y no envía consultas. La clave nunca se escribe en logs y las
peticiones directas envían las cabeceras `X-Android-Package` y `X-Android-Cert`
(SHA-1 de la firma) para que Google pueda restringir la clave al paquete
`com.vibemusic.app` y al certificado de release.

Pasos para crear la clave:

1. En Google Cloud Console crea un proyecto y habilita **YouTube Data API v3**.
2. Crea una clave de API en «Credenciales».
3. Restríngela a la app Android con el nombre de paquete `com.vibemusic.app` y
   la huella SHA-1 del certificado de release.
4. Restringe las API permitidas a YouTube Data API v3.
5. No la subas al repositorio ni la pegues en issues o capturas.

Cuota: la API gratuita incluye 10.000 unidades al día. Cada búsqueda nueva usa
`search.list` (100 unidades) y `videos.list` (1 unidad). Por eso la app solo
busca cuando el usuario lo pide y cachea los resultados en memoria. Con una
clave en el APK, su protección real son las restricciones de paquete y firma en
Google Cloud; el proxy evita esa exposición por completo.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Cubren el parser/exportador CSV, el comparador de versiones y el parser de
releases de GitHub, los parsers de Deezer, el parser del perfil de Google y el
cliente de YouTube (URLs, identificadores, combinación de `search.list` +
`videos.list`, duraciones ISO-8601, errores de cuota o clave, modo proxy,
validación de credenciales y formato de la clave). Desde 2.0.0 se añaden 13
pruebas: 9 del motor de recomendaciones (estabilidad diaria, exclusión de
saltadas, orden por reproducciones, favoritos, listas, descubrimientos, artistas
y géneros, fuerza de la señal) y 4 de los presets del ecualizador (12 nombres
únicos, rangos válidos, Balanced plano y búsqueda tolerante a mayúsculas).
Desde 2.1.0 se añaden 9 pruebas del resolutor de descargas (muestra propia,
duplicados por clave e identidad, Deezer, YouTube, archivo local, URL autorizada
y claves estables).
Desde 3.0.0 se añaden 12 pruebas de Audius (pista real, artista, álbum, lista,
portada, metadatos, pista no descargable, compra en USDC, seguir al artista,
acceso de descarga desactivado, stream con puerta y campos ausentes) y 4 pruebas
del resolutor para descargas de Audius (URL autorizada, motivo visible, rechazo
sin archivo y clave estable).
En total son **81 pruebas unitarias** en 11 suites, todas correctas.

En el emulador API 37 se han probado además la barra inferior de cuatro
apartados (a 411 dp y a 360 dp de ancho), Samples, el buscador online de Deezer,
la reproducción local completa con notificación y cola, el reproductor, ajustes,
el diálogo de borrado de datos y la actualización 1.1.0 → 1.2.0 sin desinstalar.

En 1.2.1 se verificó en el mismo emulador el reproductor embebido de YouTube con
un vídeo de prueba: carga de la IFrame API dentro del WebView, vídeo visible en
16:9, audio sin silenciar, controles oficiales, pausa y reanudación, pausa al
pasar la app a segundo plano, regreso a los resultados con el WebView destruido
y ausencia de apps externas en primer plano. También se comprobó el mensaje de
error con un identificador de vídeo no disponible.

En 1.2.2 se comprobaron en el emulador el formulario y el diálogo de clave, el
error de formato, el error real de Google con una clave con formato válido pero
inexistente, el error sin conexión, la reproducción local con pausa y
reanudación, y la actualización 1.2.1 → 1.2.2 sin desinstalar. La búsqueda con
resultados reales sigue pendiente de una clave válida: no hay acceso a Google
Cloud en este entorno y la app no incluye ninguna clave.

En 2.0.0 se verificaron en el emulador API 37: las cinco pestañas y la
navegación a Ajustes → Bluetooth y ecualizador; la pantalla de ecualizador con
la salida del teléfono, los 12 presets, la aplicación de Bass Boost (5/4/0/-1/0
dB), el mensaje de ajuste aplicado y la explicación honesta del códec; la
descarga completa de un MP3 directo de SoundHelix con nombre original
(`SoundHelix-Song-3.mp3`), metadatos, duración, tamaño y reproducción local; el
diálogo de progreso real (79 %, 6,4 MB de 8,1 MB, 6,4 MB/s, «Quedan 1 s»);
la cancelación con conservación del archivo parcial (5,2 MB) y la
reanudación en un segundo intento; la biblioteca con fecha y tamaño; la
eliminación segura (desaparece de MediaStore y del almacenamiento) y la
persistencia tras reiniciar; la creación de una lista, el renombrado, la
portada (copiada a almacenamiento privado y visible tras reiniciar), la
reordenación, la reproducción y la eliminación de una canción; y la
migración 1→2 de la base de datos conservando las canciones de la versión
anterior.

En 2.1.0 se verificó en el emulador API 37: la descarga de una muestra con un
solo toque desde el menú, el progreso y el aviso de finalización, la entrada en
Descargas con portada, título, artista, duración y tamaño, el archivo real en
`Music/VibeMusic` con su fila de MediaStore, la portada PNG generada, la
reproducción con modo avión, la persistencia tras reiniciar el emulador, la
adición al final de la cola sin interrumpir la reproducción (7→8 y «Siguiente»
mantiene el orden), el favorito con su etiqueta «Eliminar de Canciones que te
gustan», la creación y adición a una lista, el aviso de duplicado «Ya está en esa
lista», «Quitar de esta lista» sin borrar el archivo, el menú contextual en
búsqueda, resultados online, Descargas, lista y «Para ti», y la eliminación de la
descarga con limpieza de archivo, MediaStore, portada y entrada, dejando intactos
archivos ajenos en `Music/`. El menú de YouTube no se pudo abrir con datos reales
porque no hay clave de API en este entorno.

La actualización 2.0.0 → 2.1.0 se instaló sin desinstalar con el APK firmado del
release: `firstInstallTime` no cambió, el favorito y la lista creados en 2.0.0 se
conservaron, y la descarga de un toque funcionó en el APK publicado.

## Arquitectura

- `data/`: Room (v3 con migración desde 1.x y 2.x), MediaStore, descargas,
  ajustes, CSV, Deezer, GitHub, Google y YouTube Data API v3 (`data/youtube`).
- `data/audius/`: modelos, parsers puros y cliente de la API oficial de Audius
  con caché TTL, reintentos y traducción de errores.
- `data/provider/`: contrato `MusicProvider`, registro y `AudiusProvider`. Un
  futuro proveedor (por ejemplo Tuned Global) solo implementa la interfaz; no se
  simula ninguna conexión comercial ni se incluyen credenciales.
- `data/recommend/`: motor local y determinista de recomendaciones, sin red.
- `audio/`: detección de salida Bluetooth, perfiles por dispositivo y
  ecualizador global (`EqualizerController`, `EqPresets`).
- `data/download/`: bus de progreso, servicio en primer plano y modelo de
  solicitud persistente.
- `playback/`: servicio Media3, controlador y sesión multimedia.
- `ui/`: pantallas Compose (incluidas `ForYouScreen`, `DownloadsScreen`,
  `BluetoothScreen`, `PlaylistDetailScreen`, `SamplesScreen`, `YouTubeScreen` y
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
- 1.2.2: configuración de la clave de YouTube dentro de la app (icono de llave y
  tarjeta «Conecta YouTube»), comprobación real contra Google antes de guardar,
  cifrado con Android Keystore, error de formato, error real de Google con clave
  inexistente, error sin conexión, reproducción local con pausa y reanudación.
- 2.0.0: cinco pestañas y mini reproductor; «Para ti» con mezcla diaria y
  descubrimientos; ajustes por categorías con descargas, Bluetooth y privacidad;
  migración de base de datos 1→2; descarga real de un MP3 directo con progreso,
  cancelación, reanudación y biblioteca; eliminación segura; ecualizador global
  con presets; creación de listas.
- 2.1.0: descarga con un solo toque de una muestra propia, portada y metadatos,
  reproducción sin conexión, persistencia tras reiniciar, cola al final,
  favoritos, listas con duplicados y «Quitar de esta lista», menú contextual en
  búsqueda, online, Descargas, lista y «Para ti», y eliminación segura de la
  descarga sin tocar archivos ajenos.
- 3.0.0: búsqueda unificada real con Audius (pistas completas, artistas, álbumes
  y listas) y Deezer (vistas previas), reproducción completa de una pista de
  Audius, pantallas de artista y álbum con biografía, seguidores, pistas y
  relacionadas, favorito y lista con una pista de Audius, descarga autorizada de
  «Relevant Future» (79,2 MB), «Liquor & Cigarettes» (26,2 MB) y «UNO DOS»
  (45,4 MB) con portada y metadatos originales, rechazo de «The Search» por
  compra en USDC, rechazo de una pista no descargable, cancelación de «UNO DOS»
  con 4,1 MB parciales, reanudación hasta completar, reproducción sin conexión
  tras reiniciar la app y del emulador, y persistencia de favoritos, listas y
  descargas. También se comprobó que una vista previa de Deezer con URL válida
  sigue sonando y que una vista previa sin URL ya no llega al reproductor.
- Actualización: se instaló el APK publicado de 1.1.0 y después el de 1.2.0
  firmado con el mismo certificado, sin desinstalar y conservando los datos.
  El APK que publica CI tiene el mismo keystore estable. En 1.2.2 se comprobó
  además la actualización 1.2.1 → 1.2.2 conservando `firstInstallTime` y datos.

La búsqueda de YouTube con resultados reales no se ha podido verificar porque
no hay una clave válida en este entorno. La app no incluye ninguna clave; con
una clave válida pegada en la sección YouTube, el mismo flujo ya probado
(`search.list` + `videos.list`, apertura del vídeo y reproducción embebida)
queda operativo sin recompilar.

## Limitaciones

- La búsqueda de YouTube necesita una clave válida de YouTube Data API v3, un
  proxy configurado o una clave pegada en la sección YouTube. Sin ninguna de
  ellas, la sección muestra «Conecta YouTube» y no envía peticiones; el resto de
  la app funciona igual. La cuota gratuita es de 10.000 unidades/día y cada
  búsqueda consume unas 101.
- La clave guardada en el dispositivo se cifra con Android Keystore y no se
  incluye en copias de seguridad, pero un dispositivo con root podría llegar a
  extraerla. Para producción se recomienda el servicio intermediario
  `VIBE_YOUTUBE_PROXY_URL`, que no expone ninguna clave en el APK.
- Los vídeos de YouTube se reproducen con el reproductor oficial embebido, con
  sus anuncios y restricciones. No hay extracción de audio, descarga, MP3 ni
  reproducción en segundo plano de YouTube.
- El reproductor embebido depende de que YouTube permita la inserción y de la
  versión de Android System WebView del dispositivo. Si YouTube cambia sus
  requisitos de origen, la app muestra el error correspondiente en lugar de
  abrir una app externa.
- Las canciones de Deezer siguen siendo vistas previas de 30 segundos; no hay
  reproducción completa ni descarga de Deezer. Las canciones completas y sus
  descargas pertenecen a Audius y solo se ofrecen cuando su API las autoriza.
- Audius requiere conexión para buscar y reproducir en streaming. Sin conexión
  solo se pueden reproducir las pistas descargadas previamente.
- Las pistas con compra en USDC, seguimiento al artista, NFT, token u otra
  condición de descarga se muestran como no disponibles; VibeMusic no procesa
  pagos, no crea cuentas de Audius y no omite esas condiciones.
- El endpoint oficial de «relacionadas» de Audius respondió 404 durante la
  verificación; las recomendaciones se construyen con pistas del mismo artista,
  género y tendencias usando solo endpoints oficiales.
- Tuned Global no está conectado. La interfaz `MusicProvider` está preparada
  para integrarlo cuando exista un acuerdo y credenciales, sin simular el
  servicio ni incluir claves.
- El ecualizador usa la sesión global de audio. Funciona en el emulador API 37
  con 5 bandas reales, pero algunos fabricantes no exponen efectos globales; en
  ese caso la app lo indica y no simula que los sliders tengan efecto. No hay
  mediciones acústicas por modelo: los presets son puntos de partida.
- El códec Bluetooth negociado (SBC, AAC, aptX, LDAC…) no se muestra porque
  Android solo lo expone a apps del sistema con permisos privilegiados. La app
  evita inventarlo y lo explica en la pantalla.
- El modelo Bluetooth no existe como API pública: se muestra el nombre, el
  fabricante solo como pista y el usuario puede guardar el modelo manualmente.
- Las funciones nuevas se han probado en emulador API 37; no se han probado en
  un teléfono físico (incluido el Vivo V40 SE) ni con auriculares o altavoces
  Bluetooth reales.
- Las descargas de Audius usan únicamente su endpoint oficial de descarga y las
  condiciones declaradas por la API. El formulario manual solo acepta URLs HTTPS
  directas que el usuario tenga derecho a usar. No hay scraping ni extracción de
  audio de YouTube, Spotify ni Deezer. La reanudación depende de que el servidor
  acepte `Range`.
- El catálogo depende de la disponibilidad y latencia de Audius y Deezer; no se
  ofrece un catálogo comercial ilimitado ni se garantiza que una pista siga
  publicada.
- El inicio de sesión con Google requiere configurar un ID de cliente OAuth.
- La firma de la versión 1.0.2 se generó con un keystore debug efímero del
  runner de CI y su clave privada no existe en el repositorio. Por eso las
  versiones 1.1.0 y 1.2.0 usan una firma estable distinta y requieren
  desinstalar una vez la 1.0.2 antes de instalar la nueva. Desde 1.1.0 las
  actualizaciones conservan la misma firma.
