# Registro de decisiones

Cada decisión cerrada se anota aquí con su fecha y su porqué. Antes de proponer algo nuevo, revisar que no contradiga una decisión ya tomada. Si hay que cambiarla, se agrega una entrada nueva que la reemplace; no se borra la anterior.

## 2026-10-28: App nativa de Android, no web
Kotlin, Jetpack Compose y Room. Entrar a una web o a un Excel desde el celular es incómodo, y la persona debe tener el control desde su propio teléfono.

## 2026-10-28: Base de datos local, sin servidor
No habrá uso masivo y es una herramienta personal. Room (SQLite) en el celular. Sin cuentas ni sincronización.

## 2026-10-28: Sin permiso de internet
Es la base de la promesa de privacidad: los datos nunca salen del celular, salvo un respaldo cifrado que exporta la persona. Una integración que necesite internet tiene que ser opcional, ir aislada y documentarse antes en `SEGURIDAD.md`.

## 2026-10-28: Nombre Ahorro+ (antes "Sencillo")
Los identificadores internos (`pe.sencillo.app`, clases, variables `SENCILLO_*`) no se cambian: cambiar el id del paquete obligaría a desinstalar y perder datos.

## 2026-10-28: Repositorio público
Es más transparente: cualquiera puede revisar que la app no envía datos. El historial se auditó antes de abrirlo: no tiene claves ni datos personales.

## 2026-10-28: La clave de firma vive solo en la PC de Augusto
Está en `~/.sencillo/`. Los Releases se firman en local con `scripts/publicar.sh`. Subirla como secreto a GitHub queda como opción futura. En Google Play se usará esta misma clave.

## 2026-10-28: Gratis y sin anuncios; donaciones más adelante
Los anuncios pagan poco en Perú (banner de unos 0.15 USD por mil vistas), obligan a pedir internet y a usar SDKs que recolectan datos, y estorban al anotar. Las donaciones voluntarias se habilitan cuando la app sea más robusta y tenga usuarios activos.

## 2026-10-29: "Límites por categoría" en vez de "Sobres"
Las primeras personas que la usaron no entendieron qué era un "sobre". El nombre debe decir qué hace: el monto máximo que quieres gastar en cada categoría al mes. Por la misma razón, el + se llama "Anotar" y "Boleta" pasó a "Movimientos". La tabla interna sigue llamándose `sobres`, para no migrar la base de datos.

## 2026-10-29: Diseño plano, sin adornos
El diseño anterior se veía "plástico", como generado por IA: gradientes, rosetón, monedas con brillo y tarjetas iguales en todas partes. Desde ahora:
- Superficies planas, con secciones separadas por espacio y títulos.
- El color solo comunica: acción, aviso o serie de un gráfico.
- Lo único que destaca es el monto de lo que te queda.

Cualquier pantalla nueva sigue estas reglas (ver el comentario de `ui/tema/Tema.kt`).

