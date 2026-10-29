package pe.sencillo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.Movimiento
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Categoria
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Nivel
import pe.sencillo.ui.tema.Tema

private data class FilaLimite(val c: Categoria, val limite: Long, val gastado: Long) {
    val proporcion: Float get() = if (limite > 0) gastado.toFloat() / limite else 0f
}

@Composable
fun PantallaMes(
    e: EstadoUi.Listo,
    onEditar: (Movimiento) -> Unit,
    onVerRecomendaciones: () -> Unit,
    onEditarLimites: () -> Unit,
    onVerMovimientos: () -> Unit,
) {
    val p = Tema.p
    val a = e.analisis
    var verTodos by rememberSaveable { mutableStateOf(false) }
    // Primero las categorías que más se acercan a su límite: son las que piden atención.
    val filas = Catalogo.gastos
        .map { FilaLimite(it, e.sobres[it.id] ?: 0L, a.porCategoria[it.id] ?: 0L) }
        .filter { it.limite > 0 || it.gastado > 0 }
        .sortedWith(compareByDescending<FilaLimite> { if (it.limite == 0L && it.gastado > 0) 9f else it.proporcion }.thenByDescending { it.gastado })
    val visibles = if (verTodos) filas else filas.take(5)
    val ultimos = a.movimientos.sortedWith(compareByDescending<Movimiento> { it.dia }.thenByDescending { it.id }).take(4)
    val pendientes = e.recomendaciones.count { it.nivel != Nivel.BIEN }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
        item { ResumenMes(a, Modifier.zonaTutorial(Zona.SALDO)) }
        item {
            AvisoEstado(e.estado, Modifier.padding(top = 16.dp))
            if (e.recomendaciones.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(onClick = onVerRecomendaciones)
                        .padding(start = 30.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when {
                            pendientes == 0 -> "Ver cómo te fue"
                            pendientes == 1 -> "Ver 1 sugerencia para ahorrar más"
                            else -> "Ver $pendientes sugerencias para ahorrar más"
                        },
                        color = p.marca, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    )
                    Icon(Iconos.derecha, null, tint = p.marca, modifier = Modifier.padding(start = 2.dp).size(16.dp))
                }
            }
        }
        item {
            // El título y la primera categoría van juntos: el tutorial los resalta como una sola zona.
            Column(Modifier.padding(top = 28.dp).zonaTutorial(Zona.LIMITES)) {
                EncabezadoSeccion(
                    "Límites por categoría",
                    "Lo máximo que quieres gastar en cada cosa este mes",
                    accion = "Editar",
                    espacioArriba = 0.dp,
                    onAccion = onEditarLimites,
                )
                visibles.firstOrNull()?.let { Limite(it) }
            }
            if (visibles.isNotEmpty()) Separador()
        }
        items(visibles.drop(1), key = { it.c.id }) { f ->
            Limite(f)
            Separador()
        }
        if (filas.size > 5) item {
            Enlace(if (verTodos) "Ver menos" else "Ver las ${filas.size} categorías", { verTodos = !verTodos }, Modifier.padding(top = 4.dp))
        }
        item { EncabezadoSeccion("Lo último que anotaste", accion = if (ultimos.isEmpty()) null else "Ver todo", onAccion = onVerMovimientos) }
        if (ultimos.isEmpty()) item {
            Vacio(
                "Todavía no anotas nada en ${nombreMes(a.mes)}",
                "Cuando gastes o recibas plata, toca Anotar abajo al centro. Toma unos segundos.",
            )
        }
        items(ultimos, key = { it.id }) { m ->
            FilaMovimiento(m, e.metas, onClick = { onEditar(m) })
            Separador()
        }
    }
}

@Composable
private fun Limite(f: FilaLimite) {
    val p = Tema.p
    val r = f.proporcion
    val resto = f.limite - f.gastado
    val (relleno, pista) = when {
        f.limite > 0 && r > 1f -> p.alerta to p.alerta.copy(alpha = .18f)
        r >= .85f -> p.ojo to p.ojo.copy(alpha = .22f)
        else -> p.marca to p.marcaSuave
    }
    Column(Modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(f.c.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(f.c.pista, fontSize = 12.5.sp, color = p.texto2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (f.limite > 0) "${Dinero.sol0(f.gastado)} de ${Dinero.sol0(f.limite)}" else Dinero.sol0(f.gastado),
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, style = estiloMonto,
                )
                Text(
                    when {
                        f.limite == 0L -> "Sin límite"
                        resto >= 0 -> "Te quedan ${Dinero.sol0(resto)}"
                        else -> "Te pasaste por ${Dinero.sol0(-resto)}"
                    },
                    fontSize = 12.5.sp,
                    color = if (f.limite > 0 && resto < 0) p.alertaTexto else p.texto2,
                    fontWeight = if (f.limite > 0 && resto < 0) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        if (f.limite > 0) {
            Spacer(Modifier.height(10.dp))
            Medidor(r, relleno, pista)
        }
    }
}
