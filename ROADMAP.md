# Hoja de ruta

Cada versión tiene un objetivo y **criterios de salida**: no se publica hasta cumplirlos todos. El detalle de cada tarea vive en los [issues](../../issues), agrupados por [hito](../../milestones).

Modelo: **gratis y sin anuncios**. Las donaciones voluntarias se habilitan cuando la app tenga usuarios activos (ver `docs/decisiones.md`).

## v0.2: que se use todos los días

Objetivo: que anotar se vuelva hábito y que la app cumpla los requisitos técnicos de Google Play.

- [ ] Recordatorio diario opcional ("¿anotaste tus gastos de hoy?"). Es una notificación local, sin internet. (#2)
- [ ] Widget de inicio con el botón "Anotar" y, opcionalmente, el saldo del mes. (#3)
- [ ] Actualizar a Android 16 (API 36): `compileSdk`, `targetSdk` y dependencias. (#1)

Criterios de salida:
- Todos los tests pasan y hay tests nuevos para lo agregado.
- El APK sigue sin permiso de INTERNET.
- Probado en emulador, y en al menos un celular real con huella.
- CHANGELOG actualizado.

## v0.3: que se adapte a cada persona

- [ ] Categorías personalizadas: crear, renombrar y ocultar. (#4)
- [ ] Presupuesto por quincena, además de mensual. (#5)
- [ ] Gastos fijos recurrentes, como el alquiler o el plan del celular, que se anotan solos cada mes previa confirmación. (#6)

Criterios de salida: los de v0.2, más pruebas de migración de base de datos de v0.2 a v0.3 sin pérdida de datos.

## v0.4: menos tipeo

- [ ] Exportar a CSV para abrirlo en Excel o Google Sheets. (#7)
- [ ] Importar el estado de cuenta del banco en CSV, con confirmación por movimiento (ver `INTEGRACIONES.md`). (#8)

## v1.0: Google Play

- [ ] Política de privacidad publicada (GitHub Pages) y ficha de "Seguridad de datos": no recolecta datos. (#9)
- [ ] Firma de Google Play con la **clave propia**, para que quien instaló desde GitHub pueda actualizar desde Play.
- [ ] Prueba cerrada: 12 testers o más durante 14 días seguidos. (#10)
- [ ] Publicación escalonada: 20% de los usuarios y luego 100%.

Criterios de salida: cero crashes en la prueba cerrada y comentarios de los testers atendidos.

## Después de v1.0

- Donaciones voluntarias: GitHub Sponsors o Ko-fi en el README; en Play, solo por los medios que permita su política.
- Presupuesto compartido en pareja.
- iPhone, solo si Android muestra tracción. La lógica de `dominio/` es Kotlin puro y se podría reusar con Kotlin Multiplatform.
