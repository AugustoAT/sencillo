package pe.sencillo.dominio

import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.fecha
import java.time.LocalDate
import java.time.YearMonth

/** Todo lo que la app calcula sobre un mes. Montos en céntimos. */
data class Analisis(
    val mes: YearMonth,
    val diasDelMes: Int,
    /** Día del mes que se toma como "hoy": el real si es el mes en curso, el último si ya pasó, 0 si es futuro. */
    val dia: Int,
    val esActual: Boolean,
    val sueldo: Long,
    val ahorroPct: Int,
    val movimientos: List<Movimiento>,
    val gastos: List<Movimiento>,
    val extra: Long,
    val ingreso: Long,
    val gastado: Long,
    val ahorrado: Long,
    val disponible: Long,
    val porCategoria: Map<String, Long>,
    val necesidades: Long,
    val gustos: Long,
    val fijos: Long,
    val variables: Long,
    /** Gasto estimado a fin de mes: los fijos ya pagados + el ritmo de los variables. */
    val proyeccion: Long,
    /** Lo máximo que se puede gastar y aún cumplir la meta de ahorro. */
    val limite: Long,
    val diasRestantes: Int,
    val margen: Long,
    val porDia: Long,
    /** acumulado[d] = gasto total hasta el día d (acumulado[0] = 0). */
    val acumulado: LongArray,
    val hormiga: List<Movimiento>,
    val porPago: Map<String, Long>,
    val porCategoriaAnterior: Map<String, Long>?,
) {
    val tasaAhorro: Double get() = if (ingreso > 0) ahorrado.toDouble() / ingreso else 0.0
    val totalHormiga: Long get() = hormiga.sumOf { it.centimos }
}

enum class Nivel { ALERTA, OJO, BIEN }

data class Estado(val nivel: Nivel, val titulo: String, val detalle: String)

object Analizador {
    const val TOPE_HORMIGA = 1500L // S/ 15

    fun analizar(
        mes: YearMonth,
        hoy: LocalDate,
        sueldo: Long,
        ahorroPct: Int,
        movs: List<Movimiento>,
        anteriores: List<Movimiento>?,
    ): Analisis {
        val actual = YearMonth.from(hoy)
        val dm = mes.lengthOfMonth()
        val esActual = mes == actual
        val dia = when {
            esActual -> hoy.dayOfMonth
            mes.isBefore(actual) -> dm
            else -> 0
        }
        val gastos = movs.filter { it.tipo == Tipo.GASTO }
        val extra = movs.filter { it.tipo == Tipo.INGRESO }.sumOf { it.centimos }
        val ingreso = sueldo + extra
        val gastado = gastos.sumOf { it.centimos }
        val ahorrado = movs.filter { it.tipo == Tipo.AHORRO }.sumOf { it.centimos }
        val porCat = sumarPorCategoria(gastos)
        fun sumaDe(pred: (Categoria) -> Boolean) =
            Catalogo.gastos.filter(pred).sumOf { porCat[it.id] ?: 0L }
        val nec = sumaDe { it.grupo == Grupo.NECESIDAD }
        val gus = sumaDe { it.grupo == Grupo.GUSTO }
        val fijos = sumaDe { it.fijo }
        val variables = gastado - fijos
        val proy = if (dia > 0) fijos + variables * dm / dia else 0L
        val limite = ingreso * (100 - ahorroPct) / 100
        val diasRest = when {
            esActual -> dm - dia + 1
            mes.isAfter(actual) -> dm
            else -> 0
        }
        val margen = limite - gastado
        val porDia = if (diasRest > 0) maxOf(0L, margen) / diasRest else 0L

        val acum = LongArray(dm + 1)
        for (g in gastos) {
            val d = g.fecha().dayOfMonth
            if (YearMonth.from(g.fecha()) == mes && d in 1..dm) acum[d] += g.centimos
        }
        for (d in 1..dm) acum[d] += acum[d - 1]

        val hormiga = gastos.filter { g ->
            val c = Catalogo.categoria(g.categoria)
            g.centimos <= TOPE_HORMIGA && c != null && c.grupo == Grupo.GUSTO && !c.fijo
        }
        val porPago = Catalogo.pagos.associate { p ->
            p.id to gastos.filter { (it.pago ?: "efectivo") == p.id }.sumOf { it.centimos }
        }
        val prev = anteriores?.takeIf { it.isNotEmpty() }?.let { l ->
            sumarPorCategoria(l.filter { it.tipo == Tipo.GASTO })
        }

        return Analisis(
            mes, dm, dia, esActual, sueldo, ahorroPct, movs, gastos, extra, ingreso, gastado, ahorrado,
            ingreso - gastado - ahorrado, porCat, nec, gus, fijos, variables, proy, limite, diasRest,
            margen, porDia, acum, hormiga, porPago, prev,
        )
    }

    private fun sumarPorCategoria(gastos: List<Movimiento>): Map<String, Long> {
        val m = Catalogo.gastos.associate { it.id to 0L }.toMutableMap()
        for (g in gastos) m[g.categoria] = (m[g.categoria] ?: 0L) + g.centimos
        return m
    }

    fun estado(a: Analisis): Estado = with(Dinero) {
        when {
            a.dia == 0 -> Estado(Nivel.BIEN, "Mes por empezar", "Cuando llegue el mes, aquí verás cómo vas.")
            a.disponible < 0 -> Estado(Nivel.ALERTA, "Te pasaste", "Llevas ${sol0(-a.disponible)} por encima de lo que entró este mes.")
            !a.esActual && a.margen >= 0 -> Estado(Nivel.BIEN, "Mes cerrado", "Cerraste ahorrando ${pct(a.tasaAhorro)} de tus ingresos.")
            !a.esActual -> Estado(Nivel.OJO, "Mes cerrado", "Gastaste ${sol0(-a.margen)} más de lo planeado.")
            a.proyeccion * 100 > a.limite * 103 -> Estado(Nivel.OJO, "Ojo con el ritmo", "Si sigues así, gastarías ${sol0(a.proyeccion)} y tu límite es ${sol0(a.limite)}.")
            else -> Estado(Nivel.BIEN, "Vas bien", "A este ritmo cierras con ${sol0(maxOf(0L, a.limite - a.proyeccion))} de margen, además de lo que ahorraste.")
        }
    }

    /** Fondo de emergencia sugerido: 3 meses de sobres de necesidades, redondeado a S/ 100. */
    fun fondoSugerido(sobres: Map<String, Long>, sueldo: Long): Long {
        val basicos = Catalogo.gastos.filter { it.grupo == Grupo.NECESIDAD }.sumOf { sobres[it.id] ?: 0L }
        val base = if (basicos > 0) basicos else sueldo / 2
        return Math.round(base * 3 / 10000.0) * 10000
    }
}
