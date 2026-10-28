package pe.sencillo.datos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pe.sencillo.dominio.Catalogo
import pe.sencillo.seguridad.DatosRespaldo
import java.time.YearMonth

class Repositorio(private val dao: SencilloDao) {
    val perfil: Flow<Perfil?> = dao.perfil()
    val sobres: Flow<Map<String, Long>> = dao.sobres().map { l -> l.associate { it.categoria to it.centimos } }

    val metas: Flow<List<MetaConAhorro>> = combine(dao.metas(), dao.ahorrosPorDestino()) { metas, sumas ->
        val porDestino = sumas.associate { it.categoria to it.total }
        metas.map { m -> MetaConAhorro(m, m.inicial + (porDestino["${Catalogo.PREFIJO_META}${m.id}"] ?: 0L)) }
    }

    fun movimientosDe(mes: YearMonth): Flow<List<Movimiento>> =
        dao.movimientos(mes.atDay(1).toEpochDay(), mes.atEndOfMonth().toEpochDay())

    suspend fun empezar(sueldo: Long, diaPago: Int, ahorroPct: Int, bloqueo: Boolean) {
        dao.guardarPerfil(Perfil(sueldo = sueldo, diaPago = diaPago, ahorroPct = ahorroPct, bloqueo = bloqueo, ocultarPantalla = true))
        dao.guardarSobres(Catalogo.sobresBase(sueldo, ahorroPct).map { (k, v) -> Sobre(k, v) })
    }

    suspend fun guardarPerfil(p: Perfil) = dao.guardarPerfil(p)
    suspend fun guardarSobres(s: Map<String, Long>) = dao.guardarSobres(s.map { (k, v) -> Sobre(k, v) })

    suspend fun guardarMovimiento(m: Movimiento) {
        if (m.id == 0L) dao.insertar(m) else dao.actualizar(m)
    }

    suspend fun movimiento(id: Long) = dao.movimiento(id)
    suspend fun borrarMovimiento(id: Long) = dao.borrarMovimiento(id)

    suspend fun guardarMeta(m: Meta): Long = if (m.id == 0L) dao.insertarMeta(m) else { dao.actualizarMeta(m); m.id }
    suspend fun borrarMeta(id: Long) = dao.borrarMeta(id)

    suspend fun exportar(): DatosRespaldo? {
        val p = dao.perfilAhora() ?: return null
        return DatosRespaldo(p, dao.todosSobres(), dao.todasMetas(), dao.todosMovimientos())
    }

    suspend fun importar(d: DatosRespaldo) = dao.reemplazarTodo(d.perfil, d.sobres, d.metas, d.movimientos)
    suspend fun borrarTodo() = dao.borrarTodo()
}
