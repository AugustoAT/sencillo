package pe.sencillo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.fecha
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Tipo
import pe.sencillo.ui.tema.Tema

/** Todos los movimientos del mes, agrupados por día. */
@Composable
fun PantallaMovimientos(e: EstadoUi.Listo, onEditar: (Movimiento) -> Unit) {
    val p = Tema.p
    val a = e.analisis
    var filtro by rememberSaveable { mutableStateOf("todo") }
    val movs = a.movimientos
        .filter { filtro == "todo" || it.tipo == filtro }
        .sortedWith(compareByDescending<Movimiento> { it.dia }.thenByDescending { it.id })
    val porDia = movs.groupBy { it.dia }.toList()
    val resumen = when (filtro) {
        Tipo.INGRESO -> "Entraron ${Dinero.sol(a.extra)} aparte de tu sueldo."
        Tipo.AHORRO -> "Apartaste ${Dinero.sol(a.ahorrado)} para ahorrar."
        else -> "Gastaste ${Dinero.sol(a.gastado)} en ${nombreMes(a.mes)}."
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("todo" to "Todo", Tipo.GASTO to "Gastos", Tipo.INGRESO to "Ingresos", Tipo.AHORRO to "Ahorro").forEach { (k, n) ->
                    Ficha(n, filtro == k, onClick = { filtro = k })
                }
            }
            Text(resumen, fontSize = 15.sp, color = p.texto2, style = estiloMonto, modifier = Modifier.padding(top = 14.dp))
        }
        if (porDia.isEmpty()) item {
            Vacio(
                "Sin movimientos",
                if (filtro == "todo") "Lo que anotes aparecerá aquí, ordenado por día. Toca Anotar para empezar."
                else "No hay movimientos de este tipo en ${nombreMes(a.mes)}.",
            )
        }
        porDia.forEach { (_, lista) ->
            item(key = "dia-${lista.first().dia}") {
                val gastoDia = lista.filter { it.tipo == Tipo.GASTO }.sumOf { it.centimos }
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(diaLargo(lista.first().fecha()), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    if (gastoDia > 0) Text("Gastaste ${Dinero.sol(gastoDia)}", fontSize = 13.sp, color = p.texto2, style = estiloMonto)
                }
                Separador()
            }
            items(lista, key = { it.id }) { m ->
                FilaMovimiento(m, e.metas, onClick = { onEditar(m) }, conFecha = false)
                Separador()
            }
        }
    }
}
