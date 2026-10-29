#!/usr/bin/env bash
# Prepara una versión de Ahorro+ para publicar en GitHub Releases.
#
#   scripts/publicar.sh 0.2.0            prepara dist/v0.2.0 e imprime los comandos para publicar
#   scripts/publicar.sh 0.2.0 --ensayo   lo mismo, pero sin revisar rama, etiqueta ni sincronización
#
# Sin el ensayo, el script no publica nada por su cuenta: crear la etiqueta y el Release
# lo haces tú con los comandos que imprime al final.
set -euo pipefail

V="${1:?Uso: scripts/publicar.sh X.Y.Z [--ensayo]}"
ENSAYO="${2:-}"
CERT_ESPERADO="f020703207ff0b9ebec92530ce7d7020a2e4ab1ca4fceddbfc6632dfa96dee03"
cd "$(dirname "$0")/.."

falla() { echo "ERROR: $*" >&2; exit 1; }
paso() { echo "-> $*"; }

[[ "$V" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || falla "la versión debe tener la forma X.Y.Z"

if [ "$ENSAYO" != "--ensayo" ]; then
    paso "Revisando el estado de git"
    [ "$(git branch --show-current)" = "main" ] || falla "publica desde main"
    git diff --quiet && git diff --cached --quiet || falla "hay cambios sin commit"
    git fetch -q origin
    [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || falla "main no está igual que origin/main"
    ! git rev-parse -q --verify "refs/tags/v$V" >/dev/null || falla "la etiqueta v$V ya existe"
fi

paso "Revisando versión y CHANGELOG"
grep -q "versionName = \"$V\"" app/build.gradle.kts || falla "app/build.gradle.kts no tiene versionName = \"$V\""
grep -q "^## \[$V\]" CHANGELOG.md || falla "CHANGELOG.md no tiene la sección ## [$V]"

SDK="${ANDROID_HOME:-${LOCALAPPDATA:-$HOME}/Android/Sdk}"
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
[ -n "$BT" ] || falla "no encontré build-tools del Android SDK en $SDK"
APKSIGNER="$BT/apksigner"; [ -f "$BT/apksigner.bat" ] && APKSIGNER="$BT/apksigner.bat"
AAPT2="$BT/aapt2"; [ -f "$BT/aapt2.exe" ] && AAPT2="$BT/aapt2.exe"

paso "Tests y compilación del APK firmado"
./gradlew --no-daemon -q clean :app:testDebugUnitTest :app:assembleRelease
APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || falla "no salió un APK firmado: ¿falta ~/.sencillo/keystore.properties?"

paso "Verificando firma y permisos"
CERT="$("$APKSIGNER" verify --print-certs "$APK" | grep "certificate SHA-256 digest" | head -1 | awk '{print $NF}' | tr -d '\r')"
[ "$CERT" = "$CERT_ESPERADO" ] || falla "el APK no está firmado con la clave de Ahorro+ ($CERT)"
if "$AAPT2" dump permissions "$APK" | grep -qi "android.permission.INTERNET"; then
    falla "el APK pide permiso de INTERNET: revisa qué librería lo agregó"
fi
"$AAPT2" dump badging "$APK" | grep -q "versionName='$V'" || falla "el APK no es la versión $V"

# Los ensayos van a otra carpeta para no pisar nunca una versión ya publicada.
D="dist/v$V"; [ "$ENSAYO" = "--ensayo" ] && D="dist/ensayo-v$V"
[ "$ENSAYO" = "--ensayo" ] || [ ! -e "$D" ] || falla "$D ya existe: esa versión ya se preparó o publicó"
paso "Preparando $D"
NOMBRE="ahorro-mas-v$V.apk"
mkdir -p "$D"
cp "$APK" "$D/$NOMBRE"
(cd "$D" && sha256sum "$NOMBRE" > "$NOMBRE.sha256")
{
    echo "## Novedades"
    awk -v v="## [$V]" 'index($0, v) == 1 {dentro=1; next} /^## \[/ {dentro=0} dentro' CHANGELOG.md
    cat <<EOF
## Instalar
1. Descarga \`$NOMBRE\`.
2. Ábrelo en el celular y permite instalar apps desde tu navegador cuando Android lo pida.
3. Si Play Protect avisa "desarrollador desconocido", toca "Instalar de todas formas": la app no viene de la Play Store.

Si ya tenías Ahorro+, se instala encima y tus datos se mantienen.

## Verificar (opcional)
- SHA-256: en \`$NOMBRE.sha256\`
- Certificado de firma: \`$(echo "$CERT_ESPERADO" | sed 's/../&:/g; s/:$//')\`

Sin internet, sin cuentas ni publicidad. Requiere Android 8.0 o superior.
EOF
} > "$D/notas.md"

echo
echo "Listo: $D"
cat "$D/$NOMBRE.sha256"
if [ "$ENSAYO" = "--ensayo" ]; then
    echo "(Ensayo: no se revisó git. No publiques desde aquí.)"
else
    cat <<EOF

Prueba el APK en el emulador instalándolo encima de la versión anterior, y después publica:

  git tag -a v$V -m "Ahorro+ $V" && git push origin v$V
  gh release create v$V "$D/$NOMBRE" "$D/$NOMBRE.sha256" --title "Ahorro+ v$V" --notes-file "$D/notas.md" --verify-tag
EOF
fi
