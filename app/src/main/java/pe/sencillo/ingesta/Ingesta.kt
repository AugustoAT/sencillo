package pe.sencillo.ingesta

import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Tipo
import pe.sencillo.seguridad.Respaldo
import java.time.LocalDate

/**
 * Punto de entrada para integraciones futuras (notificaciones de Yape, estados de cuenta en CSV,
 * widget, atajos). Ver INTEGRACIONES.md.
 *
 * Regla de diseño: ninguna fuente guarda movimientos por su cuenta. Cada una produce
 * [MovimientoPropuesto]s que pasan por [ValidadorDePropuestas] y luego la persona los confirma
 * en la hoja de "Anotar". Así un dato malicioso o mal leído nunca entra solo a tus cuentas.
 */
data class MovimientoPropuesto(
    val tipo: String,
    val centimos: Long,
    val categoriaSugerida: String?,
    val nota: String,
    val dia: LocalDate,
    /** Id de la [FuenteDeMovimientos] que lo propuso. */
    val origen: String,
)

interface FuenteDeMovimientos {
    val id: String
    val nombre: String
    /** Permisos de Android que necesita. Una fuente nueva con permisos debe ser opcional y explicarse antes de pedirlos. */
    val permisos: List<String>
}

/** Primera fuente: el atajo "Anotar gasto" al mantener presionado el ícono. No necesita permisos ni datos. */
object AtajoDelLauncher : FuenteDeMovimientos {
    const val ACCION = "pe.sencillo.ANOTAR"
    override val id = "atajo"
    override val nombre = "Atajo «Anotar gasto»"
    override val permisos = emptyList<String>()
}

object ValidadorDePropuestas {
    const val MAX_CENTIMOS = 100_000_000L // S/ 1 millón por movimiento

    sealed interface Resultado {
        data class Valida(val propuesta: MovimientoPropuesto) : Resultado
        data class Rechazada(val motivo: String) : Resultado
    }

    fun validar(p: MovimientoPropuesto, hoy: LocalDate): Resultado {
        if (p.tipo !in Tipo.todos) return Resultado.Rechazada("Tipo desconocido")
        if (p.centimos !in 1..MAX_CENTIMOS) return Resultado.Rechazada("Monto fuera de rango")
        if (p.dia.isAfter(hoy.plusDays(1)) || p.dia.isBefore(hoy.minusYears(2))) return Resultado.Rechazada("Fecha fuera de rango")
        val categoria = p.categoriaSugerida?.takeIf { c ->
            when (p.tipo) {
                Tipo.GASTO -> Catalogo.categoria(c) != null
                Tipo.INGRESO -> Catalogo.ingresos.any { it.id == c }
                else -> c == Catalogo.AHORRO_LIBRE
            }
        }
        return Resultado.Valida(p.copy(categoriaSugerida = categoria, nota = Respaldo.texto(p.nota, 60)))
    }
}
