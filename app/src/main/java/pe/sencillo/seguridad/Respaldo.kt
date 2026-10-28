package pe.sencillo.seguridad

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import pe.sencillo.datos.Meta
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.Perfil
import pe.sencillo.datos.Sobre
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Tipo
import java.time.LocalDate

data class DatosRespaldo(
    val perfil: Perfil,
    val sobres: List<Sobre>,
    val metas: List<Meta>,
    val movimientos: List<Movimiento>,
)

/**
 * Pasa los datos a JSON y de vuelta. Al importar, todo se valida como si viniera de un extraño:
 * tipos, rangos, largos y referencias. Un respaldo con algo raro se rechaza completo.
 */
object Respaldo {
    private const val VERSION = 1
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAX_MOVIMIENTOS = 200_000
    private const val MAX_METAS = 200
    private const val MAX_CENTIMOS = 10_000_000_000L // S/ 100 millones
    private val DIA_MIN = LocalDate.of(2000, 1, 1).toEpochDay()
    private val DIA_MAX = LocalDate.of(2100, 12, 31).toEpochDay()

    fun aJson(d: DatosRespaldo): ByteArray {
        val o = JSONObject()
        o.put("app", "sencillo").put("version", VERSION)
        o.put("perfil", JSONObject().apply {
            put("sueldo", d.perfil.sueldo); put("diaPago", d.perfil.diaPago); put("ahorroPct", d.perfil.ahorroPct)
            put("bloqueo", d.perfil.bloqueo); put("ocultarPantalla", d.perfil.ocultarPantalla)
        })
        o.put("sobres", JSONObject().apply { d.sobres.forEach { put(it.categoria, it.centimos) } })
        o.put("metas", JSONArray().apply {
            d.metas.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("nombre", m.nombre); put("objetivo", m.objetivo)
                    put("inicial", m.inicial); put("limite", m.limite ?: JSONObject.NULL)
                })
            }
        })
        o.put("movimientos", JSONArray().apply {
            d.movimientos.forEach { m ->
                put(JSONObject().apply {
                    put("tipo", m.tipo); put("centimos", m.centimos); put("categoria", m.categoria)
                    put("nota", m.nota); put("dia", m.dia); put("pago", m.pago ?: JSONObject.NULL)
                })
            }
        })
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun deJson(bytes: ByteArray): DatosRespaldo = try {
        leer(JSONObject(String(bytes, Charsets.UTF_8)))
    } catch (e: JSONException) {
        throw RespaldoInvalido("El respaldo está dañado.")
    }

    private fun leer(o: JSONObject): DatosRespaldo {
        exigir(o.optString("app") == "sencillo", "El archivo no es un respaldo de Sencillo.")
        exigir(o.optInt("version") == VERSION, "Este respaldo es de otra versión de Sencillo.")

        val p = o.getJSONObject("perfil")
        val perfil = Perfil(
            sueldo = rango(p.getLong("sueldo"), 0, MAX_CENTIMOS),
            diaPago = rango(p.getInt("diaPago").toLong(), 1, 31).toInt(),
            ahorroPct = rango(p.getInt("ahorroPct").toLong(), 0, 80).toInt(),
            bloqueo = p.optBoolean("bloqueo", false),
            ocultarPantalla = p.optBoolean("ocultarPantalla", true),
        )

        val s = o.getJSONObject("sobres")
        val sobres = Catalogo.gastos.map { c -> Sobre(c.id, rango(s.optLong(c.id, 0), 0, MAX_CENTIMOS)) }

        val ma = o.getJSONArray("metas")
        exigir(ma.length() <= MAX_METAS, "El respaldo tiene demasiadas metas.")
        val metas = (0 until ma.length()).map { i ->
            val m = ma.getJSONObject(i)
            val limite = if (m.isNull("limite")) null else rango(m.getLong("limite"), DIA_MIN, DIA_MAX)
            Meta(
                id = rango(m.getLong("id"), 1, Long.MAX_VALUE),
                nombre = texto(m.getString("nombre"), 40).also { exigir(it.isNotBlank(), "Hay una meta sin nombre.") },
                objetivo = rango(m.getLong("objetivo"), 1, MAX_CENTIMOS),
                inicial = rango(m.getLong("inicial"), 0, MAX_CENTIMOS),
                limite = limite,
            )
        }
        val idsMeta = metas.map { "${Catalogo.PREFIJO_META}${it.id}" }.toSet()
        exigir(metas.map { it.id }.toSet().size == metas.size, "El respaldo está dañado.")

        val mv = o.getJSONArray("movimientos")
        exigir(mv.length() <= MAX_MOVIMIENTOS, "El respaldo tiene demasiados movimientos.")
        val gastosValidos = Catalogo.gastos.map { it.id }.toSet()
        val ingresosValidos = Catalogo.ingresos.map { it.id }.toSet()
        val pagosValidos = Catalogo.pagos.map { it.id }.toSet()
        val movimientos = (0 until mv.length()).map { i ->
            val m = mv.getJSONObject(i)
            val tipo = m.getString("tipo")
            exigir(tipo in Tipo.todos, "El respaldo está dañado.")
            val cat = m.getString("categoria")
            val catOk = when (tipo) {
                Tipo.GASTO -> cat in gastosValidos
                Tipo.INGRESO -> cat in ingresosValidos
                else -> cat == Catalogo.AHORRO_LIBRE || cat in idsMeta
            }
            exigir(catOk, "El respaldo tiene una categoría desconocida.")
            val pago = if (m.isNull("pago")) null else m.getString("pago")
            exigir(pago == null || pago in pagosValidos, "El respaldo tiene un medio de pago desconocido.")
            Movimiento(
                tipo = tipo,
                centimos = rango(m.getLong("centimos"), 1, MAX_CENTIMOS),
                categoria = cat,
                nota = texto(m.optString("nota", ""), 60),
                dia = rango(m.getLong("dia"), DIA_MIN, DIA_MAX),
                pago = if (tipo == Tipo.GASTO) pago ?: "efectivo" else null,
            )
        }
        return DatosRespaldo(perfil, sobres, metas, movimientos)
    }

    private fun exigir(cond: Boolean, msg: String) { if (!cond) throw RespaldoInvalido(msg) }

    private fun rango(v: Long, min: Long, max: Long): Long {
        exigir(v in min..max, "El respaldo tiene valores fuera de rango.")
        return v
    }

    /** Recorta y quita caracteres de control (saltos raros, caracteres invisibles de dirección, etc.). */
    fun texto(s: String, max: Int): String =
        s.filter { !it.isISOControl() && Character.getType(it) != Character.FORMAT.toInt() }.trim().take(max)
}
