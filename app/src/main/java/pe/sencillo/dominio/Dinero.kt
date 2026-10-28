package pe.sencillo.dominio

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Todo el dinero de la app se maneja en céntimos (Long) para no arrastrar errores de redondeo. */
object Dinero {
    private fun simbolos() = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }

    private fun f2() = DecimalFormat("#,##0.00", simbolos())
    private fun f0() = DecimalFormat("#,##0", simbolos())

    /** S/ 1,234.50 */
    fun sol(c: Long): String = (if (c < 0) "−" else "") + "S/ " + f2().format(abs(c) / 100.0)

    /** S/ 1,235 (redondeado a soles) */
    fun sol0(c: Long): String {
        val r = (abs(c) / 100.0).roundToLong()
        return (if (c < 0 && r > 0) "−" else "") + "S/ " + f0().format(r)
    }

    fun parteEntera(c: Long): String = f0().format(abs(c) / 100)
    fun parteCentimos(c: Long): String = (abs(c) % 100).toString().padStart(2, '0')

    private val formatoTexto = Regex("""\d{1,7}(\.\d{0,2})?""")

    /** Convierte lo que se escribió en el teclado ("12.5") a céntimos. Devuelve null si no es válido. */
    fun deTexto(t: String): Long? {
        val limpio = t.trim().replace(",", "")
        if (!formatoTexto.matches(limpio)) return null
        val partes = limpio.split(".")
        val entero = partes[0].toLong()
        val dec = (partes.getOrNull(1) ?: "").padEnd(2, '0')
        return entero * 100 + dec.toLong()
    }

    /** Céntimos a texto editable: 1250 -> "12.5", 1200 -> "12" */
    fun aTexto(c: Long): String {
        val e = c / 100
        val d = c % 100
        return when {
            d == 0L -> e.toString()
            d % 10 == 0L -> "$e.${d / 10}"
            else -> "$e.${d.toString().padStart(2, '0')}"
        }
    }

    fun pct(x: Double, decimales: Int = 0): String =
        if (x.isFinite()) String.format(Locale.US, "%.${decimales}f%%", x * 100) else "0%"
}
