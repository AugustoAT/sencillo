# Integraciones

El objetivo es que registrar un gasto cueste cada vez menos. Toda integración entra por `app/src/main/java/pe/sencillo/ingesta/Ingesta.kt` y cumple tres reglas:

1. **Propone, no guarda.** Una fuente produce un `MovimientoPropuesto`. La persona lo ve en la hoja de "Anotar", ya llenada, y confirma con un toque.
2. **Todo pasa por `ValidadorDePropuestas`**: tipo conocido, monto entre S/ 0.01 y S/ 1 000 000, fecha razonable, categoría del catálogo y nota limpia.
3. **Permisos opcionales y explicados.** Si una fuente necesita un permiso, se activa desde Ajustes, se explica antes de pedirlo y la app funciona igual sin ella.

## Hoy

| Fuente | Permisos | Estado |
|---|---|---|
| Atajo "Anotar gasto": mantener presionado el ícono de Ahorro+ | Ninguno | Listo |

## Siguientes, en orden sugerido

| Fuente | Cómo funcionaría | Permisos | Riesgos y cómo cuidarlos |
|---|---|---|---|
| Widget de inicio | Botón "Anotar" y el saldo del mes en la pantalla de inicio. | Ninguno | El widget muestra montos en la pantalla de inicio: que sea opcional y que se pueda ocultar el saldo. |
| Mosaico de ajustes rápidos | Anotar desde la cortina de notificaciones. | Ninguno | Ninguno relevante. |
| Importar estado de cuenta (CSV) | La persona elige el archivo que descargó de su banco. Se muestran los movimientos y se marcan cuáles entran. | Ninguno (selector de archivos del sistema) | El archivo es de otra fuente: límite de tamaño, parser estricto y nada se guarda sin confirmar. |
| Notificaciones de Yape / Plin / banco | Un `NotificationListenerService` lee solo las notificaciones de esas apps y arma una propuesta: "Yapeaste S/ 12 a…". | Acceso a notificaciones (se activa a mano en Ajustes de Android) | Es el permiso más delicado. Debe filtrar por nombre de paquete exacto, no guardar el texto original de la notificación, mostrar las propuestas en una bandeja y poder apagarse en un toque. |
| Fotografiar una boleta | Leer el total de una foto. | Cámara, o elegir una foto | El OCR en el celular agrega peso al APK. Nunca subir la foto a un servidor. |

## Cosas que no se van a hacer

- **Leer SMS.** Pide `READ_SMS`, que da acceso a todos los mensajes, incluidos los códigos de verificación de bancos. El riesgo es demasiado alto.
- **Conectarse a bancos con usuario y contraseña** (scraping). Ahorro+ nunca debe tener credenciales bancarias.
