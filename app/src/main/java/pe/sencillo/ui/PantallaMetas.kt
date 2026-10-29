package pe.sencillo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.Meta
import pe.sencillo.datos.MetaConAhorro
import pe.sencillo.dominio.Analizador
import pe.sencillo.dominio.Dinero
import pe.sencillo.seguridad.Respaldo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Composable
fun PantallaMetas(
    e: EstadoUi.Listo,
    onAportar: (Long) -> Unit,
    onGuardar: (Meta) -> Unit,
    onBorrar: (Long) -> Unit,
) {
    val p = Tema.p
    var editando by remember { mutableStateOf<Meta?>(null) }
    val tieneFondo = e.metas.any { it.meta.nombre.contains("emergencia", ignoreCase = true) }
    val aporteMensual = if (e.analisis.ahorrado > 0) e.analisis.ahorrado else e.perfil.sueldo * e.perfil.ahorroPct / 100
    val fondo = Analizador.fondoSugerido(e.sobres, e.perfil.sueldo)

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Metas de ahorro", style = MaterialTheme.typography.titleLarge)
                    Text("Para qué estás juntando plata. Cuando anotes un ahorro, elige la meta.", fontSize = 13.5.sp, color = p.texto2, modifier = Modifier.padding(top = 2.dp))
                }
                Enlace("Nueva meta", { editando = Meta(nombre = "", objetivo = 0, inicial = 0, limite = null) }, Modifier.padding(start = 8.dp))
            }
            Separador()
        }
        items(e.metas, key = { it.meta.id }) { m ->
            Alcancia(m, aporteMensual / maxOf(1, e.metas.size), onAportar = { onAportar(m.meta.id) }, onEditar = { editando = m.meta })
        }
        if (!tieneFondo) item {
            Column {
                Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Te sugerimos un fondo de emergencia", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Lo recomendable es tener unos 3 meses de gastos básicos guardados: alrededor de ${Dinero.sol0(fondo)} según tus límites. Te cubre si pierdes la chamba o sale un imprevisto.",
                        fontSize = 14.sp, color = p.texto2,
                    )
                    BotonPrincipal("Crear esta meta", { onGuardar(Meta(nombre = "Fondo de emergencia", objetivo = fondo, inicial = 0, limite = null)) })
                }
                Separador()
            }
        }
        if (e.metas.isEmpty() && tieneFondo) item { Vacio("Sin metas", "Crea una para empezar a juntar.") }
    }

    editando?.let { m ->
        DialogoMeta(m, onGuardar = { onGuardar(it); editando = null }, onBorrar = { onBorrar(it); editando = null }, onCerrar = { editando = null })
    }
}

@Composable
private fun Alcancia(m: MetaConAhorro, aporteSugerido: Long, onAportar: () -> Unit, onEditar: () -> Unit) {
    val p = Tema.p
    val falta = maxOf(0L, m.meta.objetivo - m.ahorrado)
    val fr = if (m.meta.objetivo > 0) m.ahorrado.toFloat() / m.meta.objetivo else 0f
    val plan = when {
        falta <= 0 -> "¡Meta cumplida! Puedes subir el objetivo o crear otra."
        m.meta.limite != null -> {
            val f = LocalDate.ofEpochDay(m.meta.limite)
            val meses = maxOf(1L, ChronoUnit.MONTHS.between(LocalDate.now().withDayOfMonth(1), f.withDayOfMonth(1)))
            "Te faltan ${Dinero.sol0(falta)}. Para llegar al ${f.dayOfMonth} de ${MESES[f.monthValue - 1]} de ${f.year} necesitas ${Dinero.sol0(falta / meses)} al mes."
        }
        else -> {
            val aporte = maxOf(5000L, aporteSugerido)
            "Te faltan ${Dinero.sol0(falta)}. Con ${Dinero.sol0(aporte)} al mes llegas en ${(falta + aporte - 1) / aporte} meses."
        }
    }
    Column {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(m.meta.nombre, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${Dinero.sol0(m.ahorrado)} de ${Dinero.sol0(m.meta.objetivo)}", fontSize = 15.sp, fontWeight = FontWeight.Medium, style = estiloMonto)
            }
            Medidor(fr, p.aho, p.aho.copy(alpha = .18f), alto = 10.dp)
            Text(plan, fontSize = 14.sp, color = p.texto2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPrincipal("Anotar un ahorro", onAportar)
                BotonSecundario("Editar", onEditar)
            }
        }
        Separador()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoMeta(meta: Meta, onGuardar: (Meta) -> Unit, onBorrar: (Long) -> Unit, onCerrar: () -> Unit) {
    val p = Tema.p
    var nombre by remember { mutableStateOf(meta.nombre) }
    var objetivo by remember { mutableStateOf(if (meta.objetivo > 0) Dinero.aTexto(meta.objetivo) else "") }
    var inicial by remember { mutableStateOf(Dinero.aTexto(meta.inicial)) }
    var limite by remember { mutableStateOf(meta.limite?.let { LocalDate.ofEpochDay(it) }) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    var elegirFecha by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (meta.id == 0L) "Nueva meta" else "Editar meta", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(nombre, { if (it.length <= 40) nombre = it }, label = { Text("Nombre") },
                    placeholder = { Text("Laptop, viaje, fondo de emergencia") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(objetivo, { objetivo = it.filter { c -> c.isDigit() || c == '.' }.take(10) }, label = { Text("Objetivo S/") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(inicial, { inicial = it.filter { c -> c.isDigit() || c == '.' }.take(10) }, label = { Text("Ya tengo S/") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(limite?.let { "Para el ${diaCorto(it)} de ${it.year}" } ?: "Sin fecha límite", fontSize = 14.sp, color = p.texto2, modifier = Modifier.weight(1f))
                    TextButton(onClick = { elegirFecha = true }) { Text(if (limite == null) "Poner fecha" else "Cambiar") }
                    if (limite != null) TextButton(onClick = { limite = null }) { Text("Quitar") }
                }
                error?.let { Text(it, color = p.alertaTexto, fontSize = 13.sp) }
                if (meta.id != 0L) {
                    Spacer(Modifier.height(4.dp))
                    BotonSecundario(
                        if (confirmar) "Toca otra vez para borrar" else "Borrar meta",
                        { if (confirmar) onBorrar(meta.id) else confirmar = true },
                        Modifier.fillMaxWidth(), peligro = true,
                    )
                    Text("Lo que aportaste a esta meta pasa a «Ahorro libre».", fontSize = 12.sp, color = p.tenue)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val obj = Dinero.deTexto(objetivo)
                val ini = Dinero.deTexto(inicial.ifBlank { "0" })
                val n = Respaldo.texto(nombre, 40)
                if (n.isBlank() || obj == null || obj <= 0 || ini == null) error = "Ponle un nombre y un objetivo mayor a cero."
                else onGuardar(meta.copy(nombre = n, objetivo = obj, inicial = ini, limite = limite?.toEpochDay()))
            }) { Text("Guardar meta", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        containerColor = p.carta,
    )

    if (elegirFecha) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = (limite ?: LocalDate.now().plusMonths(6)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { elegirFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { limite = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    elegirFecha = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { elegirFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }
}
