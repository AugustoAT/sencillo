package pe.sencillo.datos

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SencilloDao {
    @Query("SELECT * FROM perfil WHERE id = 1")
    abstract fun perfil(): Flow<Perfil?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    abstract suspend fun perfilAhora(): Perfil?

    @Upsert
    abstract suspend fun guardarPerfil(p: Perfil)

    @Query("SELECT * FROM sobres")
    abstract fun sobres(): Flow<List<Sobre>>

    @Upsert
    abstract suspend fun guardarSobres(s: List<Sobre>)

    // Consultas siempre con parámetros enlazados: nada de armar SQL con texto del usuario.
    @Query("SELECT * FROM movimientos WHERE dia BETWEEN :desde AND :hasta ORDER BY dia, id")
    abstract fun movimientos(desde: Long, hasta: Long): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos WHERE id = :id")
    abstract suspend fun movimiento(id: Long): Movimiento?

    @Insert
    abstract suspend fun insertar(m: Movimiento): Long

    @Update
    abstract suspend fun actualizar(m: Movimiento)

    @Query("DELETE FROM movimientos WHERE id = :id")
    abstract suspend fun borrarMovimiento(id: Long)

    @Query("SELECT * FROM metas ORDER BY id")
    abstract fun metas(): Flow<List<Meta>>

    @Query("SELECT categoria, SUM(centimos) AS total FROM movimientos WHERE tipo = 'A' GROUP BY categoria")
    abstract fun ahorrosPorDestino(): Flow<List<SumaCategoria>>

    @Insert
    abstract suspend fun insertarMeta(m: Meta): Long

    @Update
    abstract suspend fun actualizarMeta(m: Meta)

    @Query("DELETE FROM metas WHERE id = :id")
    abstract suspend fun borrarMetaFila(id: Long)

    @Query("UPDATE movimientos SET categoria = 'libre' WHERE tipo = 'A' AND categoria = :destino")
    abstract suspend fun liberarAportes(destino: String)

    /** Al borrar una meta, sus aportes no se pierden: pasan a "Ahorro libre". */
    @Transaction
    open suspend fun borrarMeta(id: Long) {
        liberarAportes("meta:$id")
        borrarMetaFila(id)
    }

    // --- respaldo ---
    @Query("SELECT * FROM movimientos ORDER BY dia, id")
    abstract suspend fun todosMovimientos(): List<Movimiento>

    @Query("SELECT * FROM metas")
    abstract suspend fun todasMetas(): List<Meta>

    @Query("SELECT * FROM sobres")
    abstract suspend fun todosSobres(): List<Sobre>

    @Query("DELETE FROM movimientos") abstract suspend fun vaciarMovimientos()
    @Query("DELETE FROM metas") abstract suspend fun vaciarMetas()
    @Query("DELETE FROM sobres") abstract suspend fun vaciarSobres()
    @Query("DELETE FROM perfil") abstract suspend fun vaciarPerfil()

    @Insert abstract suspend fun insertarMovimientos(l: List<Movimiento>)
    @Insert abstract suspend fun insertarMetas(l: List<Meta>)

    @Transaction
    open suspend fun borrarTodo() {
        vaciarMovimientos(); vaciarMetas(); vaciarSobres(); vaciarPerfil()
    }

    @Transaction
    open suspend fun reemplazarTodo(perfil: Perfil, sobres: List<Sobre>, metas: List<Meta>, movs: List<Movimiento>) {
        borrarTodo()
        guardarPerfil(perfil)
        guardarSobres(sobres)
        insertarMetas(metas)
        insertarMovimientos(movs)
    }
}

@Database(
    entities = [Perfil::class, Sobre::class, Movimiento::class, Meta::class],
    version = 1,
    exportSchema = true,
)
abstract class BaseDatos : RoomDatabase() {
    abstract fun dao(): SencilloDao

    companion object {
        fun crear(ctx: Context): BaseDatos =
            Room.databaseBuilder(ctx.applicationContext, BaseDatos::class.java, "sencillo.db").build()
    }
}
