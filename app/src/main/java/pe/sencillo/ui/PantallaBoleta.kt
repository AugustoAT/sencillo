package pe.sencillo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.fecha
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Tipo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema

@Composable
fun PantallaBoleta(e: EstadoUi.Listo, onEditar: (Movimiento) -> Unit) {
    val p = Tema.p
    val a = e.analisis
    var filtro by rememberSaveable { mutableStateOf("todo") }
    val movs = a.movimientos
        .filter { filtro == "todo" || it.tipo == filtro }
        .sortedWith(compareByDescending<Movimiento> { it.dia }.thenByDescending { it.id })
    val porDia = movs.groupBy { it.dia }
    val (total, etiqueta) = when (filtro) {
        Tipo.INGRESO -> a.extra to "Total de ingresos extra"
        Tipo.AHORRO -> a.ahorrado to "Total ahorrado"
        else -> a.gastado to "Total gastado"
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)) {
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("todo" to "Todo", Tipo.GASTO to "Gastos", Tipo.INGRESO to "Ingresos", Tipo.AHORRO to "Ahorro").forEach { (k, n) ->
                    Ficha(n, filtro == k, onClick = { filtro = k })
                }
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(BordeBoleta())
                    .background(p.carta)
                    .padding(horizontal = 16.dp, vertical = 22.dp),
            ) {
                Text(
                    "Boleta de ${nombreMes(a.mes)}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${a.movimientos.size} movimiento${if (a.movimientos.size == 1) "" else "s"} en ${a.mes.year}",
                    fontFamily = Fuentes.boleta, fontSize = 12.sp, color = p.tenue,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
                )
                LineaPunteada(p.linea2)
                if (porDia.isEmpty()) {
                    Vacio(
                        "Sin movimientos",
                        if (filtro == "todo") "Lo que anotes aparecerá aquí, ordenado por día." else "No hay movimientos de este tipo en el mes.",
                    )
                    LineaPunteada(p.linea2)
                }
                porDia.forEach { (_, lista) ->
                    val d = lista.first().fecha()
                    val gastoDia = lista.filter { it.tipo == Tipo.GASTO }.sumOf { it.centimos }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp)) {
                        Text(diaCorto(d), fontFamily = Fuentes.boleta, fontSize = 12.sp, color = p.tenue)
                        Spacer(Modifier.weight(1f))
                        if (gastoDia > 0) Text("−" + Dinero.sol(gastoDia), fontFamily = Fuentes.boleta, fontSize = 12.sp, color = p.tenue)
                    }
                    lista.forEach { m -> FilaMovimiento(m, e.metas, onClick = { onEditar(m) }, monoespaciada = true) }
                    Spacer(Modifier.height(4.dp))
                    LineaPunteada(p.linea2)
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(etiqueta, fontFamily = Fuentes.boleta, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text(Dinero.sol(total), fontFamily = Fuentes.boleta, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun LineaPunteada(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
    }
}

/** Borde de boleta cortada: muescas semicirculares arriba y abajo. */
class BordeBoleta(private val radio: Dp = 5.dp, private val paso: Dp = 14.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radio.toPx() }
        val s = with(density) { paso.toPx() }
        val n = (size.width / s).toInt().coerceAtLeast(1)
        val margen = (size.width - n * s) / 2
        val camino = Path().apply {
            moveTo(0f, 0f)
            for (i in 0 until n) {
                val c = margen + i * s + s / 2
                lineTo(c - r, 0f)
                arcTo(Rect(c - r, -r, c + r, r), 180f, -180f, false)
            }
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            for (i in n - 1 downTo 0) {
                val c = margen + i * s + s / 2
                lineTo(c + r, size.height)
                arcTo(Rect(c - r, size.height - r, c + r, size.height + r), 0f, -180f, false)
            }
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(camino)
    }
}
