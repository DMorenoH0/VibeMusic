# VibeMusic

Reproductor de música local para Android con interfaz Compose en negro/rojo.
La app trabaja con audio del dispositivo, archivos importados por el usuario,
descargas directas por HTTPS y seis instrumentales originales incluidos como
muestras. No incluye ni extrae catálogos de plataformas de streaming.

## Funciones

- Reproducción en segundo plano con Media3 ExoPlayer y MediaSession.
- Notificación multimedia y controles en pantalla de bloqueo.
- Reproducción, pausa, anterior, siguiente, aleatorio, repetir y velocidad.
- Cola de reproducción con salto, eliminación y vaciado.
- Inicio, Explorar, Biblioteca, Buscar y Ajustes.
- Biblioteca por canciones, álbumes, listas y favoritos.
- Escaneo real de audio local con MediaStore y permisos de Android 13+.
- Importación mediante el selector de archivos (SAF).
- Descargas de enlaces directos HTTPS (MP3, M4A, OGG, FLAC, WAV).
- Persistencia con Room: favoritos, listas, descargas e historial.
- Temporizador de sueño y opciones de apariencia/descarga.
- Estados de carga, vacío y error, con textos en español.

Las muestras incluidas son instrumentales generadas para este proyecto y se
guardan en `app/src/main/assets/demo`. La app no aloja música de terceros.

## Requisitos

- Android 7.0 (API 24) o superior.
- JDK 21.
- Android SDK con compileSdk 35.
- Gradle 8.11.1 (wrapper incluido).

## Compilación

```bash
./gradlew assembleDebug
```

El APK se genera en:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Arquitectura

- `data/`: Room, MediaStore, descargas, ajustes y repositorio.
- `playback/`: servicio Media3, controlador y sesión multimedia.
- `ui/`: pantallas Compose, componentes, tema y diálogos.
- `viewmodel/`: estado de la interfaz y acciones del usuario.

## Estado de verificación

El apk se ha instalado en un vivo v40 con la version de android 16 y todos los apartados de la app funcionan correctamente

## Limitaciones

- Las descargas solo aceptan URLs HTTPS directas; no hay scraping ni extracción
  de audio de YouTube.
- No se ofrece un catálogo comercial ilimitado.
- La app es una compilación debug sin firma de producción.
