# Changelog

Todas las versiones publicadas de VibeMusic. El formato sigue la idea de
«Keep a Changelog» y las versiones usan SemVer.

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
