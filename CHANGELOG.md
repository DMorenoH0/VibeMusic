# Changelog

Todas las versiones publicadas de VibeMusic. El formato sigue la idea de
«Keep a Changelog» y las versiones usan SemVer.

## [3.1.0] - 27 de noviembre de 2026

### Añadido

- **Importador de playlists de Spotify y YouTube Music** en
  Explorar → Descargas → «Importar playlist». Se pega el enlace público, se
  detecta la plataforma y se analizan todas las canciones con progreso y
  cancelación.
- **Spotify con su API oficial**: flujo *client credentials* con el Client ID y
  el Client Secret de la app del propio usuario, guardados cifrados con Android
  Keystore. Paginación completa, artistas primarios y secundarios, álbum,
  duración, ISRC y posición original. Los archivos locales y los episodios se
  descartan sin inventar canciones.
- **YouTube Music con YouTube Data API v3**: cabecera de la playlist y todas las
  páginas de `playlistItems`, con duraciones reales por lotes de `videos.list`.
  Solo se leen metadatos; nunca se descarga ni se extrae audio de YouTube.
- **Motor de coincidencias Audius** que puntúa título, artista, versión
  (remix, slowed, sped up, instrumental, directo, cover, extendido, acústico,
  remaster, radio edit, demo), duración, álbum e ISRC. Las versiones
  incompatibles se rechazan aunque el título sea idéntico, y dos resultados muy
  parecidos se marcan como dudosos para que el usuario elija.
- **Estados por canción**: Disponible, Descargable, Local, Coincidencia dudosa,
  No encontrada, No disponible y Error de consulta, con selección individual,
  «seleccionar todo», confirmación de candidatas y añadido parcial.
- **Biblioteca con orden original**: la playlist se crea en una sola transacción
  con la posición original, los metadatos de Spotify/YouTube y la referencia al
  audio completo de Audius. Se evitan duplicados por pista y por versión.
- **Room v4** con migración aditiva 3 → 4: columnas de origen en `playlists` y
  de metadatos de importación en `playlist_tracks`. Favoritos, listas,
  historial y descargas existentes se conservan.
- **Credenciales configurables**: sección «Credenciales» en la pantalla de
  importación, con verificación antes de guardar, borrado y almacenamiento
  cifrado. Nunca se incluyen claves en el APK ni en el repositorio.
- **Pruebas**: 143 pruebas unitarias (enlaces, motor de coincidencias,
  reducers, paginación y errores de ambos importadores, análisis parcial y
  duplicados) y 2 pruebas instrumentadas de la migración 3 → 4 ejecutadas en
  emulador API 37.

### Limitaciones conocidas

- Importar una playlist real requiere credenciales propias de Spotify o una
  clave válida de YouTube Data API v3; sin ellas la app lo explica y no simula
  resultados.
- Las coincidencias dependen del catálogo público de Audius: una canción que no
  esté publicada allí aparecerá como «No encontrada».
- Las pruebas de reproducción y descarga siguen limitadas al emulador; no hay
  teléfono físico ni Bluetooth real en este entorno.

## [3.0.0] - 21 de noviembre de 2026

### Añadido

- **Audius como fuente principal de canciones completas** mediante su API
  oficial v1 (`https://api.audius.co/v1`, `app_name=VibeMusic`). Se consultan
  pistas, artistas, álbumes y listas reales; el audio se reproduce desde el
  endpoint oficial de streaming de cada pista.
- **Buscador unificado**: una sola búsqueda muestra canciones completas,
  artistas, álbumes y listas de Audius, además de las vistas previas de Deezer
  como fuente secundaria etiquetada. Cada proveedor falla de forma aislada y
  muestra su propio aviso.
- **Pantallas de artista, álbum y lista de Audius** con portada, biografía,
  seguidores, número de pistas, reproducción, mezcla y recomendaciones
  relacionadas obtenidas de endpoints oficiales.
- **Descarga autorizada de Audius con un solo toque**: se comprueban los campos
  reales `is_downloadable`, `is_download_gated`, `access.download` y
  `download_conditions` antes de ofrecer la descarga. Se descargan la portada
  original, el título, artista, álbum y duración, y el archivo se publica en
  `Music/VibeMusic` y MediaStore con el nombre derivado del título.
- Progreso con porcentaje, tamaño total, velocidad y tiempo restante; botón
  Cancelar que conserva el archivo parcial y reanudación con `Range` cuando el
  servidor la admite.
- **Motivos claros de rechazo** cuando la pista no es descargable: el artista no
  la autorizó, la descarga está desactivada, requiere compra en USDC, seguir al
  artista, un NFT o un token. VibeMusic no procesa pagos ni elude condiciones.
- Arquitectura de proveedores (`MusicProvider`, `ProviderRegistry`,
  `AudiusProvider`) preparada para añadir otro proveedor sin simular una conexión
  comercial ni incluir credenciales.
- Caché en memoria con TTL para búsquedas y detalles de Audius, y traducción de
  errores HTTP 401/403/404/429/5xx y de red a mensajes en español.
- Pistas de Audius en favoritos, listas, historial, cola, reproductor, mini
  reproductor y biblioteca, con insignia de origen y acciones completas.
- Secciones de descubrimiento de Audius en Inicio, Explorar y Para ti.

### Cambiado

- La base de datos pasa a la **versión 3** con una migración aditiva 2→3 que
  conserva favoritos, listas, historial y descargas. No se borra ningún dato.
- La política de privacidad y los términos describen Audius, la Open Music
  License, las descargas autorizadas y la ausencia de envío de historial.
- El menú de canción muestra «Ver álbum» y «Ver artista» de Audius y mantiene
  «Añadir a lista», cola, favoritos y descarga.
- Las vistas previas de Deezer sin URL de audio dejan de mostrarse, y un error
  anterior del reproductor se limpia al iniciar una reproducción nueva.

### Pruebas

- 12 pruebas nuevas del parser y del permiso de descarga de Audius, y 4 nuevas
  del resolutor de descargas para Audius (81 pruebas unitarias en 11 suites).
- Verificación real en emulador API 37: búsqueda unificada, reproducción
  completa, pantallas de artista/álbum, favoritos y listas con pistas de Audius,
  descarga de pistas autorizadas, rechazo de pista con compra en USDC, rechazo
  de pista no descargable, cancelación con archivo parcial, reanudación, archivo
  final con metadatos y portada, reproducción sin conexión y persistencia tras
  reiniciar.

## [2.1.0] - 17 de noviembre de 2026

### Añadido

- **Descarga con un solo toque**: cada canción ofrece «Descargar canción» en su
  menú contextual. VibeMusic identifica la fuente autorizada, toma título,
  artista, álbum, portada y duración, descarga el archivo y lo añade a
  Descargas sin pedir URL ni metadatos.
- Fuente autorizada para las **muestras originales** incluidas en la app: se
  copian con su portada procedural convertida en imagen real, conservan nombre
  y metadatos y se publican en `Music/VibeMusic` y MediaStore.
- Campo opcional `downloadUrl` en las pistas para proveedores que concedan
  derechos de descarga; solo se usa cuando la fuente lo autoriza expresamente.
- **Menú contextual de canción** en resultados de búsqueda, YouTube, biblioteca
  local, descargas, listas, Canciones que te gustan y «Para ti»:
  reproducir, reproducir a continuación, **añadir a la cola**, añadir o quitar
  de Canciones que te gustan, ver álbum, descargar, quitar de la lista actual y
  eliminar el archivo descargado.
- **Indicadores de descarga** en cada fila: completada, en curso con porcentaje
  y fallida, además del estado detallado dentro del menú.
- Avisos claros cuando una canción **no se puede descargar**: vistas previas de
  Deezer (licencia de reproducción), referencias de YouTube (sin autorización
  de descarga) y archivos locales que ya se reproducen sin conexión.

### Cambiado

- El formulario manual de URL, título, artista y calidad deja de ser el flujo
  principal. Pasa a ser una opción avanzada, «Añadir enlace autorizado», dentro
  de Descargas, para enlaces HTTPS directos que el usuario tenga derecho a usar.
- «Añadir a la cola» agrega la canción **al final** de la cola sin interrumpir
  la reproducción; «Reproducir a continuación» mantiene su comportamiento.
- El menú evita duplicados por identidad de canción y por clave de fuente, y
  recuerda las descargas entre reinicios sin cambiar el esquema de la base de
  datos.
- Los textos de Descargas explican el flujo de un toque y mantienen la
  eliminación limitada a los archivos que gestiona VibeMusic.

### Pruebas

- 65 pruebas unitarias en 10 suites (9 nuevas del resolutor de descargas:
  muestras, duplicados, Deezer, YouTube, archivos locales, URL autorizada,
  claves estables e identidad sin distinción de mayúsculas).
- Verificado en emulador API 37 (compilación debug):
  - «Descargar canción» de una muestra con un solo toque: diálogo de progreso,
    aviso de finalización y entrada en Descargas con portada, título, artista
    (Luna Vale), duración 0:40 y tamaño 1,7 MB.
  - Archivo real en `Music/VibeMusic/Afterglow.wav` (1.764.044 bytes) y fila en
    MediaStore con título, álbum y duración; portada PNG real de 220 KB en el
    almacenamiento privado de la app.
  - Reproducción sin conexión con modo avión activo, mostrando la portada
    generada, el título y el artista originales.
  - Persistencia de la descarga y de la portada tras reiniciar el emulador.
  - «Añadir a la cola» con una cola de 7 canciones: la canción se añadió al
    final (8 en cola), la reproducción actual no se interrumpió y «Siguiente»
    siguió con la segunda canción de la cola, no con la añadida.
  - Favorito: «Añadir a Canciones que te gustan», cambio de etiqueta a
    «Eliminar de Canciones que te gustan» y eliminación sin tocar la descarga.
  - Lista: creación desde el menú, adición de la canción, aviso «Ya está en esa
    lista» al repetirla y «Quitar de esta lista» desde el detalle de la lista,
    que la dejó con 0 canciones sin borrar el archivo musical.
  - Menú contextual comprobado en búsqueda local, resultados online de Deezer,
    Descargas, detalle de lista y «Para ti», con el estado de descarga y el
    aviso de fuente no descargable según el contexto.
  - «Eliminar archivo descargado» borró el archivo de `Music/VibeMusic`, su fila
    de MediaStore, la portada y la entrada de Descargas, y dejó intactos dos
    archivos antiguos de `Music/` que no pertenecen a esta descarga. Después se
    pudo volver a descargar la misma canción.
- Actualización 2.0.0 → 2.1.0 con el APK firmado del release, instalado encima
  sin desinstalar: `firstInstallTime` se mantuvo (17 nov 2026, 14:24:23), el
  favorito de Afterglow y la lista «Actualizacion21» creados en 2.0.0 siguieron
  presentes, el menú mostró «Eliminar de Canciones que te gustan» y
  «Descargar canción», y la descarga de un toque volvió a completarse en el
  APK publicado.
- No verificable en emulador: el menú de un vídeo de YouTube, porque la
  búsqueda real necesita una clave válida de YouTube Data API y no hay ninguna
  configurada en este entorno.

## [2.0.0] - 15 de noviembre de 2026

### Añadido

- Barra inferior de **cinco pestañas**: Inicio, Explorar, Buscar, Biblioteca y
  Para ti, con mini reproductor persistente y portadas grandes.
- **Para ti**: mezcla diaria estable durante el día, descubrimientos, artistas
  similares, favoritos, lo más escuchado y novedades de tus listas, calculado
  solo en el dispositivo con historial, saltos y preferencias. Sin red ni
  analítica; se puede desactivar y borrar el historial desde Ajustes.
- **Descargas autorizadas** desde enlaces HTTPS directos: calidad, solo Wi-Fi o
  Wi-Fi y datos, aviso de consumo, progreso con portada, porcentaje, tamaño,
  velocidad y tiempo restante, cancelación con archivo parcial y reanudación
  HTTP `Range`, guardado en `Music/VibeMusic`, publicación en MediaStore y
  reproducción sin conexión.
- **Biblioteca de descargas** con portada, título, artista, duración, fecha y
  tamaño; reproducción, añadir a una lista y eliminación segura del archivo y su
  fila de MediaStore.
- **Perfiles de audio por dispositivo** con tipo de salida, nombre, fabricante
  inferido y modelo escrito a mano; 12 presets de ecualizador (Bass Boost, Deep
  Bass, Reggaetón, Dembow, Hip-Hop, Pop, Rock, Electrónica, Vocal, Balanced,
  Acoustic y Custom), cinco bandas reales sobre la sesión global, guardado,
  restablecimiento y autoaplicación al conectar.
- **Listas ampliadas**: crear, renombrar, portada desde el dispositivo
  (copiada al almacenamiento privado de la app), eliminar, añadir canciones
  locales, descargas, muestras y referencias de YouTube sin duplicados,
  reordenar con subir/bajar, reproducir, aleatorio y repetir.
- **Ajustes** organizados por categorías: reproducción, descargas, Bluetooth y
  ecualizador, apariencia, privacidad, datos, actualizaciones y acerca de.
- Migración de base de datos **1 → 2** que conserva canciones, favoritos,
  historial y listas de versiones anteriores.

### Cambiado

- La base de datos incluye historial de escucha, perfiles de audio y descargas.
- El reproductor muestra el título y el artista reales del archivo descargado
  cuando el usuario no los indica y el archivo no trae metadatos.
- La pantalla de listas muestra portada, número de canciones y duración total.
- El ecualizador conserva el nombre del preset si no se mueven los sliders y
  pasa a «Custom» cuando se ajustan a mano.
- El permiso de Bluetooth se refresca al volver a la app para reflejar
  concesiones hechas fuera de ella.

### Corregido

- La portada de una lista se copia al almacenamiento privado de la app, así que
  sigue visible tras reiniciar aunque el archivo original se mueva o borre.

### Pruebas

- 56 pruebas unitarias en 9 suites (13 nuevas: 9 del motor de recomendaciones y
  4 de los presets del ecualizador).
- Verificado en emulador API 37: cinco pestañas, migración 1→2, descarga real de
  MP3 con progreso, cancelación y reanudación, biblioteca con tamaño y
  eliminación segura, ecualizador con Bass Boost aplicado, listas con creación,
  renombrado, portada persistente, reordenación, reproducción y eliminación de
  canciones.

## [1.2.2] - 11 de noviembre de 2026

### Añadido

- Configuración de la clave de YouTube **desde la propia app**: tarjeta
  «Conecta YouTube» y diálogo con icono de llave, campo con visibilidad
  opcional y botón «Guardar y comprobar».
- La clave se valida contra Google con `videos.list` (1 unidad de cuota) antes
  de guardarse y se cifra con Android Keystore (AES-256/GCM). La clave guardada
  tiene prioridad sobre la compilada y se puede eliminar desde la app.
- Cabeceras `X-Android-Package` y `X-Android-Cert` (SHA-1 de la firma) en las
  peticiones directas, para poder restringir la clave al paquete
  `com.vibemusic.app` y al certificado de release.
- Modo **proxy** opcional mediante `VIBE_YOUTUBE_PROXY_URL`: el APK no contiene
  ninguna clave y las búsquedas se delegan a un servicio intermediario.
- Tests de credenciales (precedencia, formato y máscara), del modo proxy, de la
  validación y de las URL de búsqueda/detalles. Total: 43 tests en 7 suites.

### Cambiado

- `HttpJson` admite cabeceras HTTP opcionales.
- Los textos de Ajustes y de la sección YouTube explican las tres formas de
  configuración y ya no hablan solo del secreto de compilación.

### Corregido

- El icono de llave de la cabecera de YouTube ya no queda oculto por el título
  en pantallas estrechas.

### Notas

- La búsqueda con resultados reales sigue pendiente de una clave válida: no hay
  acceso a Google Cloud en el entorno de desarrollo y el repositorio no incluye
  ninguna clave. La petición real a Google, el error de clave inexistente, el
  error de formato y el error sin conexión sí se han ejecutado en el emulador.

## [1.2.1] - 11 de noviembre de 2026

### Añadido

- Reproductor oficial de YouTube **dentro de VibeMusic**: al pulsar un
  resultado se abre una pantalla propia con la IFrame Player API en un WebView.
  Ya no se lanza `vnd.youtube:` ni el navegador.
- Pantalla `YouTubePlayerScreen` con fondo oscuro, reproductor 16:9 arriba,
  título, canal, duración, fecha, descripción, aviso legal y botón «Volver a
  los resultados».
- HTML auditable en `assets/youtube_player.html` con `playsinline`, autoplay y
  el tamaño real del WebView; sin extracción de audio ni bloqueo de anuncios.
- Mensajes en español para vídeos privados, eliminados, con inserción
  desactivada, errores del reproductor y falta de respuesta (incluye códigos
  100, 101, 150, 152, 153, -1 y -2).
- Vigilancia nativa del estado del reproductor: si YouTube deja el vídeo en
  «sin iniciar» o no responde, la app reemplaza el reproductor por una
  explicación en lugar de dejar un marco negro.

### Cambiado

- Los resultados de YouTube muestran «Reproducir en VibeMusic» y el texto de la
  sección explica que la reproducción es interna.
- El vídeo seleccionado se conserva al rotar o recrear la actividad mediante
  `rememberSaveable`; atrás vuelve a los resultados sin perder la búsqueda.
- Al abrir el reproductor de YouTube se pausa Media3 si estaba sonando, para no
  solapar dos fuentes de audio. La reproducción local no cambia.

### Corregido

- El iframe del reproductor ya no se queda con altura 0 (se fija al tamaño real
  del WebView), por lo que el vídeo y sus controles son visibles.
- El reproductor ya no se carga con un origen de YouTube que provocaba el error
  152-4; ahora usa `https://com.vibemusic.app` como origen y base del HTML.

## [1.2.0] - 9 de noviembre de 2026

### Añadido

- Sección **YouTube** con búsqueda por título, artista o álbum mediante
  YouTube Data API v3.
- Resultados con miniatura, canal y duración real (parseo de ISO-8601 de
  `videos.list`).
- Reproducción delegada al reproductor oficial de YouTube: se abre
  `vnd.youtube:<id>` y, si la app no está instalada, el navegador con
  `https://www.youtube.com/watch?v=<id>`.
- Estados de carga, error, resultados vacíos y «configuración pendiente» en la
  sección YouTube.
- Caché en memoria por consulta y búsqueda solo al pulsar Buscar o el botón del
  teclado, para no gastar cuota.
- Error de cuota, clave no válida, API deshabilitada y red con mensajes
  específicos.
- Configuración de la clave por secreto de GitHub Actions, variable de entorno
  o `local.properties` (ignorado por Git), inyectada en
  `BuildConfig.YOUTUBE_API_KEY`.
- Nueva barra inferior de cuatro apartados (**Home**, **Samples**, **Explore**,
  **Library**) con estilo minimalista, iconos rellenos/delineados y transición
  discreta.
- Pantalla **Samples** con las instrumentales originales incluidas, filtros por
  género y reproducción completa en local.
- Acceso a YouTube desde **Explore**, sin una quinta pestaña.
- Tests unitarios de URLs, identificadores, combinación de respuestas,
  duraciones ISO-8601 y errores de YouTube.

### Cambiado

- La pestaña de búsqueda se integra en **Explore** como vista de búsqueda
  superpuesta; se conserva el buscador de Deezer y el de la biblioteca local.
- La barra inferior ya no usa el indicador tipo píldora de Material 3: fondo
  `#171717`, iconos blancos activos y grises inactivos, altura compacta y
  respeto de los insets de navegación.
- Textos legales (privacidad, términos y cookies) actualizados para YouTube
  Data API v3, miniaturas y reproducción oficial.
- Ajustes muestra el estado de la integración YouTube.
- `HttpJson` no reintenta respuestas 4xx para no consumir cuota inútilmente.

### Seguridad

- Sin claves en el repositorio ni en el APK de ejemplo: la clave se inyecta en
  tiempo de compilación desde secretos.
- Sin permisos nuevos: YouTube usa el permiso `INTERNET` existente.
- Sin extracción de audio, conversión a MP3, descargas de YouTube,
  reproducción en segundo plano por extracción ni bloqueo de anuncios.

## [1.1.0] - 7 de noviembre de 2026

### Añadido

- Buscador online con el catálogo público de Deezer (vistas previas oficiales
  de 30 s), pantallas de álbum y artista online.
- Bibliotecas separadas: dispositivo, descargas e importados.
- Importación y exportación CSV con detección de columnas y emparejamiento.
- Temas AMOLED, Midnight, Ocean, Purple, Crimson, Emerald y Light.
- Comprobación de actualizaciones desde GitHub Releases con verificación
  SHA-256 e instalación mediante el diálogo oficial de Android.
- Documentos legales y borrado de datos locales sin tocar archivos personales.
- Inicio de sesión opcional con Google Identity Services.

### Corregido

- Permisos de `gradlew` en CI antes de ejecutar los tests.
- Notas de release en texto plano en el diálogo de actualización.
- Firma de release estable a partir de esta versión.

## [1.0.x]

- Reproductor local con Media3, notificaciones multimedia, cola, favoritos,
  listas, escaneo de audio y descargas directas HTTPS.
- Las versiones 1.0.x usaron una firma distinta (1.0.2 se firmó con un keystore
  debug efímero del runner de CI).
