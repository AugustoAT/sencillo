package pe.sencillo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.sencillo.BuildConfig
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Grupo
import pe.sencillo.seguridad.Bloqueo
import pe.sencillo.seguridad.Respaldo
import pe.sencillo.seguridad.RespaldoInvalido
import pe.sencillo.ui.tema.Tema
import java.io.InputStream
import java.time.LocalDate

private fun leerConLimite(entrada: InputStream, limite: Int): ByteArray? {
    val salida = java.io.ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    while (true) {
        val n = entrada.read(buf)
        if (n < 0) break
        salida.write(buf, 0, n)
        if (salida.size() > limite) return null
    }
    return salida.toByteArray()
}

@Composable
fun PantallaAjustes(
    e: EstadoUi.Listo,
    vm: AppViewModel,
    enfocarSobres: Boolean,
    avisar: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    val p = Tema.p
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val perfil = e.perfil
    var sueldo by remember { mutableStateOf(Dinero.aTexto(perfil.sueldo)) }
    var diaPago by remember { mutableStateOf(perfil.diaPago.toString()) }
    var ahorro by remember { mutableStateOf(perfil.ahorroPct.toString()) }
    val sobres = remember { mutableStateMapOf<String, String>().apply { Catalogo.gastos.forEach { put(it.id, Dinero.aTexto(e.sobres[it.id] ?: 0L)) } } }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var dialogoClave by remember { mutableStateOf<String?>(null) } // "exportar" | "importar"
    var claveExportar by remember { mutableStateOf<CharArray?>(null) }
    var bytesImportar by remember { mutableStateOf<ByteArray?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    val bloqueoDisponible = remember { Bloqueo.disponible(ctx) }
    val lista = rememberLazyListState()
    LaunchedEffect(enfocarSobres) { if (enfocarSobres) lista.scrollToItem(2) }

    val crearArchivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val clave = claveExportar
        claveExportar = null
        if (uri == null || clave == null) { clave?.fill(' '); return@rememberLauncherForActivityResult }
        trabajando = true
        scope.launch {
            try {
                val datos = vm.respaldoCifrado(clave)
                if (datos == null) avisar("No hay datos para respaldar.")
                else {
                    withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(datos) } }
                    avisar("Respaldo guardado. Guarda también tu contraseña: sin ella no se puede abrir.")
                }
            } catch (ex: Exception) {
                avisar("No se pudo guardar el respaldo. Elige otra carpeta e inténtalo de nuevo.")
            } finally {
                clave.fill(' ')
                trabajando = false
            }
        }
    }
    val abrirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = try {
                withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { leerConLimite(it, Respaldo.MAX_BYTES) } }
            } catch (ex: Exception) { null }
            if (bytes == null) avisar("No se pudo leer ese archivo o es demasiado grande.")
            else { bytesImportar = bytes; dialogoClave = "importar" }
        }
    }

    fun guardar() {
        val s = Dinero.deTexto(sueldo)
        val d = diaPago.toIntOrNull()
        val a = ahorro.toIntOrNull()
        val nuevos = sobres.mapValues { (_, v) -> Dinero.deTexto(v.ifBlank { "0" }) }
        error = when {
            s == null || s <= 0 -> "Escribe tu sueldo neto en soles."
            d == null || d !in 1..31 -> "El día de pago va del 1 al 31."
            a == null || a !in 0..80 -> "La meta de ahorro va de 0% a 80%."
            nuevos.values.any { it == null } -> "Revisa los montos de los sobres."
            else -> null
        }
        if (error != null) return
        vm.guardarAjustes(perfil.copy(sueldo = s!!, diaPago = d!!, ahorroPct = a!!), nuevos.mapValues { it.value!! })
        avisar("Ajustes guardados")
        onCerrar()
    }

    Box(Modifier.fillMaxSize().background(p.papel)) {
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ajustes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCerrar) { Icon(Iconos.cerrar, "Cerrar ajustes", tint = p.texto2) }
                }
            }
            item {
                Seccion("Tu sueldo") {
                    Campo("Sueldo neto mensual (S/)", sueldo, { sueldo = it }, KeyboardType.Decimal, "Lo que te llega después de AFP u ONP.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Campo("Día de pago", diaPago, { diaPago = it.filter(Char::isDigit).take(2) }, KeyboardType.Number, modifier = Modifier.weight(1f))
                        Campo("Meta de ahorro (%)", ahorro, { ahorro = it.filter(Char::isDigit).take(2) }, KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Seccion("Sobres del mes", accion = "Repartir 50/30/20", onAccion = {
                    val s = Dinero.deTexto(sueldo) ?: perfil.sueldo
                    val a = (ahorro.toIntOrNull() ?: perfil.ahorroPct).coerceIn(0, 80)
                    Catalogo.sobresBase(s, a).forEach { (k, v) -> sobres[k] = Dinero.aTexto(v) }
                }) {
                    Grupo.entries.forEach { g ->
                        Text(g.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.tenue, modifier = Modifier.padding(top = 6.dp))
                        Catalogo.gastos.filter { it.grupo == g }.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.nombre, Modifier.weight(1f), fontSize = 15.sp)
                                OutlinedTextField(
                                    value = sobres[c.id] ?: "",
                                    onValueChange = { v -> sobres[c.id] = v.filter { it.isDigit() || it == '.' }.take(10) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    prefix = { Text("S/ ") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(140.dp),
                                )
                            }
                        }
                    }
                    val total = sobres.values.sumOf { Dinero.deTexto(it.ifBlank { "0" }) ?: 0L }
                    val lim = (Dinero.deTexto(sueldo) ?: 0L) * (100 - (ahorro.toIntOrNull() ?: 0)) / 100
                    Text(
                        "Total en sobres: ${Dinero.sol0(total)} de ${Dinero.sol0(lim)} disponibles para gastar." +
                            if (total > lim) " Te pasas por ${Dinero.sol0(total - lim)}." else "",
                        fontSize = 13.sp, color = if (total > lim) p.alertaTexto else p.texto2, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                error?.let { Text(it, color = p.alertaTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
                BotonPrincipal("Guardar ajustes", ::guardar, Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            item {
                Seccion("Privacidad y seguridad") {
                    Interruptor(
                        "Pedir huella o PIN al abrir",
                        if (bloqueoDisponible) "Usa el bloqueo de tu celular. Sencillo nunca ve tu huella ni tu PIN."
                        else "Primero configura un bloqueo de pantalla en tu celular.",
                        perfil.bloqueo,
                        habilitado = bloqueoDisponible,
                    ) { activar ->
                        if (!activar) vm.cambiarSeguridad(perfil.copy(bloqueo = false))
                        else (ctx as? FragmentActivity)?.let { act ->
                            // Se confirma con la huella antes de activar, para no dejar a nadie fuera de su app.
                            Bloqueo.pedir(act, { vm.cambiarSeguridad(perfil.copy(bloqueo = true)) }, { avisar(it) })
                        }
                    }
                    Interruptor(
                        "Ocultar en apps recientes",
                        "Tapa el contenido en la vista de apps abiertas y bloquea las capturas de pantalla.",
                        perfil.ocultarPantalla,
                    ) { vm.cambiarSeguridad(perfil.copy(ocultarPantalla = it)) }
                    Row(Modifier.padding(top = 10.dp)) {
                        Icon(Iconos.candado, null, tint = p.marca, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sencillo no tiene permiso para usar internet. Tus datos se guardan solo en este celular y no se copian a la nube.",
                            fontSize = 13.sp, color = p.texto2,
                        )
                    }
                }
            }
            item {
                Seccion("Respaldo") {
                    Text(
                        "Para cambiar de celular o no perder nada, guarda un respaldo cifrado con una contraseña. Sin la contraseña nadie puede abrirlo, ni siquiera tú.",
                        fontSize = 14.sp, color = p.texto2,
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonSecundario(if (trabajando) "Cifrando…" else "Guardar respaldo", { if (!trabajando) dialogoClave = "exportar" }, Modifier.weight(1f))
                        BotonSecundario("Restaurar", { abrirArchivo.launch(arrayOf("application/octet-stream", "*/*")) }, Modifier.weight(1f))
                    }
                }
            }
            item {
                Seccion("Zona de cuidado") {
                    BotonSecundario(
                        if (confirmarBorrado) "Se borrará todo. Toca otra vez para confirmar" else "Borrar todos mis datos",
                        { if (confirmarBorrado) { vm.borrarTodo(); onCerrar() } else confirmarBorrado = true },
                        Modifier.fillMaxWidth(), peligro = true,
                    )
                    Text("Sencillo ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = p.tenue, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }

    dialogoClave?.let { modo ->
        DialogoClave(
            confirmar = modo == "exportar",
            onCancelar = { dialogoClave = null; bytesImportar = null },
            onAceptar = { clave ->
                dialogoClave = null
                if (modo == "exportar") {
                    claveExportar = clave
                    crearArchivo.launch("sencillo-respaldo-${LocalDate.now()}.sencillo")
                } else {
                    val bytes = bytesImportar
                    bytesImportar = null
                    if (bytes != null) {
                        trabajando = true
                        scope.launch {
                            try {
                                vm.restaurar(bytes, clave)
                                avisar("Respaldo restaurado")
                                onCerrar()
                            } catch (ex: RespaldoInvalido) {
                                avisar(ex.message ?: "No se pudo restaurar el respaldo.")
                            } catch (ex: Exception) {
                                avisar("No se pudo restaurar el respaldo.")
                            } finally {
                                clave.fill(' ')
                                trabajando = false
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun Seccion(titulo: String, accion: String? = null, onAccion: () -> Unit = {}, contenido: @Composable () -> Unit) {
    EncabezadoSeccion(titulo, accion = accion, onAccion = onAccion)
    Tarjeta {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { contenido() }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    teclado: KeyboardType,
    ayuda: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        supportingText = ayuda?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    )
}

@Composable
private fun Interruptor(titulo: String, detalle: String, activo: Boolean, habilitado: Boolean = true, onCambio: (Boolean) -> Unit) {
    val p = Tema.p
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, color = if (habilitado) p.texto else p.tenue)
            Text(detalle, fontSize = 13.sp, color = p.texto2)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = activo, onCheckedChange = onCambio, enabled = habilitado,
            colors = SwitchDefaults.colors(checkedTrackColor = p.marca, checkedThumbColor = p.sobreMarca),
        )
    }
}

@Composable
private fun DialogoClave(confirmar: Boolean, onCancelar: () -> Unit, onAceptar: (CharArray) -> Unit) {
    val p = Tema.p
    var clave by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (confirmar) "Contraseña del respaldo" else "Abrir respaldo", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (confirmar) "Usa al menos 8 caracteres y guárdala en un lugar seguro. Si la pierdes, el respaldo no se puede abrir."
                    else "Escribe la contraseña con la que guardaste el respaldo. Restaurar reemplaza todo lo que hay ahora en la app.",
                    fontSize = 14.sp, color = p.texto2,
                )
                OutlinedTextField(clave, { clave = it; error = null }, label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                    shape = RoundedCornerShape(10.dp))
                if (confirmar) OutlinedTextField(repetir, { repetir = it; error = null }, label = { Text("Repite la contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                    shape = RoundedCornerShape(10.dp))
                error?.let { Text(it, color = p.alertaTexto, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    confirmar && clave.length < 8 -> "La contraseña necesita al menos 8 caracteres."
                    confirmar && clave != repetir -> "Las contraseñas no coinciden."
                    clave.isEmpty() -> "Escribe la contraseña."
                    else -> null
                }
                if (error == null) {
                    val c = clave.toCharArray()
                    clave = ""; repetir = ""
                    onAceptar(c)
                }
            }) { Text(if (confirmar) "Elegir dónde guardar" else "Restaurar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        containerColor = p.carta,
    )
}
