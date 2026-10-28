## Qué cambia y por qué

Cierra #

## Cómo se probó

- [ ] Tests unitarios (`./gradlew :app:testDebugUnitTest`)
- [ ] En el emulador, también en modo oscuro
- [ ] Instalando encima de la versión anterior, si cambia la base de datos

## Revisión

- [ ] El APK sigue sin permiso de INTERNET
- [ ] No hay claves, datos personales ni APK en el commit
- [ ] Si cambia el esquema: `Migration` y su test, sin migración destructiva
- [ ] Dependencias nuevas o actualizadas con las reglas de SEGURIDAD.md y la verificación regenerada
- [ ] CHANGELOG actualizado en "Sin publicar"
