package pe.sencillo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.MetaConAhorro
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.fecha
import pe.sencillo.dominio.Analisis
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Estado
import pe.sencillo.dominio.Grupo
import pe.sencillo.dominio.Nivel
import pe.sencillo.dominio.Tipo
import pe.sencillo.ui.tema.CIFRAS_TABULARES
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Paleta
import pe.sencillo.ui.tema.Tema
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

// ---------- formatos ----------
val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre")
private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")
private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
private val DIAS_LARGOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

fun nombreMes(m: YearMonth) = MESES[m.monthValue - 1]
fun diaCorto(d: LocalDate) = "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MESES_CORTOS[d.monthValue - 1]}"
fun diaLargo(d: LocalDate): String {
    val hoy = LocalDate.now()
    return when (d) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> "${DIAS_LARGOS[d.dayOfWeek.value - 1]} ${d.dayOfMonth}"
    }
}

/**
 * Estilo para montos en listas: el estilo de texto actual (con su color del tema) más cifras del
 * mismo ancho, para que se alineen. Hereda del tema: reemplazarlo dejaría el texto negro en modo oscuro.
 */
val estiloMonto: TextStyle
    @Composable get() = LocalTextStyle.current.copy(fontFeatureSettings = CIFRAS_TABULARES)

// ---------- colores por rol ----------
@Composable
fun colorNivel(n: Nivel): Color = when (n) {
    Nivel.ALERTA -> Tema.p.alerta
    Nivel.OJO -> Tema.p.ojo
    Nivel.BIEN -> Tema.p.bien
}

fun Paleta.colorGrupo(g: Grupo) = if (g == Grupo.NECESIDAD) nec else gus

fun Paleta.colorMovimiento(m: Movimiento): Color = when (m.tipo) {
    Tipo.GASTO -> colorGrupo(Catalogo.categoria(m.categoria)?.grupo ?: Grupo.GUSTO)
    Tipo.AHORRO -> aho
    else -> bien
}

fun iconoNivel(n: Nivel): ImageVector = when (n) {
    Nivel.ALERTA -> Iconos.alerta
    Nivel.OJO -> Iconos.ojo
    Nivel.BIEN -> Iconos.bien
}

fun nombreDestino(m: Movimiento, metas: List<MetaConAhorro>): String = when (m.tipo) {
    Tipo.GASTO -> Catalogo.categoria(m.categoria)?.nombre ?: "Gasto"
    Tipo.INGRESO -> Catalogo.nombreIngreso(m.categoria)
    else -> metas.firstOrNull { "${Catalogo.PREFIJO_META}${it.meta.id}" == m.categoria }?.meta?.nombre ?: "Ahorro libre"
}

// ---------- resumen del mes ----------
/**
 * Lo que te queda del mes, en grande, con el reparto de lo que entró y tres datos clave.
 * Va directo sobre el papel: es el único elemento que levanta la voz en la pantalla.
 */
@Composable
fun ResumenMes(a: Analisis, modifier: Modifier = Modifier) {
    val p = Tema.p
    val disp = a.disponible
    // El monto se achica si es largo, para que entre en una línea en celulares angostos.
    val tamMonto = when (Dinero.parteEntera(disp).length) { in 0..5 -> 54.sp; 6, 7 -> 44.sp; else -> 36.sp }
    val total = maxOf(1L, a.ingreso).toFloat()
    val fGastado = min(1f, a.gastado / total)
    val fAhorrado = min(1f - fGastado, a.ahorrado / total)
    val fLibre = maxOf(0f, 1f - fGastado - fAhorrado)

    Column(modifier.fillMaxWidth()) {
        Text(
            if (disp < 0) "Te pasaste por" else "Te quedan",
            fontSize = 15.sp, color = p.texto2,
        )
        Text(
            buildAnnotatedString {
                val chico = SpanStyle(fontSize = 0.48.em)
                withStyle(chico) { append(if (disp < 0) "−S/ " else "S/ ") }
                append(Dinero.parteEntera(disp))
                withStyle(chico) { append("." + Dinero.parteCentimos(disp)) }
            },
            fontFamily = Fuentes.monto,
            fontSize = tamMonto,
            lineHeight = tamMonto * 1.1f,
            letterSpacing = (-0.02).em,
            color = if (disp < 0) p.alertaTexto else p.texto,
            maxLines = 1,
        )
        Text(
            "de ${Dinero.sol0(a.ingreso)} que entraron en ${nombreMes(a.mes)}" +
                if (a.extra > 0) ", con ${Dinero.sol0(a.extra)} extra" else "",
            fontSize = 14.sp, color = p.texto2,
        )
        Row(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(10.dp)
                .semantics {
                    contentDescription = "Gastado ${Dinero.pct(fGastado.toDouble())}, ahorrado ${Dinero.pct(fAhorrado.toDouble())}, libre ${Dinero.pct(fLibre.toDouble())}"
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (fGastado > 0f) Box(Modifier.fillMaxHeight().weight(fGastado).clip(RoundedCornerShape(3.dp)).background(p.texto))
            if (fAhorrado > 0f) Box(Modifier.fillMaxHeight().weight(fAhorrado).clip(RoundedCornerShape(3.dp)).background(p.aho))
            if (fLibre > 0.001f) Box(Modifier.fillMaxHeight().weight(fLibre).clip(RoundedCornerShape(3.dp)).background(p.pista))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Clave(p.texto, "Gastado ${Dinero.sol0(a.gastado)}")
            Clave(p.aho, "Ahorrado ${Dinero.sol0(a.ahorrado)}")
        }
        Separador(Modifier.padding(top = 18.dp))
        val datos = if (a.esActual) listOf(
            "Puedes gastar" to "${Dinero.sol0(a.porDia)} al día",
            "Faltan" to "${a.diasRestantes} día${if (a.diasRestantes == 1) "" else "s"}",
            "Ahorraste" to Dinero.pct(a.tasaAhorro),
        ) else listOf(
            "Gastaste" to Dinero.sol0(a.gastado),
            "Ahorraste" to Dinero.sol0(a.ahorrado),
            "De lo que entró" to Dinero.pct(a.tasaAhorro),
        )
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            datos.forEachIndexed { i, (k, v) ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp).background(p.linea))
                Column(Modifier.weight(1f).padding(start = if (i == 0) 0.dp else 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
                    Text(k, fontSize = 12.5.sp, color = p.texto2, maxLines = 1)
                    Text(v, fontFamily = Fuentes.cifra, fontSize = 17.sp, maxLines = 1)
                }
            }
        }
        Separador()
    }
}

@Composable
fun Clave(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 13.sp, color = Tema.p.texto2, style = estiloMonto)
    }
}

// ---------- piezas pequeñas ----------
@Composable
fun Separador(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Tema.p.linea))
}

/** Cómo va el mes, en una línea: ícono de estado, título y explicación. */
@Composable
fun AvisoEstado(e: Estado, modifier: Modifier = Modifier) {
    val p = Tema.p
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(iconoNivel(e.nivel), null, tint = colorNivel(e.nivel), modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(e.titulo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(e.detalle, fontSize = 14.sp, color = p.texto2)
        }
    }
}

@Composable
fun Medidor(fraccion: Float, relleno: Color, pista: Color, alto: Dp = 6.dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(alto).clip(RoundedCornerShape(alto / 2)).background(pista)) {
        Box(Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(alto / 2)).background(relleno))
    }
}

@Composable
fun Ficha(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    punto: Color? = null,
) {
    val p = Tema.p
    Row(
        modifier
            .clip(CircleShape)
            .background(if (seleccionada) p.marca else p.carta)
            .border(1.dp, if (seleccionada) p.marca else p.linea2, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = texto + if (seleccionada) ", elegido" else "" }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto != null) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(if (seleccionada) p.sobreMarca else punto))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (seleccionada) p.sobreMarca else p.texto, maxLines = 1)
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val p = Tema.p
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = p.marca, contentColor = p.sobreMarca),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge, color = p.sobreMarca) }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, peligro: Boolean = false) {
    val p = Tema.p
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (peligro) p.alerta.copy(alpha = .5f) else p.linea2),
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = if (peligro) p.alertaTexto else p.texto)
    }
}

/** Enlace de texto para acciones secundarias ("Editar", "Ver todos"). */
@Composable
fun Enlace(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        texto,
        color = Tema.p.marca,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
fun EncabezadoSeccion(
    titulo: String,
    subtitulo: String? = null,
    accion: String? = null,
    modifier: Modifier = Modifier,
    espacioArriba: Dp = 28.dp,
    onAccion: () -> Unit = {},
) {
    val p = Tema.p
    Row(modifier.fillMaxWidth().padding(top = espacioArriba, bottom = 6.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) Text(subtitulo, fontSize = 13.5.sp, color = p.texto2, modifier = Modifier.padding(top = 2.dp))
        }
        if (accion != null) Enlace(accion, onAccion, Modifier.padding(start = 8.dp))
    }
}

@Composable
fun FilaMovimiento(m: Movimiento, metas: List<MetaConAhorro>, onClick: () -> Unit, conFecha: Boolean = true) {
    val p = Tema.p
    val destino = nombreDestino(m, metas)
    val sub = listOfNotNull(
        if (m.nota.isNotBlank()) destino else null,
        if (m.tipo == Tipo.GASTO) Catalogo.nombrePago(m.pago) else null,
        if (conFecha) diaCorto(m.fecha()) else null,
    ).joinToString(", ")
    val (signo, color) = when (m.tipo) {
        Tipo.GASTO -> "−" to p.texto
        Tipo.INGRESO -> "+" to p.bienTexto
        else -> "" to p.texto
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(p.colorMovimiento(m)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.nota.ifBlank { destino }, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, fontSize = 12.5.sp, color = p.texto2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.tipo == Tipo.AHORRO) Text("Ahorro", fontSize = 12.5.sp, color = p.texto2)
        }
        Spacer(Modifier.width(8.dp))
        Text(signo + Dinero.sol(m.centimos), fontSize = 15.sp, color = color, style = estiloMonto, maxLines = 1)
    }
}

@Composable
fun Vacio(titulo: String, texto: String) {
    val p = Tema.p
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(texto, color = p.texto2, fontSize = 14.sp, textAlign = TextAlign.Start)
    }
}

/** Marca del producto, en texto: sin logos dibujados dentro de la app. */
@Composable
fun MarcaTexto(tam: androidx.compose.ui.unit.TextUnit = 20.sp) {
    Text("Ahorro+", fontFamily = Fuentes.titulo, fontSize = tam, color = Tema.p.texto)
}
