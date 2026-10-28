package pe.sencillo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.sencillo.datos.Meta
import pe.sencillo.datos.MetaConAhorro
import pe.sencillo.datos.Movimiento
import pe.sencillo.datos.Perfil
import pe.sencillo.datos.Repositorio
import pe.sencillo.dominio.Analisis
import pe.sencillo.dominio.Analizador
import pe.sencillo.dominio.Estado
import pe.sencillo.dominio.Recomendacion
import pe.sencillo.dominio.Recomendador
import pe.sencillo.seguridad.Cifrado
import pe.sencillo.seguridad.Respaldo
import java.time.LocalDate
import java.time.YearMonth

sealed interface EstadoUi {
    data object Cargando : EstadoUi
    data object SinPerfil : EstadoUi
    data class Listo(
        val perfil: Perfil,
        val sobres: Map<String, Long>,
        val analisis: Analisis,
        val estado: Estado,
        val recomendaciones: List<Recomendacion>,
        val metas: List<MetaConAhorro>,
    ) : EstadoUi
}

enum class Pestana { MES, BOLETA, ANALISIS, METAS }

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val repo: Repositorio) : ViewModel() {

    private fun hoy(): LocalDate = LocalDate.now()

    private val _mes = MutableStateFlow(YearMonth.from(hoy()))
    val mes: StateFlow<YearMonth> = _mes.asStateFlow()

    val pestana = MutableStateFlow(Pestana.MES)

    /** Se usa para el atajo "Anotar gasto" del launcher. */
    val pedirAnotar = MutableStateFlow(false)

    private val movsDelMes = _mes.flatMapLatest { m ->
        combine(repo.movimientosDe(m), repo.movimientosDe(m.minusMonths(1))) { a, b -> Triple(m, a, b) }
    }

    val estado: StateFlow<EstadoUi> = combine(repo.perfil, repo.sobres, movsDelMes, repo.metas) { perfil, sobres, (m, movs, prev), metas ->
        if (perfil == null) return@combine EstadoUi.SinPerfil
        val a = Analizador.analizar(m, hoy(), perfil.sueldo, perfil.ahorroPct, movs, prev)
        val tieneFondo = metas.any { it.meta.nombre.contains("emergencia", ignoreCase = true) }
        EstadoUi.Listo(
            perfil, sobres, a, Analizador.estado(a),
            Recomendador.recomendar(a, sobres, perfil.diaPago, tieneFondo), metas,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    fun irMes(delta: Long) {
        val nuevo = _mes.value.plusMonths(delta)
        if (nuevo <= YearMonth.from(hoy()).plusMonths(1)) _mes.value = nuevo
    }

    fun irA(p: Pestana) { pestana.value = p }

    fun empezar(sueldo: Long, diaPago: Int, ahorroPct: Int, bloqueo: Boolean) = viewModelScope.launch {
        repo.empezar(sueldo, diaPago, ahorroPct, bloqueo)
    }

    fun guardarMovimiento(m: Movimiento) = viewModelScope.launch {
        repo.guardarMovimiento(m)
        val ym = YearMonth.from(LocalDate.ofEpochDay(m.dia))
        if (ym != _mes.value) _mes.value = ym
    }

    fun borrarMovimiento(id: Long) = viewModelScope.launch { repo.borrarMovimiento(id) }

    fun guardarMeta(m: Meta) = viewModelScope.launch { repo.guardarMeta(m) }
    fun borrarMeta(id: Long) = viewModelScope.launch { repo.borrarMeta(id) }

    fun guardarAjustes(p: Perfil, sobres: Map<String, Long>) = viewModelScope.launch {
        repo.guardarPerfil(p)
        repo.guardarSobres(sobres)
    }

    fun cambiarSeguridad(p: Perfil) = viewModelScope.launch { repo.guardarPerfil(p) }

    /** Devuelve los bytes cifrados del respaldo, o null si no hay datos. */
    suspend fun respaldoCifrado(clave: CharArray): ByteArray? = withContext(Dispatchers.Default) {
        val d = repo.exportar() ?: return@withContext null
        val json = Respaldo.aJson(d)
        try { Cifrado.cifrar(json, clave) } finally { json.fill(0) }
    }

    /** Descifra, valida y reemplaza todo. Lanza RespaldoInvalido con un mensaje para la persona. */
    suspend fun restaurar(bytes: ByteArray, clave: CharArray) = withContext(Dispatchers.Default) {
        val json = Cifrado.descifrar(bytes, clave)
        val datos = try { Respaldo.deJson(json) } finally { json.fill(0) }
        repo.importar(datos)
        _mes.value = YearMonth.from(hoy())
    }

    fun borrarTodo() = viewModelScope.launch {
        repo.borrarTodo()
        _mes.value = YearMonth.from(hoy())
        pestana.value = Pestana.MES
    }

    class Fabrica(private val repo: Repositorio) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T
    }
}
