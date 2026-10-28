package pe.sencillo.dominio

import pe.sencillo.datos.Movimiento

data class Recomendacion(
    val nivel: Nivel,
    val titulo: String,
    val detalle: String,
    /** Cuánto se podría ahorrar al mes siguiendo el consejo, si aplica. */
    val ahorroPosible: Long? = null,
)

/**
 * Reglas locales que miran el mes y sugieren cambios concretos. Todo se calcula en el celular;
 * no se envía nada a ningún servidor.
 */
object Recomendador {

    fun recomendar(
        a: Analisis,
        sobres: Map<String, Long>,
        diaPago: Int,
        tieneFondoEmergencia: Boolean,
    ): List<Recomendacion> = with(Dinero) {
        val r = mutableListOf<Recomendacion>()
        if (a.dia == 0 || a.gastos.size < 3) return r
        val factor = if (a.esActual) a.diasDelMes.toDouble() / a.dia else 1.0
        val meta = a.ahorroPct / 100.0

        // 1. Ritmo del mes
        if (a.esActual && a.proyeccion * 100 > a.limite * 102) {
            r += Recomendacion(
                Nivel.ALERTA,
                "A este ritmo gastarías ${sol0(a.proyeccion)} este mes",
                "Son ${sol0(a.proyeccion - a.limite)} más de lo que te permite tu meta de ahorro del ${a.ahorroPct}%. " +
                    "Para cerrar bien, trata de no pasar de ${sol0(a.porDia)} por día los ${a.diasRestantes} días que quedan.",
                a.proyeccion - a.limite,
            )
        } else if (!a.esActual && a.margen < 0) {
            r += Recomendacion(
                Nivel.ALERTA,
                "Gastaste ${sol0(-a.margen)} más de lo planeado",
                "Tu límite era ${sol0(a.limite)} para cumplir el ${a.ahorroPct}% de ahorro. Revisa qué sobres se pasaron y ajusta esos primero el próximo mes.",
            )
        }

        // 2. Sobres pasados o en camino de pasarse
        data class Fila(val c: Categoria, val presupuesto: Long, val gastado: Long, val proyectado: Long)
        val filas = Catalogo.gastos.map { c ->
            val g = a.porCategoria[c.id] ?: 0L
            Fila(c, sobres[c.id] ?: 0L, g, if (c.fijo) g else (g * factor).toLong())
        }.filter { it.presupuesto > 0 }

        filas.filter { it.gastado > it.presupuesto }
            .sortedByDescending { it.gastado - it.presupuesto }
            .take(2)
            .forEach { f ->
                r += Recomendacion(
                    if (f.gastado * 10 > f.presupuesto * 12) Nivel.ALERTA else Nivel.OJO,
                    "${f.c.nombre} se pasó de su sobre por ${sol0(f.gastado - f.presupuesto)}",
                    Catalogo.consejos[f.c.id] ?: "",
                    f.gastado - f.presupuesto,
                )
            }
        if (a.esActual) {
            filas.filter { !it.c.fijo && it.gastado <= it.presupuesto && it.proyectado * 10 > it.presupuesto * 11 }
                .maxByOrNull { it.proyectado - it.presupuesto }
                ?.let { f ->
                    r += Recomendacion(
                        Nivel.OJO,
                        "${f.c.nombre} va camino a pasarse",
                        "Llevas ${sol0(f.gastado)} de ${sol0(f.presupuesto)} y faltan ${a.diasRestantes} días. " +
                            "A este paso terminarías en ${sol0(f.proyectado)}. ${Catalogo.consejos[f.c.id] ?: ""}",
                        f.proyectado - f.presupuesto,
                    )
                }
        }

        // 3. Gastos hormiga
        val hT = a.totalHormiga
        if (a.hormiga.size >= 8 && hT * 100 >= a.gastado * 6) {
            val nombres = a.hormiga.groupBy { it.categoria }
                .mapValues { (_, l) -> l.sumOf(Movimiento::centimos) }
                .entries.sortedByDescending { it.value }.take(2)
                .mapNotNull { Catalogo.categoria(it.key)?.nombre?.lowercase() }
            val mensual = (hT * factor).toLong()
            r += Recomendacion(
                Nivel.OJO,
                "${a.hormiga.size} gastos hormiga suman ${sol0(hT)}",
                "Son compras chicas de S/ 15 o menos, sobre todo en ${nombres.joinToString(" y ")}." +
                    (if (a.esActual) " Proyectado al mes completo serían ${sol0(mensual)}." else "") +
                    " Recortar uno de cada tres te deja unos ${sol0(mensual / 3)} al mes.",
                mensual / 3,
            )
        }

        // 4. Ahorro
        val faltaAhorro = (a.ingreso * meta).toLong() - a.ahorrado
        when {
            a.tasaAhorro < meta * 0.5 -> r += Recomendacion(
                if (a.tasaAhorro < 0.05) Nivel.ALERTA else Nivel.OJO,
                "Llevas ahorrado ${pct(a.tasaAhorro)} de tus ingresos",
                "Tu meta es ${a.ahorroPct}%. Lo más efectivo es apartar ${sol0((a.ingreso * meta).toLong())} apenas te pagan (día $diaPago) y vivir con el resto. Lo que se aparta primero no se gasta.",
            )
            a.tasaAhorro < meta -> r += Recomendacion(
                Nivel.OJO,
                "Te faltan ${sol0(faltaAhorro)} para tu meta de ahorro",
                "Llevas ${pct(a.tasaAhorro)} de ${a.ahorroPct}%. " + if (a.esActual)
                    "Si separas eso hoy, lo que queda (${sol0(maxOf(0L, a.disponible - faltaAhorro))}) es lo que tienes para el resto del mes."
                else "El próximo mes apártalo el mismo día que te pagan.",
            )
            else -> r += Recomendacion(
                Nivel.BIEN,
                "Ya cumpliste tu meta de ahorro del ${a.ahorroPct}%",
                "Apartaste ${sol0(a.ahorrado)} este mes. Si te sobra algo al cierre, mándalo a la meta que tengas más cerca.",
            )
        }

        // 5. Tarjeta de crédito
        val credito = a.porPago["credito"] ?: 0L
        if (a.gastado > 0 && credito * 10 > a.gastado * 3) {
            r += Recomendacion(
                Nivel.OJO,
                "${pct(credito.toDouble() / a.gastado)} de tus gastos fueron con tarjeta de crédito",
                "Son ${sol0(credito)}. Asegúrate de pagar el total del estado de cuenta, no el mínimo: los intereses de las tarjetas de consumo pueden pasar del 50% anual.",
            )
        }

        // 6. Reparto 50/30/20
        val gustosMeta = 0.5 - meta
        if (a.ingreso > 0 && a.gustos.toDouble() / a.ingreso > gustosMeta + 0.05) {
            r += Recomendacion(
                Nivel.OJO,
                "Tus gustos se llevan ${pct(a.gustos.toDouble() / a.ingreso)} de tus ingresos",
                "Para ahorrar tu ${a.ahorroPct}%, los gustos deberían quedarse cerca de ${pct(gustosMeta)} (${sol0((a.ingreso * gustosMeta).toLong())}). Elige uno o dos que de verdad disfrutes y recorta el resto.",
            )
        }
        if (a.ingreso > 0 && a.necesidades.toDouble() / a.ingreso > 0.6) {
            r += Recomendacion(
                Nivel.OJO,
                "Las necesidades ocupan ${pct(a.necesidades.toDouble() / a.ingreso)} de tus ingresos",
                "Cuando lo básico pasa del 60%, el margen para ahorrar es muy chico. Revisa los gastos fijos que se pueden negociar o cambiar, como plan de celular, internet o alquiler.",
            )
        }

        // 7. Comparación con el mes anterior (solo meses cerrados)
        val prev = a.porCategoriaAnterior
        if (prev != null && !a.esActual) {
            Catalogo.gastos.map { c ->
                val base = prev[c.id] ?: 0L
                Triple(c, (a.porCategoria[c.id] ?: 0L) - base, base)
            }.filter { (_, d, base) -> d > 5000 && base > 0 && d * 10 > base * 3 }
                .maxByOrNull { it.second }
                ?.let { (c, d, base) ->
                    r += Recomendacion(
                        Nivel.OJO,
                        "${c.nombre} subió ${sol0(d)} frente al mes anterior",
                        "Pasó de ${sol0(base)} a ${sol0(base + d)}. Si fue algo puntual, está bien. Si se repite, súbele el sobre o recórtalo.",
                    )
                }
        }

        // 8. Fondo de emergencia
        if (!tieneFondoEmergencia) {
            r += Recomendacion(
                Nivel.OJO,
                "Todavía no tienes un fondo de emergencia",
                "Apunta a cubrir 3 meses de gastos básicos: unos ${sol0(Analizador.fondoSugerido(sobres, a.sueldo))}. Créalo en Metas y aporta aunque sea poco cada mes.",
            )
        }

        // 9. Ingreso extra
        if (a.extra > 0) {
            r += Recomendacion(
                Nivel.BIEN,
                "Este mes entraron ${sol0(a.extra)} extra",
                "Si mandas la mitad (${sol0(a.extra / 2)}) a tus metas, avanzas sin tocar tu presupuesto normal.",
            )
        }

        return r.sortedBy { it.nivel.ordinal }.take(6)
    }
}
