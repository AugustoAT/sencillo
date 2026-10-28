# Seguridad y protección de datos

Ahorro+ guarda cuánto gana y en qué gasta una persona. Es información sensible, así que la app parte de una regla: **tus datos no salen del celular, salvo que tú exportes un respaldo cifrado**. Además, la app no puede servir de puerta de entrada para atacar el resto del dispositivo.

## De qué nos protegemos

| Riesgo | Qué hace Ahorro+ |
|---|---|
| La app envía datos a un servidor, sea por un bug o por una librería maliciosa | No tiene permiso de **internet**. El manifest lo elimina de forma explícita (`tools:node="remove"`), aunque una librería intente agregarlo. Android bloquea cualquier conexión. |
| Otra app del celular lee o modifica los datos | La base de datos vive en el espacio privado de la app (sandbox de Android). No se exporta ningún `ContentProvider`, `Service` ni `BroadcastReceiver`. La única pantalla exportada es la del launcher. |
| Otra app manda datos falsos a Ahorro+ (intents) | El atajo "Anotar gasto" solo acepta su acción exacta y **no lee ningún dato** del intent. Solo abre la hoja vacía. |
| Alguien toma el celular desbloqueado | Bloqueo opcional con huella, rostro o PIN del celular (`BiometricPrompt`). Se vuelve a pedir si la app pasa más de 30 segundos en segundo plano. Ahorro+ nunca ve la huella ni el PIN. |
| Capturas de pantalla o vista previa en "apps recientes" | `FLAG_SECURE` activo por defecto: el contenido se tapa en recientes y no se pueden hacer capturas. Se puede desactivar en Ajustes. |
| Copias automáticas a la nube o a otro equipo | `allowBackup="false"` y reglas de extracción que excluyen todo. Para mudarse de celular está el respaldo cifrado. |
| Pérdida o robo del archivo de respaldo | El respaldo se cifra con **AES-256-GCM**. La clave se deriva de una contraseña con **PBKDF2-HMAC-SHA256 (600 000 iteraciones)**, sal aleatoria de 16 bytes e IV de 12 bytes. Sin la contraseña no se puede abrir. |
| Un respaldo manipulado | GCM autentica todo, cabecera incluida: cualquier cambio hace fallar el descifrado. Al importar se valida cada campo (tipos, rangos, largos, categorías conocidas, máximo de registros y tamaño máximo de 20 MB) antes de tocar la base de datos. El reemplazo es una sola transacción: si algo falla, no queda nada a medias. |
| Inyección SQL | Room con consultas fijas y parámetros enlazados. Nunca se arma SQL con texto. |
| Texto malicioso en notas (caracteres invisibles, saltos de línea) | Las notas se recortan a 60 caracteres y se les quitan los caracteres de control y de formato (por ejemplo U+202E). |
| Librerías adulteradas (cadena de suministro) | Versiones exactas en `gradle/libs.versions.toml`, cada repositorio limitado a sus grupos (`settings.gradle.kts`), **verificación SHA-256 de cada dependencia** (`gradle/verification-metadata.xml`), wrapper de Gradle con checksum oficial y acciones de CI fijadas por SHA de commit. |
| Ingeniería inversa del APK | R8 minimiza y ofusca el release, y se eliminan los logs. No hay secretos dentro del APK que valga la pena extraer. |

## Lo que Ahorro+ no protege

- **Un celular con root o con malware de sistema** puede leer el almacenamiento de cualquier app. Con el celular apagado o bloqueado, los datos quedan protegidos por el cifrado de Android (FBE).
- **La contraseña del respaldo** solo la conoces tú. Si la olvidas, ese respaldo no se puede abrir.

## Reglas para quien modifique el código

1. **No agregar el permiso de internet.** Si una integración futura lo necesita, debe ir en un módulo aparte y opcional, y documentarse aquí antes.
2. Ninguna integración guarda movimientos directamente: todo pasa por `ValidadorDePropuestas` y la persona lo confirma (ver `INTEGRACIONES.md`).
3. Antes de subir una dependencia:
   - Revisar avisos de seguridad de los últimos 7 días.
   - Esperar 3 días desde que se publicó la versión.
   - Regenerar la verificación con `./gradlew --write-verification-metadata sha256 :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest help` y revisar el diff.
4. Las claves de firma viven fuera del repo (`~/.sencillo/` o secretos de GitHub). `.gitignore` bloquea `*.jks`, `*.keystore` y `keystore.properties`.
5. Revisar el manifest final después de agregar librerías:
   `aapt2 dump permissions app-release.apk` no debe mostrar `INTERNET`.

## Permisos del APK publicado

```
uses-permission: android.permission.USE_BIOMETRIC      (normal: para el bloqueo con huella)
uses-permission: android.permission.USE_FINGERPRINT    (normal: lo mismo, en Android 8 y 9)
permission:      pe.sencillo.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (de firma, interno de androidx)
```

Android concede los tres sin preguntar. Ninguno da acceso a datos personales, a la red ni a otros archivos.
