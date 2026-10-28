# Proceso de desarrollo

Cómo se trabaja en Ahorro+ para que el proyecto se mantenga ordenado a largo plazo.

## 1. Planificar

- Todo trabajo empieza como un **issue**, con su etiqueta (`bug`, `mejora`, `seguridad`, `integración`, `docs`) y su **hito** (la versión donde entra).
- La [hoja de ruta](../ROADMAP.md) dice qué entra en cada versión y con qué criterios se publica.
- Las decisiones importantes se anotan en [decisiones.md](decisiones.md).

## 2. Desarrollar

- `main` está protegida: nada entra sin un pull request y con el CI en verde.
- Una rama por issue, con nombre `tipo/descripcion-corta`, por ejemplo `mejora/recordatorio-diario` o `bug/teclado-cortado`.
- Commits pequeños, en español, que expliquen el porqué. Autor: AugustoAT.
- Lo nuevo de la lógica (`dominio/`, `seguridad/`, `ingesta/`) lleva tests unitarios.
- Si cambia el esquema de la base de datos:
  - Sube `version` en `@Database`.
  - Escribe una `Migration`, con su test.
  - **Nunca** uses `fallbackToDestructiveMigration`: borraría los datos de la gente.

### Definición de terminado
- [ ] El CI pasa: tests, compilación y verificación de dependencias.
- [ ] Probado en el emulador, incluido el modo oscuro.
- [ ] El APK sigue sin permiso de INTERNET (el script de publicación lo revisa).
- [ ] CHANGELOG actualizado en "Sin publicar".
- [ ] Si toca algo de seguridad o privacidad, `SEGURIDAD.md` también se actualizó.

## 3. Publicar

Una versión menor por mes como ritmo de referencia. Los arreglos urgentes salen como parche (0.2.1).

1. En `app/build.gradle.kts`, sube `versionCode` (+1) y `versionName`.
2. En `CHANGELOG.md`, mueve "Sin publicar" a la versión nueva con su fecha.
3. Mergea a `main` por PR y espera el CI en verde.
4. Desde `main` actualizada:
   ```bash
   scripts/publicar.sh 0.2.0
   ```
   El script compila el APK firmado y revisa el certificado y que no tenga internet. Después prepara `dist/v0.2.0/` e imprime los comandos para crear la etiqueta y el Release.
5. Prueba ese mismo APK en el emulador **instalándolo encima de la versión anterior**: los datos deben seguir ahí.
6. Corre los comandos que imprimió el script.

## 4. Mantener

- **Revisión mensual de dependencias**, con las reglas de [SEGURIDAD.md](../SEGURIDAD.md): avisos de los últimos 7 días, 3 días de espera desde la publicación y luego regenerar `gradle/verification-metadata.xml`.
- **Cada agosto**, subir `targetSdk` al nivel que exige Google Play ese año.
- Los reportes de bugs llegan por issues. En Google Play, también por Android vitals, sin SDKs de rastreo dentro de la app.
