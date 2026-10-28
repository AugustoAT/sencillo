# Sencillo

App de Android para llevar el sueldo, los gastos y el ahorro del mes, en soles. Se anota cada gasto (hasta el menú de S/ 12) en segundos, y la app muestra a dónde se va la plata y sugiere qué cambiar.

**Sin internet, sin cuentas, sin publicidad.** Todo queda en el celular.

<p>
  <img src="docs/capturas/mes.png" width="240" alt="Mi mes: el saldo como un billete, el estado del mes y los sobres">
  <img src="docs/capturas/anotar.png" width="240" alt="Anotar un gasto con el teclado propio">
  <img src="docs/capturas/analisis.png" width="240" alt="Análisis: ritmo del mes y regla 50/30/20">
</p>

## Qué hace

- **Mi mes**: lo que te queda del mes como un billete, cuánto puedes gastar por día y los días que faltan. Debajo, los **sobres** (presupuesto por categoría) que se ponen ámbar cuando te acercas al límite y rojos cuando lo pasas.
- **Anotar**: el botón dorado abre un teclado propio. Monto, categoría y listo. También registra ingresos extra (cachuelo, gratificación) y aportes de ahorro, y guarda cómo pagaste: efectivo, Yape/Plin, débito o crédito.
- **Boleta**: todos los movimientos del mes por día. Al tocar uno se puede editar o borrar.
- **Análisis**:
  - Ritmo de gasto contra el límite que te deja ahorrar, con la proyección a fin de mes.
  - Regla 50/30/20.
  - Gasto por categoría contra su sobre y contra el mes anterior.
  - Medios de pago.
- **Recomendaciones**: se calculan en el celular. Detectan:
  - Ritmo de gasto excesivo.
  - Sobres pasados o por pasarse.
  - Gastos hormiga.
  - Falta de ahorro.
  - Uso alto de la tarjeta de crédito.
  - Ausencia de un fondo de emergencia.

  Cada una dice cuánto se podría ahorrar.
- **Metas**: alcancías con cuánto aportar al mes para llegar a la fecha.
- **Ajustes**: sueldo neto, día de pago, % de ahorro, sobres, bloqueo con huella, respaldo cifrado.

## Instalar

1. En [Releases](../../releases) descarga `sencillo-vX.Y.Z.apk` y su `.sha256`.
2. Verifica el archivo: `sha256sum -c sencillo-vX.Y.Z.apk.sha256`.
3. En el celular, permite instalar apps de esa fuente cuando Android lo pida, y abre el APK.

Requiere Android 8.0 o superior.

> El certificado de firma de las versiones oficiales tiene este SHA-256:
> `f0:20:70:32:07:ff:0b:9e:be:c9:25:30:ce:7d:70:20:a2:e4:ab:1c:a4:fc:ed:db:fc:66:32:df:a9:6d:ee:03`.
> Puedes revisarlo con `apksigner verify --print-certs`.

## Seguridad

Ver [SEGURIDAD.md](SEGURIDAD.md). En corto:
- No tiene permiso de internet.
- No exporta componentes.
- No hace copias a la nube.
- Tiene bloqueo opcional con huella.
- Oculta el contenido en "recientes".
- El respaldo se cifra con AES-256-GCM.
- Todas las dependencias se verifican por SHA-256.

## Compilar

Requisitos: JDK 17 y Android SDK 34.

```bash
./gradlew :app:testDebugUnitTest      # tests
./gradlew :app:assembleDebug          # APK de prueba (pe.sencillo.app.debug)
./gradlew :app:assembleRelease        # APK firmado
```

`assembleRelease` busca la clave de firma así:
1. Primero en las variables `SENCILLO_KEYSTORE`, `SENCILLO_KEYSTORE_CLAVE`, `SENCILLO_KEY_ALIAS` y `SENCILLO_KEY_CLAVE`.
2. Si no están, en `~/.sencillo/keystore.properties`.

Si no encuentra ninguna, genera un APK sin firmar. **Guarda una copia de la clave fuera de la PC**: si se pierde, las actualizaciones no se pueden instalar encima de la versión anterior.

### Publicar una versión desde GitHub

1. Carga la clave como secreto, una sola vez:
   ```bash
   base64 -w0 ~/.sencillo/sencillo-release.jks | gh secret set SENCILLO_KEYSTORE_B64
   gh secret set SENCILLO_KEYSTORE_CLAVE      # pega la clave de ~/.sencillo/keystore.properties
   ```
2. Sube `versionCode` y `versionName` en `app/build.gradle.kts`, crea la etiqueta y súbela:
   ```bash
   git tag v0.1.0 && git push origin v0.1.0
   ```
   El workflow corre los tests, compila, firma y publica el APK con su SHA-256 en Releases.

## Estructura

```
app/src/main/java/pe/sencillo/
  datos/       Room: movimientos, perfil, sobres, metas (dinero en céntimos)
  dominio/     catálogo, análisis del mes y recomendaciones (Kotlin puro, con tests)
  seguridad/   bloqueo biométrico, cifrado y validación del respaldo
  ingesta/     punto de entrada para integraciones (ver INTEGRACIONES.md)
  ui/          Jetpack Compose: pantallas, tema y componentes
prototipo/     primer prototipo web (HTML), solo como referencia de diseño
```

Stack:
- Kotlin 2.0.0
- Jetpack Compose (BOM 2024.06.00)
- Room 2.6.1
- androidx.biometric 1.1.0
- AGP 8.5.2
- Gradle 8.9

Las fuentes Archivo, Albert Sans e IBM Plex Mono van dentro de la app con licencia SIL OFL 1.1 (ver `licencias/`).
