package pe.sencillo.datos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Un gasto, ingreso extra o aporte de ahorro. `dia` es LocalDate.toEpochDay(). */
@Entity(tableName = "movimientos", indices = [Index("dia"), Index("tipo")])
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val centimos: Long,
    val categoria: String,
    val nota: String,
    val dia: Long,
    val pago: String?,
    val creado: Long = System.currentTimeMillis(),
)

fun Movimiento.fecha(): LocalDate = LocalDate.ofEpochDay(dia)

/** Una sola fila (id = 1) con la configuración de la persona. */
@Entity(tableName = "perfil")
data class Perfil(
    @PrimaryKey val id: Int = 1,
    val sueldo: Long,
    val diaPago: Int,
    val ahorroPct: Int,
    val bloqueo: Boolean,
    val ocultarPantalla: Boolean,
    val creado: Long = System.currentTimeMillis(),
)

/** Presupuesto mensual de una categoría de gasto. */
@Entity(tableName = "sobres")
data class Sobre(
    @PrimaryKey val categoria: String,
    val centimos: Long,
)

/** Una alcancía. Lo ahorrado = `inicial` + aportes (movimientos tipo A con categoria "meta:<id>"). */
@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val objetivo: Long,
    val inicial: Long,
    val limite: Long?,
)

data class SumaCategoria(val categoria: String, val total: Long)

data class MetaConAhorro(val meta: Meta, val ahorrado: Long)
