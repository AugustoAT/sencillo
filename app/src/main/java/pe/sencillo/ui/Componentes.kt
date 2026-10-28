package pe.sencillo.ui

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
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
import pe.sencillo.dominio.Grupo
import pe.sencillo.dominio.Nivel
import pe.sencillo.dominio.Tipo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Paleta
import pe.sencillo.ui.tema.Tema
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------- formatos ----------
val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre")
private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")
private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

fun nombreMes(m: YearMonth) = MESES[m.monthValue - 1]
fun diaCorto(d: LocalDate) = "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MESES_CORTOS[d.monthValue - 1]}"

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

// ---------- billete ----------
@Composable
fun TarjetaBillete(a: Analisis, modifier: Modifier = Modifier) {
    val p = Tema.p
    val disp = a.disponible
    // El monto se achica si es largo, para que entre en una línea en celulares angostos.
    val tamMonto = when (Dinero.parteEntera(disp).length) { in 0..5 -> 52.sp; 6, 7 -> 42.sp; else -> 34.sp }
    val fraccionGastado = if (a.ingreso > 0) min(1f, a.gastado.toFloat() / a.ingreso) else 0f
    val fraccionAhorrado = if (a.ingreso > 0) min(1f - fraccionGastado, a.ahorrado.toFloat() / a.ingreso) else 0f
    val stats = if (a.esActual) listOf(
        "Por día puedes" to Dinero.sol0(a.porDia),
        "Quedan" to "${a.diasRestantes} día${if (a.diasRestantes == 1) "" else "s"}",
        "Ahorrado" to Dinero.pct(a.tasaAhorro),
    ) else listOf(
        "Gastado" to Dinero.sol0(a.gastado),
        "Ahorrado" to Dinero.sol0(a.ahorrado),
        "Tasa de ahorro" to Dinero.pct(a.tasaAhorro),
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(p.billete, p.billete2)))
            .guilloche(p.billeteLinea),
    ) {
        Column(Modifier.padding(top = 18.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (disp < 0) "Te pasaste en ${nombreMes(a.mes)}" else "Te queda en ${nombreMes(a.mes)}",
                    color = p.billeteTexto, style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.weight(1f))
                Text("Serie ${a.mes.year}-${a.mes.monthValue.toString().padStart(2, '0')}", color = p.billeteTexto.copy(alpha = .65f), fontSize = 11.sp)
            }
            Text(
                buildAnnotatedString {
                    val chico = SpanStyle(fontSize = 0.42.em, baselineShift = BaselineShift(0.62f))
                    withStyle(chico) { append(if (disp < 0) "−S/ " else "S/ ") }
                    append(Dinero.parteEntera(disp))
                    withStyle(chico) { append("." + Dinero.parteCentimos(disp)) }
                },
                fontFamily = Fuentes.billete,
                fontSize = tamMonto,
                lineHeight = tamMonto * 1.05f,
                letterSpacing = (-0.03).em,
                color = if (disp < 0) p.alertaTexto else p.billeteTexto,
                maxLines = 1,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
            )
            Text(
                "de ${Dinero.sol(a.ingreso)} que entraron" + if (a.extra > 0) " (con ${Dinero.sol0(a.extra)} extra)" else "",
                color = p.billeteTexto.copy(alpha = .85f), fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Row(
                Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.billeteTexto.copy(alpha = .12f))
                    .semantics { contentDescription = "${Dinero.pct(fraccionGastado.toDouble())} gastado, ${Dinero.pct(fraccionAhorrado.toDouble())} ahorrado" },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (fraccionGastado > 0f) Box(Modifier.fillMaxHeight().weight(fraccionGastado).background(p.billeteTexto))
                if (fraccionAhorrado > 0f) Box(Modifier.fillMaxHeight().weight(fraccionAhorrado).background(p.sol))
                val resto = 1f - fraccionGastado - fraccionAhorrado
                if (resto > 0.001f) Spacer(Modifier.weight(resto))
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.billeteTexto.copy(alpha = .14f)))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                stats.forEachIndexed { i, (k, v) ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(p.billeteTexto.copy(alpha = .14f)))
                    Column(Modifier.weight(1f).padding(start = if (i == 0) 18.dp else 12.dp, top = 11.dp, bottom = 13.dp, end = 6.dp)) {
                        Text(k, color = p.billeteTexto.copy(alpha = .75f), fontSize = 12.sp, maxLines = 1)
                        Text(v, color = p.billeteTexto, fontFamily = Fuentes.cifra, fontSize = 17.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Rosetón y ondas de seguridad como los de un billete, más una línea de microtexto. */
private fun Modifier.guilloche(color: Color) = drawWithCache {
    val w = size.width
    val h = size.height
    val cx = w - min(110.dp.toPx(), w * .24f)
    val cy = h * .36f
    val r = min(h * .40f, 96.dp.toPx())
    val rosetas = (0 until 22).map { k ->
        Path().apply {
            for (i in 0..720) {
                val t = i / 720.0 * 2 * PI
                val rr = r * (0.58 + 0.2 * sin(9 * t + k * 0.29) + 0.07 * cos(17 * t - k * 0.13)) * (0.9 + k * 0.006)
                val x = (cx + rr * cos(t)).toFloat()
                val y = (cy + rr * sin(t)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
    val ondas = (0 until 9).map { k ->
        Path().apply {
            var x = 0f
            while (x <= w) {
                val y = (h * .62f + 7.dp.toPx() * sin(x / 15.dp.toPx() + k * 0.7) * sin(x / 90.dp.toPx()) + k * 2.2f.dp.toPx()).toFloat()
                if (x == 0f) moveTo(x, y) else lineTo(x, y)
                x += 2f
            }
        }
    }
    val trazo = Stroke(width = 0.7.dp.toPx())
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = 6.5.dp.toPx()
        typeface = Typeface.MONOSPACE
    }
    val micro = "AHORRO+  LO QUE APARTAS PRIMERO NO SE GASTA  ".repeat(10)
    onDrawBehind {
        rosetas.forEach { drawPath(it, color, style = trazo) }
        ondas.forEach { drawPath(it, color, style = trazo) }
        drawContext.canvas.nativeCanvas.drawText(micro, 6.dp.toPx(), h - 4.dp.toPx(), pincel)
    }
}

// ---------- piezas pequeñas ----------
@Composable
fun Pildora(nivel: Nivel, texto: String) {
    val p = Tema.p
    Row(
        Modifier
            .clip(CircleShape)
            .background(p.papel)
            .border(1.dp, p.linea, CircleShape)
            .padding(start = 7.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconoNivel(nivel), null, tint = colorNivel(nivel), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            .background(if (seleccionada) p.texto else p.carta)
            .border(1.dp, if (seleccionada) p.texto else p.linea, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = texto + if (seleccionada) ", elegido" else "" }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto != null) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(punto))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (seleccionada) p.papel else p.texto2, maxLines = 1)
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

@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val p = Tema.p
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.carta)
            .border(1.dp, p.linea, RoundedCornerShape(14.dp)),
    ) { contenido() }
}

@Composable
fun EncabezadoSeccion(titulo: String, subtitulo: String? = null, accion: String? = null, onAccion: () -> Unit = {}) {
    val p = Tema.p
    Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) Text(subtitulo, fontSize = 13.sp, color = p.tenue)
        }
        if (accion != null) {
            Text(
                accion,
                color = p.marca,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAccion).padding(6.dp),
            )
        }
    }
}

@Composable
fun FilaMovimiento(m: Movimiento, metas: List<MetaConAhorro>, onClick: () -> Unit, monoespaciada: Boolean = false) {
    val p = Tema.p
    val destino = nombreDestino(m, metas)
    val sub = listOfNotNull(destino, if (m.tipo == Tipo.GASTO) Catalogo.nombrePago(m.pago) else null, diaCorto(m.fecha()))
        .joinToString(", ")
    val signo = when (m.tipo) { Tipo.GASTO -> "−"; Tipo.INGRESO -> "+"; else -> "" }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(p.colorMovimiento(m)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.nota.ifBlank { destino }, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, color = p.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (m.tipo == Tipo.AHORRO) "a alcancía " else signo) + Dinero.sol(m.centimos),
            fontFamily = if (monoespaciada) Fuentes.boleta else Fuentes.texto,
            fontSize = if (monoespaciada) 14.sp else 15.sp,
            color = if (m.tipo == Tipo.INGRESO) p.bienTexto else p.texto,
            maxLines = 1,
        )
    }
}

@Composable
fun Vacio(titulo: String, texto: String) {
    val p = Tema.p
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(texto, color = p.tenue, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
