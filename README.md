# VibeMusic

Reproductor de música para Android con interfaz Compose, biblioteca local y un
buscador de catálogo que reproduce **vistas previas oficiales de 30 segundos**
de Deezer. No extrae audio de plataformas de streaming ni descarga canciones
completas de catálogos protegidos. El catálogo completo de streaming está en
desarrollo.

## Funciones

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

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Cubren el parser/exportador CSV, el comparador de versiones y el parser de
releases de GitHub, los parsers de Deezer y el parser del perfil de Google.
En el emulador API 37 se han probado además la reproducción local, la cola, la
notificación, el buscador online, las pantallas de álbum/artista, los temas y su
persistencia, la exportación e importación real de CSV, el borrado de datos, los
documentos legales y la comprobación de actualizaciones.

## Arquitectura

- `data/`: Room, MediaStore, descargas, ajustes, CSV, Deezer, GitHub y Google.
- `playback/`: servicio Media3, controlador y sesión multimedia.
- `ui/`: pantallas Compose, componentes, tema y diálogos.
- `viewmodel/`: estado de la interfaz y acciones del usuario.
- `.github/workflows/release-apk.yml`: tests, firma opcional, SHA-256 y release.

## Estado de verificación

La versión 1.0.x se instaló en un vivo v40 con Android 16 y sus funciones
básicas funcionaban. Las funciones nuevas de la versión 1.1.0 se han probado en
un emulador API 37: buscador online, álbum y artista, reproducción de vistas
previas, CSV, temas, actualización manual, borrado de datos y reproducción
local con notificación y cola. También se probó el ciclo completo de
actualización: una build 1.0.9 detectó la 1.1.0 publicada, descargó el APK,
verificó su SHA-256 y se actualizó con el diálogo oficial de Android.

## Limitaciones

- El buscador online reproduce únicamente vistas previas de 30 segundos; no hay
  reproducción completa ni descarga de canciones de Deezer.
- Las funciones nuevas de 1.1.0 se han probado en emulador; no se han probado
  en un teléfono físico.
- Las descargas solo aceptan URLs HTTPS directas; no hay scraping ni extracción
  de audio de YouTube.
- No se ofrece un catálogo comercial ilimitado.
- El inicio de sesión con Google requiere configurar un ID de cliente OAuth.
- La firma de la versión 1.0.2 se generó con un keystore debug efímero del
  runner de CI y su clave privada no existe en el repositorio. Por eso la nueva
  versión usa una firma estable distinta y requiere desinstalar una vez la
  versión anterior antes de instalar la nueva. A partir de esta versión, las
  siguientes actualizaciones conservarán la misma firma.
