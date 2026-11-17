# Changelog

Todas las versiones publicadas de VibeMusic. El formato sigue la idea de
«Keep a Changelog» y las versiones usan SemVer.

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
