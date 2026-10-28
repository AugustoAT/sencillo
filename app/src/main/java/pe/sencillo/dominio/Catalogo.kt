package pe.sencillo.dominio

enum class Grupo(val nombre: String) { NECESIDAD("Necesidades"), GUSTO("Gustos") }

data class Categoria(
    val id: String,
    val nombre: String,
    val grupo: Grupo,
    val fijo: Boolean,
    val pista: String,
)

data class Opcion(val id: String, val nombre: String)

object Tipo {
    const val GASTO = "G"
    const val INGRESO = "I"
    const val AHORRO = "A"
    val todos = setOf(GASTO, INGRESO, AHORRO)
}

object Catalogo {
    const val AHORRO_LIBRE = "libre"
    const val PREFIJO_META = "meta:"

    val gastos = listOf(
        Categoria("vivienda", "Vivienda", Grupo.NECESIDAD, true, "Alquiler, mantenimiento"),
        Categoria("servicios", "Servicios", Grupo.NECESIDAD, true, "Luz, agua, internet, celular"),
        Categoria("mercado", "Mercado", Grupo.NECESIDAD, false, "Mercado, súper, bodega"),
        Categoria("transporte", "Transporte", Grupo.NECESIDAD, false, "Combi, metro, taxi, gasolina"),
        Categoria("salud", "Salud", Grupo.NECESIDAD, false, "Farmacia, consultas"),
        Categoria("educacion", "Estudios", Grupo.NECESIDAD, true, "Pensión, cursos, materiales"),
        Categoria("deudas", "Deudas", Grupo.NECESIDAD, true, "Cuotas, préstamos"),
        Categoria("comida", "Comida fuera", Grupo.GUSTO, false, "Menú, delivery, cafés"),
        Categoria("salidas", "Salidas", Grupo.GUSTO, false, "Juntadas, cine, tragos"),
        Categoria("ropa", "Ropa y compras", Grupo.GUSTO, false, "Ropa, tecnología, casa"),
        Categoria("suscripciones", "Suscripciones", Grupo.GUSTO, true, "Streaming, apps, gimnasio"),
        Categoria("otros", "Otros", Grupo.GUSTO, false, "Lo que no encaja"),
    )
    private val porId = gastos.associateBy { it.id }
    fun categoria(id: String): Categoria? = porId[id]

    val ingresos = listOf(
        Opcion("cachuelo", "Cachuelo"),
        Opcion("gratificacion", "Gratificación o bono"),
        Opcion("reembolso", "Reembolso"),
        Opcion("otro-ingreso", "Otro ingreso"),
    )

    val pagos = listOf(
        Opcion("efectivo", "Efectivo"),
        Opcion("yape", "Yape / Plin"),
        Opcion("debito", "Débito"),
        Opcion("credito", "Crédito"),
    )

    /** Cómo se reparte cada mitad del sueldo entre sobres al empezar. */
    private val pesos = mapOf(
        "vivienda" to .40, "servicios" to .12, "mercado" to .25, "transporte" to .12, "salud" to .05,
        "educacion" to .03, "deudas" to .03,
        "comida" to .35, "salidas" to .20, "ropa" to .15, "suscripciones" to .15, "otros" to .15,
    )

    /** Regla 50 / (50 - ahorro) / ahorro, redondeada a decenas de soles. */
    fun sobresBase(sueldo: Long, ahorroPct: Int): Map<String, Long> {
        val nec = sueldo * 0.5
        val gus = sueldo * maxOf(0.0, 0.5 - ahorroPct / 100.0)
        return gastos.associate { c ->
            val base = (if (c.grupo == Grupo.NECESIDAD) nec else gus) * (pesos[c.id] ?: 0.0)
            c.id to Math.round(base / 1000.0) * 1000
        }
    }

    val consejos = mapOf(
        "comida" to "Fija dos o tres días de táper a la semana. Un menú cuesta entre S/ 12 y S/ 15, y la misma porción hecha en casa sale cerca de la mitad.",
        "salidas" to "Ponle un monto semanal a las salidas y sepáralo el lunes. Cuando se acaba, se acaba hasta la otra semana.",
        "ropa" to "Antes de una compra que no estaba en el plan, espera 48 horas. Si después de eso todavía la quieres, cómprala.",
        "suscripciones" to "Revisa cuáles usaste este mes. Cancelar una sola de S/ 35 son S/ 420 al año.",
        "transporte" to "Mira los trayectos que repites en taxi: si alguno se puede hacer en metro o combi, ahí está el ahorro.",
        "mercado" to "Haz lista y compra por semana, no por día. Las compras sueltas en la bodega suman más de lo que parece.",
        "servicios" to "Revisa tu plan de celular e internet. Muchas veces pagas por más datos o velocidad de la que usas.",
        "vivienda" to "Si la vivienda pasa del 35% de tu sueldo, todo lo demás se aprieta. Tenlo en cuenta en tu próxima renovación.",
        "salud" to "La salud no se recorta, pero sí conviene un pequeño fondo aparte para imprevistos médicos.",
        "educacion" to "Si es un gasto puntual del mes, súbele el sobre en Ajustes para que tu presupuesto sea realista.",
        "deudas" to "Si tienes varias deudas, paga primero la de interés más alto y en las demás cubre solo la cuota.",
        "otros" to "Mucho en «Otros» suele ser gasto hormiga. Clasifícalo bien para ver a dónde se va.",
    )

    fun nombrePago(id: String?): String? = pagos.firstOrNull { it.id == id }?.nombre
    fun nombreIngreso(id: String): String = ingresos.firstOrNull { it.id == id }?.nombre ?: "Ingreso"
}
