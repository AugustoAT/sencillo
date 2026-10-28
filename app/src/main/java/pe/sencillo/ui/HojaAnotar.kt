package pe.sencillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.MetaConAhorro
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.fecha
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Opcion
import pe.sencillo.dominio.Tipo
import pe.sencillo.seguridad.Respaldo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Lo que se está escribiendo en la hoja de anotar. */
data class Borrador(
    val id: Long = 0,
    val tipo: String = Tipo.GASTO,
    val monto: String = "",
    val categoria: String? = null,
    val pago: String = "yape",
    val nota: String = "",
    val dia: LocalDate = LocalDate.now(),
) {
    companion object {
        fun de(m: Movimiento) = Borrador(m.id, m.tipo, Dinero.aTexto(m.centimos), m.categoria, m.pago ?: "yape", m.nota, m.fecha())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAnotar(
    inicial: Borrador,
    metas: List<MetaConAhorro>,
    onGuardar: (Movimiento) -> Unit,
    onBorrar: (Long) -> Unit,
    onCerrar: () -> Unit,
) {
    val p = Tema.p
    var b by remember { mutableStateOf(inicial) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var elegirFecha by remember { mutableStateOf(false) }
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun tecla(k: String) {
        var t = b.monto
        when {
            k == "⌫" -> t = t.dropLast(1)
            k == "." -> if (!t.contains('.')) t = (t.ifEmpty { "0" }) + "."
            else -> {
                if (t.contains('.') && t.substringAfter('.').length >= 2) return
                if (!t.contains('.') && t.trimStart('0').length >= 7) return
                t = if (t == "0") k else t + k
            }
        }
        b = b.copy(monto = t); error = null
    }

    fun guardar() {
        val c = Dinero.deTexto(b.monto)
        when {
            c == null || c <= 0 -> error = "Escribe un monto mayor a cero."
            b.categoria == null -> error = when (b.tipo) {
                Tipo.GASTO -> "Elige en qué gastaste."
                Tipo.INGRESO -> "Elige de dónde vino."
                else -> "Elige a qué meta va."
            }
            else -> onGuardar(
                Movimiento(
                    id = b.id, tipo = b.tipo, centimos = c, categoria = b.categoria!!,
                    nota = Respaldo.texto(b.nota, 60), dia = b.dia.toEpochDay(),
                    pago = if (b.tipo == Tipo.GASTO) b.pago else null,
                ),
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estadoHoja, containerColor = p.carta) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (b.id == 0L) "Anotar" else "Editar movimiento", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCerrar) { Icon(Iconos.cerrar, "Cerrar", tint = p.texto2) }
            }
            // Tipo
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.papel).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf(Tipo.GASTO to "Gasto", Tipo.INGRESO to "Ingreso", Tipo.AHORRO to "Ahorro").forEach { (k, n) ->
                    val sel = b.tipo == k
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) p.carta else p.papel)
                            .clickable {
                                if (!sel) b = b.copy(
                                    tipo = k,
                                    categoria = when (k) {
                                        Tipo.INGRESO -> "cachuelo"
                                        Tipo.AHORRO -> metas.firstOrNull()?.let { "${Catalogo.PREFIJO_META}${it.meta.id}" } ?: Catalogo.AHORRO_LIBRE
                                        else -> null
                                    },
                                )
                                error = null
                            }
                            .semantics { contentDescription = n + if (sel) ", elegido" else "" }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(n, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (sel) p.texto else p.texto2) }
                }
            }
            // Monto
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 0.44.em, color = p.tenue, baselineShift = BaselineShift(0.55f))) { append("S/ ") }
                    append(b.monto.ifEmpty { "0" })
                },
                fontFamily = Fuentes.billete, fontSize = 46.sp, letterSpacing = (-0.03).em,
                color = if (b.monto.isEmpty()) p.tenue else p.texto,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
            // Categoría
            Column {
                Etiqueta(when (b.tipo) { Tipo.GASTO -> "¿En qué?"; Tipo.INGRESO -> "¿De dónde?"; else -> "¿Para qué meta?" })
                val opciones: List<Pair<Opcion, androidx.compose.ui.graphics.Color?>> = when (b.tipo) {
                    Tipo.GASTO -> Catalogo.gastos.map { Opcion(it.id, it.nombre) to p.colorGrupo(it.grupo) }
                    Tipo.INGRESO -> Catalogo.ingresos.map { it to null }
                    else -> metas.map { Opcion("${Catalogo.PREFIJO_META}${it.meta.id}", it.meta.nombre) to p.aho } +
                        (Opcion(Catalogo.AHORRO_LIBRE, "Ahorro libre") to p.aho)
                }
                Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(opciones.filterIndexed { i, _ -> i % 2 == 0 }, opciones.filterIndexed { i, _ -> i % 2 == 1 }).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            fila.forEach { (o, color) ->
                                Ficha(o.nombre, b.categoria == o.id, onClick = { b = b.copy(categoria = o.id); error = null }, punto = color)
                            }
                        }
                    }
                }
            }
            if (b.tipo == Tipo.GASTO) Column {
                Etiqueta("¿Cómo pagaste?")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalogo.pagos.forEach { o -> Ficha(o.nombre, b.pago == o.id, onClick = { b = b.copy(pago = o.id) }) }
                }
            }
            // Fecha
            Column {
                Etiqueta("¿Cuándo?")
                val hoy = LocalDate.now()
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ficha("Hoy", b.dia == hoy, onClick = { b = b.copy(dia = hoy) })
                    Ficha("Ayer", b.dia == hoy.minusDays(1), onClick = { b = b.copy(dia = hoy.minusDays(1)) })
                    val otra = b.dia != hoy && b.dia != hoy.minusDays(1)
                    Ficha(if (otra) diaCorto(b.dia) else "Otra fecha", otra, onClick = { elegirFecha = true })
                }
            }
            Teclado(::tecla)
            OutlinedTextField(
                value = b.nota,
                onValueChange = { if (it.length <= 60) b = b.copy(nota = it) },
                placeholder = { Text("Nota (opcional): menú, pasaje…", color = p.tenue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
        // Siempre visible: guardar es la acción principal de la hoja.
        Column(
            Modifier.fillMaxWidth().background(p.carta).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            error?.let { Text(it, color = p.alertaTexto, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            BotonPrincipal(
                when (b.tipo) { Tipo.GASTO -> "Guardar gasto"; Tipo.INGRESO -> "Guardar ingreso"; else -> "Guardar ahorro" },
                ::guardar,
                Modifier.fillMaxWidth(),
            )
            if (b.id != 0L) BotonSecundario(
                if (confirmarBorrado) "Toca otra vez para borrar" else "Borrar movimiento",
                { if (confirmarBorrado) onBorrar(b.id) else confirmarBorrado = true },
                Modifier.fillMaxWidth(),
                peligro = true,
            )
        }
        }
    }

    if (elegirFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = b.dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { elegirFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        b = b.copy(dia = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    elegirFecha = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { elegirFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }
}

@Composable
fun Etiqueta(texto: String) {
    Text(texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Tema.p.tenue, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Teclado(onTecla: (String) -> Unit) {
    val p = Tema.p
    val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        teclas.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.papel)
                            .clickable { onTecla(k) }
                            .semantics { contentDescription = if (k == "⌫") "Borrar dígito" else if (k == ".") "punto decimal" else k },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (k == "⌫") Icon(Iconos.retroceso, null, tint = p.texto, modifier = Modifier.size(22.dp))
                        else Text(k, fontFamily = Fuentes.cifra, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}
