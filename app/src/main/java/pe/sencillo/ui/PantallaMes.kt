package pe.sencillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.Movimiento
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Categoria
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Nivel
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema

private data class FilaSobre(val c: Categoria, val presupuesto: Long, val gastado: Long) {
    val proporcion: Float get() = if (presupuesto > 0) gastado.toFloat() / presupuesto else if (gastado > 0) 9f else 0f
}

@Composable
fun PantallaMes(
    e: EstadoUi.Listo,
    onEditar: (Movimiento) -> Unit,
    onVerRecomendaciones: () -> Unit,
    onEditarSobres: () -> Unit,
    onVerBoleta: () -> Unit,
) {
    val p = Tema.p
    val a = e.analisis
    var verTodos by rememberSaveable { mutableStateOf(false) }
    val sobres = Catalogo.gastos
        .map { FilaSobre(it, e.sobres[it.id] ?: 0L, a.porCategoria[it.id] ?: 0L) }
        .filter { it.presupuesto > 0 || it.gastado > 0 }
        .sortedByDescending { it.proporcion }
    val visibles = if (verTodos) sobres else sobres.take(5)
    val ultimos = a.movimientos.sortedWith(compareByDescending<Movimiento> { it.dia }.thenByDescending { it.id }).take(4)

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)) {
        item { TarjetaBillete(a) }
        item {
            Tarjeta(Modifier.padding(top = 14.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    Pildora(e.estado.nivel, e.estado.titulo)
                    Spacer(Modifier.width(10.dp))
                    Text(e.estado.detalle, fontSize = 14.sp, color = p.texto2)
                }
            }
        }
        if (e.recomendaciones.isNotEmpty()) item {
            val pendientes = e.recomendaciones.count { it.nivel != Nivel.BIEN }
            Tarjeta(Modifier.padding(top = 10.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onVerRecomendaciones).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Moneda(e.recomendaciones.size.toString())
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (pendientes > 0) "Sugerencias para este mes" else "Buenas noticias de tu mes", fontWeight = FontWeight.SemiBold)
                        Text(e.recomendaciones.first().titulo, fontSize = 13.sp, color = p.tenue, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Iconos.derecha, null, tint = p.tenue, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { EncabezadoSeccion("Tus sobres", "Lo que separaste para cada cosa este mes", "Editar", onEditarSobres) }
        items(visibles, key = { it.c.id }) { f -> Sobre(f) }
        if (sobres.size > 5) item {
            Text(
                if (verTodos) "Ver menos" else "Ver los ${sobres.size} sobres",
                color = p.marca, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).clickable { verTodos = !verTodos }.padding(6.dp),
            )
        }
        item { EncabezadoSeccion("Lo último", accion = "Ver la boleta", onAccion = onVerBoleta) }
        item {
            Tarjeta {
                if (ultimos.isEmpty()) {
                    Vacio("Nada anotado en ${nombreMes(a.mes)}", "Toca el botón dorado para anotar tu primer gasto. Toma menos de 5 segundos.")
                } else {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        ultimos.forEach { m -> FilaMovimiento(m, e.metas, onClick = { onEditar(m) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sobre(f: FilaSobre) {
    val p = Tema.p
    val r = f.proporcion
    val (relleno, pista) = when {
        r > 1f -> p.alerta to p.alerta.copy(alpha = .2f)
        r >= .85f -> p.ojo to p.ojo.copy(alpha = .22f)
        else -> p.marca to p.marcaSuave
    }
    val resto = f.presupuesto - f.gastado
    Box(Modifier.padding(bottom = 8.dp)) {
        Tarjeta(Modifier.solapa(p.papel, p.linea)) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 13.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(f.c.nombre, fontWeight = FontWeight.SemiBold)
                        Text(f.c.pista, fontSize = 12.sp, color = p.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Text(Dinero.sol0(f.gastado), fontFamily = Fuentes.cifra, fontSize = 14.sp)
                            Text(" de ${Dinero.sol0(f.presupuesto)}", fontSize = 13.sp, color = p.tenue)
                        }
                        Text(
                            if (resto >= 0) "quedan ${Dinero.sol0(resto)}" else "se pasó ${Dinero.sol0(-resto)}",
                            fontSize = 12.sp,
                            color = if (resto >= 0) p.tenue else p.alertaTexto,
                            fontWeight = if (resto >= 0) FontWeight.Normal else FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Medidor(r, relleno, pista)
            }
        }
    }
}

/** La solapa del sobre: una pestaña en U sobre el borde superior de la tarjeta. */
private fun Modifier.solapa(fondo: Color, borde: Color) = drawWithContent {
    drawContent()
    val w = 28.dp.toPx()
    val h = 9.dp.toPx()
    val x = 14.dp.toPx()
    // Solo la mitad de abajo, dentro de la tarjeta: una muesca en U en el borde superior.
    clipRect(0f, 0f, size.width, size.height) {
        drawRoundRect(fondo, topLeft = Offset(x, -h), size = Size(w, h * 2), cornerRadius = CornerRadius(h, h))
        drawRoundRect(borde, topLeft = Offset(x, -h), size = Size(w, h * 2), cornerRadius = CornerRadius(h, h), style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun Moneda(texto: String, tam: androidx.compose.ui.unit.Dp = 44.dp) {
    val p = Tema.p
    Box(
        Modifier
            .size(tam)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(p.solClaro, p.sol), center = Offset.Unspecified))
            .border(3.dp, p.solClaro, CircleShape)
            .drawBehind {
                drawCircle(p.sobreSol.copy(alpha = .22f), radius = size.minDimension / 2 - 4.dp.toPx(), style = Stroke(1.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, fontFamily = Fuentes.billete, fontSize = 18.sp, color = p.sobreSol)
    }
}

