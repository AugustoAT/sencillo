package pe.sencillo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.dominio.Analisis
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Dinero
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PantallaAnalisis(e: EstadoUi.Listo, lista: LazyListState) {
    val p = Tema.p
    val a = e.analisis
    val ingreso = maxOf(1L, a.ingreso)
    LazyColumn(state = lista, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cifra("Ingresos", Dinero.sol0(a.ingreso), if (a.extra > 0) "incluye ${Dinero.sol0(a.extra)} extra" else "sueldo neto", Modifier.weight(1f))
                    Cifra("Gastos", Dinero.sol0(a.gastado), "${Dinero.pct(a.gastado.toDouble() / ingreso)} de lo que entró", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cifra("Ahorrado", Dinero.sol0(a.ahorrado), "meta: ${Dinero.sol0(a.ingreso * a.ahorroPct / 100)}", Modifier.weight(1f))
                    Cifra("Gasto diario promedio", Dinero.sol0(if (a.dia > 0) a.variables / a.dia else 0), "sin contar fijos", Modifier.weight(1f))
                }
            }
        }
        item { Panel("Ritmo del mes", "Gasto acumulado frente al límite que te deja ahorrar el ${a.ahorroPct}%") { GraficoRitmo(a) } }
        item { Panel("Regla 50 / ${50 - a.ahorroPct} / ${a.ahorroPct}", "Cómo se reparte lo que entró: necesidades, gustos y ahorro") { Reparto(a) } }
        item {
            Panel("Por categoría", "Barra: lo gastado. Línea vertical: tu sobre" + if (a.porCategoriaAnterior != null) ". Entre paréntesis, el mes anterior." else ".") {
                Categorias(e)
            }
        }
        item { Panel("Cómo pagas", "Tus gastos según el medio de pago") { MediosDePago(a) } }
        item {
            Panel("Recomendaciones", "Calculadas en tu celular con tus gastos de ${nombreMes(a.mes)}") {
                if (e.recomendaciones.isEmpty()) {
                    Text("Anota algunos días de gastos y aquí aparecerán recomendaciones.", color = p.texto2, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                } else Column(Modifier.padding(top = 4.dp)) {
                    e.recomendaciones.forEachIndexed { i, r ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.linea))
                        Row(Modifier.padding(vertical = 12.dp)) {
                            Icon(iconoNivel(r.nivel), null, tint = colorNivel(r.nivel), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(r.titulo, fontWeight = FontWeight.SemiBold)
                                Text(r.detalle, fontSize = 14.sp, color = p.texto2, modifier = Modifier.padding(top = 2.dp))
                                val ah = r.ahorroPosible
                                if (ah != null && ah > 2000) Text("Ahorro posible: unos ${Dinero.sol0(ah)} al mes", fontSize = 12.sp, color = p.bienTexto, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cifra(etiqueta: String, valor: String, nota: String, modifier: Modifier) {
    val p = Tema.p
    Tarjeta(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(etiqueta, fontSize = 12.5.sp, color = p.tenue, maxLines = 1)
            Text(valor, fontFamily = Fuentes.titulo, fontSize = 22.sp, maxLines = 1)
            Text(nota, fontSize = 12.sp, color = p.texto2, maxLines = 1)
        }
    }
}

@Composable
private fun Panel(titulo: String, sub: String, contenido: @Composable () -> Unit) {
    Tarjeta(Modifier.padding(top = 10.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(sub, fontSize = 13.sp, color = Tema.p.tenue)
            contenido()
        }
    }
}

@Composable
private fun Clave(color: Color, texto: String, linea: Boolean = false, punteada: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (linea) Canvas(Modifier.width(16.dp).height(8.dp)) {
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx(),
                pathEffect = if (punteada) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null)
        } else Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 12.5.sp, color = Tema.p.texto2)
    }
}

/** Devuelve (paso, máximo) redondos para el eje, en céntimos. */
private fun escala(maximo: Double): Pair<Long, Long> {
    val crudo = maxOf(maximo / 3, 100.0)
    val mag = 10.0.pow(floor(log10(crudo)))
    val paso = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= crudo }
    return paso.toLong() to (ceil(maximo / paso) * paso).toLong()
}

private fun etiquetaEje(c: Long): String {
    val soles = c / 100
    return if (soles >= 1000) "S/ " + (if (soles % 1000 == 0L) "${soles / 1000}" else String.format(java.util.Locale.US, "%.1f", soles / 1000.0)) + " mil" else "S/ $soles"
}

@Composable
private fun GraficoRitmo(a: Analisis) {
    val p = Tema.p
    if (a.dia == 0) {
        Text("El mes aún no empieza.", color = p.texto2, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        return
    }
    val medidor = rememberTextMeasurer()
    var sel by remember(a.mes) { mutableStateOf<Int?>(null) }
    var verTabla by remember(a.mes) { mutableStateOf(false) }
    val dm = a.diasDelMes
    val dia = a.dia
    val tope = maxOf(a.limite, if (a.esActual) a.proyeccion else 0L, a.acumulado[dia], 100L)
    val (paso, maximo) = escala(tope * 1.04)

    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Clave(p.marca, "Gastado", linea = true)
        if (a.esActual) Clave(p.marca.copy(alpha = .55f), "Proyección", linea = true, punteada = true)
        Clave(p.tenue, "Ritmo ideal", linea = true)
    }
    val ideal = { d: Int -> a.limite * d / dm }
    val info = sel?.let { d ->
        if (d <= dia) "Día $d: llevabas ${Dinero.sol0(a.acumulado[d])}, ese día ${Dinero.sol0(a.acumulado[d] - a.acumulado[d - 1])}. Ritmo ideal ${Dinero.sol0(ideal(d))}."
        else "Día $d: ritmo ideal ${Dinero.sol0(ideal(d))}" + if (a.esActual) ", proyección ${Dinero.sol0(a.acumulado[dia] + (a.proyeccion - a.acumulado[dia]) * (d - dia) / maxOf(1, dm - dia))}." else "."
    } ?: if (a.esActual) "Si sigues así, cierras el mes con ${Dinero.sol0(a.proyeccion)} gastados. Tu límite es ${Dinero.sol0(a.limite)}."
    else "Cerraste con ${Dinero.sol0(a.gastado)} gastados. Tu límite era ${Dinero.sol0(a.limite)}."
    Text(info, fontSize = 13.5.sp, color = p.texto2, modifier = Modifier.padding(top = 8.dp).height(40.dp))

    val izq = 52.dp
    val der = 10.dp
    val estiloEje = TextStyle(fontFamily = Fuentes.texto, fontSize = 11.sp, color = p.tenue)
    val estiloFuerte = TextStyle(fontFamily = Fuentes.texto, fontSize = 12.sp, color = p.texto, fontWeight = FontWeight.SemiBold)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(196.dp)
            .semantics { contentDescription = "Gráfico del gasto acumulado: ${Dinero.sol0(a.acumulado[dia])} al día $dia. Toca o desliza para ver cada día." }
            .pointerInput(a.mes, dm) {
                val diaEn = { x: Float -> (((x - izq.toPx()) / (size.width - izq.toPx() - der.toPx())) * dm).roundToInt().coerceIn(1, dm) }
                detectTapGestures { sel = diaEn(it.x) }
            }
            .pointerInput(a.mes, dm) {
                val diaEn = { x: Float -> (((x - izq.toPx()) / (size.width - izq.toPx() - der.toPx())) * dm).roundToInt().coerceIn(1, dm) }
                detectHorizontalDragGestures { cambio, _ -> sel = diaEn(cambio.position.x) }
            },
    ) {
        val l = izq.toPx(); val r = der.toPx(); val t = 16.dp.toPx(); val b = 22.dp.toPx()
        val iw = size.width - l - r
        val ih = size.height - t - b
        fun x(d: Int) = l + d.toFloat() / dm * iw
        fun y(v: Long) = t + ih - v.toFloat() / maximo * ih

        // rejilla y eje Y
        var v = 0L
        while (v <= maximo) {
            drawLine(if (v == 0L) p.linea2 else p.linea, Offset(l, y(v)), Offset(size.width - r, y(v)), strokeWidth = 1.dp.toPx())
            texto(medidor, etiquetaEje(v), Offset(l - 8.dp.toPx(), y(v)), estiloEje, alinear = 1f)
            v += paso
        }
        listOf(1, 10, 20, dm).distinct().forEach { d -> texto(medidor, "$d", Offset(x(d), size.height - b / 2 + 2.dp.toPx()), estiloEje, alinear = .5f) }

        // ritmo ideal
        drawLine(p.tenue, Offset(x(0), y(0)), Offset(x(dm), y(a.limite)), strokeWidth = 1.5.dp.toPx())
        texto(medidor, "límite ${Dinero.sol0(a.limite)}", Offset(size.width - r, y(a.limite) - 9.dp.toPx()), estiloEje, alinear = 1f)

        // gastado: área + línea
        val linea = Path().apply {
            moveTo(x(0), y(0))
            for (d in 1..dia) lineTo(x(d), y(a.acumulado[d]))
        }
        val area = Path().apply { addPath(linea); lineTo(x(dia), y(0)); lineTo(x(0), y(0)); close() }
        drawPath(area, p.marca.copy(alpha = .10f))
        drawPath(linea, p.marca, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        if (a.esActual && dia < dm) {
            drawLine(p.marca.copy(alpha = .55f), Offset(x(dia), y(a.acumulado[dia])), Offset(x(dm), y(a.proyeccion)),
                strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        }
        // punto final con anillo del color de la superficie
        val fin = Offset(x(dia), y(a.acumulado[dia]))
        drawCircle(p.carta, 7.dp.toPx(), fin)
        drawCircle(p.marca, 5.dp.toPx(), fin)
        texto(medidor, Dinero.sol0(a.acumulado[dia]), fin + Offset(if (fin.x > size.width - 90.dp.toPx()) -10.dp.toPx() else 10.dp.toPx(), -12.dp.toPx()),
            estiloFuerte, alinear = if (fin.x > size.width - 90.dp.toPx()) 1f else 0f)

        // selección
        sel?.let { d ->
            drawLine(p.linea2, Offset(x(d), t), Offset(x(d), t + ih), strokeWidth = 1.dp.toPx())
            if (d <= dia) {
                drawCircle(p.carta, 6.dp.toPx(), Offset(x(d), y(a.acumulado[d])))
                drawCircle(p.marca, 4.5.dp.toPx(), Offset(x(d), y(a.acumulado[d])))
            }
        }
    }
    val dias = (1..dia).filter { a.acumulado[it] != a.acumulado[it - 1] }
    if (dias.isNotEmpty()) {
        Text(
            if (verTabla) "Ocultar tabla" else "Ver como tabla",
            color = p.marca, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(6.dp)).clickable { verTabla = !verTabla }.padding(4.dp),
        )
        if (verTabla) Column(Modifier.padding(top = 4.dp)) {
            FilaTabla("Día", "Ese día", "Acumulado", encabezado = true)
            dias.forEach { d -> FilaTabla("$d", Dinero.sol(a.acumulado[d] - a.acumulado[d - 1]), Dinero.sol(a.acumulado[d])) }
        }
    }
}

@Composable
private fun FilaTabla(a: String, b: String, c: String, encabezado: Boolean = false) {
    val p = Tema.p
    val color = if (encabezado) p.tenue else p.texto
    val peso = if (encabezado) FontWeight.SemiBold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(a, Modifier.weight(.6f), color = color, fontWeight = peso, fontSize = 13.sp)
        Text(b, Modifier.weight(1f), color = color, fontWeight = peso, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text(c, Modifier.weight(1f), color = color, fontWeight = peso, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(p.linea))
}

/** Dibuja texto centrado verticalmente en `ancla`; `alinear` 0 = izquierda, .5 = centro, 1 = derecha. */
private fun DrawScope.texto(m: TextMeasurer, s: String, ancla: Offset, estilo: TextStyle, alinear: Float) {
    val r = m.measure(s, estilo)
    drawText(r, topLeft = Offset(ancla.x - r.size.width * alinear, ancla.y - r.size.height / 2f))
}

@Composable
private fun Reparto(a: Analisis) {
    val p = Tema.p
    val ingreso = maxOf(1L, a.ingreso)
    val meta = a.ahorroPct / 100.0
    data class Seg(val nombre: String, val valor: Long, val meta: Double, val color: Color, val esAhorro: Boolean)
    val segs = listOf(
        Seg("Necesidades", a.necesidades, .5, p.nec, false),
        Seg("Gustos", a.gustos, maxOf(0.0, .5 - meta), p.gus, false),
        Seg("Ahorro", a.ahorrado, meta, p.aho, true),
    )
    val libre = maxOf(0L, ingreso - a.necesidades - a.gustos - a.ahorrado)
    val total = maxOf(ingreso, a.necesidades + a.gustos + a.ahorrado).toFloat()
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().height(22.dp).clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segs.filter { it.valor > 0 }.forEach { s -> Box(Modifier.weight(s.valor / total).fillMaxHeight().background(s.color)) }
        if (libre > 0) Box(Modifier.weight(libre / total).fillMaxHeight().background(p.pista))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp)) {
        val ancho = maxWidth
        listOf(.5, 1 - meta).forEach { m ->
            val pos = ancho * (m * ingreso / total).toFloat()
            Box(Modifier.offset(x = pos - 0.75.dp, y = (-24).dp).width(1.5.dp).height(26.dp).background(p.texto.copy(alpha = .55f)))
            Text("${(m * 100).roundToInt()}%", fontSize = 11.sp, color = p.tenue, modifier = Modifier.offset(x = pos - 12.dp, y = 4.dp))
        }
    }
    segs.forEach { s ->
        val r = s.valor.toDouble() / ingreso
        val ok = if (s.esAhorro) r >= s.meta else r <= s.meta
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.linea))
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(s.color))
            Spacer(Modifier.width(10.dp))
            Text(s.nombre, Modifier.weight(1f))
            Text("${Dinero.sol0(s.valor)}, ${Dinero.pct(r)}", fontFamily = Fuentes.cifra, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Text("meta ${Dinero.pct(s.meta)}", fontSize = 12.sp, color = p.tenue)
            Spacer(Modifier.width(6.dp))
            Icon(if (ok) Iconos.bien else Iconos.alerta, if (ok) "dentro de la meta" else "fuera de la meta",
                tint = if (ok) p.bien else p.alerta, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun Categorias(e: EstadoUi.Listo) {
    val p = Tema.p
    val a = e.analisis
    val filas = Catalogo.gastos
        .map { c -> Triple(c, a.porCategoria[c.id] ?: 0L, e.sobres[c.id] ?: 0L) }
        .filter { it.second > 0 || it.third > 0 }
        .sortedByDescending { it.second }
    val escala = maxOf(1L, filas.maxOfOrNull { maxOf(it.second, it.third) } ?: 1L).toFloat()
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Clave(p.nec, "Necesidad")
        Clave(p.gus, "Gusto")
    }
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        filas.forEach { (c, gastado, sobre) ->
            val anterior = a.porCategoriaAnterior?.get(c.id)
            Column {
                Row {
                    Text(c.nombre, Modifier.weight(1f), fontSize = 14.sp)
                    Text(Dinero.sol0(gastado), fontFamily = Fuentes.cifra, fontSize = 13.sp)
                    if (anterior != null) Text(" (${Dinero.sol0(anterior)})", fontSize = 13.sp, color = p.tenue)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 5.dp).height(12.dp)) {
                    val ancho = maxWidth
                    Box(
                        Modifier.width(ancho * (gastado / escala)).fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(p.colorGrupo(c.grupo)),
                    )
                    if (sobre > 0) Box(Modifier.offset(x = ancho * (sobre / escala) - 1.dp, y = (-3).dp).width(2.dp).height(18.dp).background(p.texto.copy(alpha = .6f)))
                }
                if (sobre in 1 until gastado) Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Iconos.alerta, null, tint = p.alerta, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("se pasó ${Dinero.sol0(gastado - sobre)} de su sobre", fontSize = 12.sp, color = p.alertaTexto, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MediosDePago(a: Analisis) {
    val p = Tema.p
    val max = maxOf(1L, a.porPago.values.maxOrNull() ?: 1L).toFloat()
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Catalogo.pagos.forEach { pago ->
            val v = a.porPago[pago.id] ?: 0L
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pago.nombre, Modifier.width(96.dp), fontSize = 14.sp)
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(p.pista)) {
                    Box(Modifier.fillMaxWidth(v / max).fillMaxHeight().background(p.marca))
                }
                Spacer(Modifier.width(10.dp))
                Text(Dinero.sol0(v), fontFamily = Fuentes.cifra, fontSize = 13.sp)
            }
        }
    }
}
