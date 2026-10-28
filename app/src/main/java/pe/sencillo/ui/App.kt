package pe.sencillo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pe.sencillo.dominio.Tipo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun Raiz(vm: AppViewModel, desbloqueada: Boolean, onDesbloquear: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoUi.Cargando -> Box(Modifier.fillMaxSize().background(Tema.p.papel))
        EstadoUi.SinPerfil -> Bienvenida { s, d, a, b -> vm.empezar(s, d, a, b) }
        is EstadoUi.Listo ->
            if (e.perfil.bloqueo && !desbloqueada) PantallaBloqueo(onDesbloquear)
            else Principal(vm, e)
    }
}

@Composable
private fun PantallaBloqueo(onDesbloquear: () -> Unit) {
    val p = Tema.p
    LaunchedEffect(Unit) { onDesbloquear() }
    Column(
        Modifier.fillMaxSize().background(p.papel).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Moneda("S/", 64.dp)
        Spacer(Modifier.height(18.dp))
        Text("Sencillo está bloqueado", style = MaterialTheme.typography.titleMedium)
        Text("Usa tu huella, tu rostro o el bloqueo de tu celular.", color = p.texto2, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        BotonPrincipal("Desbloquear", onDesbloquear)
    }
}

private fun diaPorDefecto(mes: YearMonth): LocalDate {
    val hoy = LocalDate.now()
    val actual = YearMonth.from(hoy)
    return when {
        mes == actual -> hoy
        mes.isBefore(actual) -> mes.atEndOfMonth()
        else -> mes.atDay(1)
    }
}

@Composable
private fun Principal(vm: AppViewModel, e: EstadoUi.Listo) {
    val p = Tema.p
    val pestana by vm.pestana.collectAsStateWithLifecycle()
    val mes by vm.mes.collectAsStateWithLifecycle()
    val pedirAnotar by vm.pedirAnotar.collectAsStateWithLifecycle()
    var hoja by remember { mutableStateOf<Borrador?>(null) }
    var ajustes by rememberSaveable { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var irARecos by remember { mutableIntStateOf(0) }
    val listaAnalisis = rememberLazyListState()

    LaunchedEffect(pedirAnotar) {
        if (pedirAnotar) { hoja = Borrador(dia = diaPorDefecto(mes)); vm.pedirAnotar.value = false }
    }
    LaunchedEffect(irARecos) { if (irARecos > 0) { delay(60); listaAnalisis.animateScrollToItem(5) } }
    LaunchedEffect(aviso) { if (aviso != null) { delay(3000); aviso = null } }
    BackHandler(enabled = ajustes != null) { ajustes = null }
    BackHandler(enabled = ajustes == null && pestana != Pestana.MES) { vm.irA(Pestana.MES) }

    Box(Modifier.fillMaxSize().background(p.papel)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sencillo", fontFamily = Fuentes.billete, fontSize = 22.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { ajustes = "todo" }) { Icon(Iconos.ajustes, "Ajustes", tint = p.texto2) }
            }
            if (pestana != Pestana.METAS) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${nombreMes(mes).replaceFirstChar { it.uppercase() }} ${mes.year}",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                    )
                    BotonRedondo(Iconos.izquierda, "Mes anterior") { vm.irMes(-1) }
                    Spacer(Modifier.width(6.dp))
                    BotonRedondo(Iconos.derecha, "Mes siguiente", habilitado = mes < YearMonth.now().plusMonths(1)) { vm.irMes(1) }
                }
            }
            Box(Modifier.weight(1f)) {
                val editar = { m: pe.sencillo.datos.Movimiento -> hoja = Borrador.de(m) }
                when (pestana) {
                    Pestana.MES -> PantallaMes(
                        e, onEditar = editar,
                        onVerRecomendaciones = { vm.irA(Pestana.ANALISIS); irARecos++ },
                        onEditarSobres = { ajustes = "sobres" },
                        onVerBoleta = { vm.irA(Pestana.BOLETA) },
                    )
                    Pestana.BOLETA -> PantallaBoleta(e, onEditar = editar)
                    Pestana.ANALISIS -> PantallaAnalisis(e, listaAnalisis)
                    Pestana.METAS -> PantallaMetas(
                        e,
                        onAportar = { id -> hoja = Borrador(tipo = Tipo.AHORRO, categoria = "meta:$id", dia = LocalDate.now()) },
                        onGuardar = { vm.guardarMeta(it); aviso = "Meta guardada" },
                        onBorrar = { vm.borrarMeta(it); aviso = "Meta borrada" },
                    )
                }
            }
        }

        BarraInferior(pestana, vm::irA, onAnotar = { hoja = Borrador(dia = diaPorDefecto(mes)) }, Modifier.align(Alignment.BottomCenter))

        aviso?.let { texto ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(p.texto)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) { Text(texto, color = p.papel, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        }

        ajustes?.let { modo ->
            PantallaAjustes(e, vm, enfocarSobres = modo == "sobres", avisar = { aviso = it }, onCerrar = { ajustes = null })
        }
    }

    hoja?.let { b ->
        HojaAnotar(
            inicial = b,
            metas = e.metas,
            onGuardar = { m ->
                vm.guardarMovimiento(m)
                hoja = null
                aviso = if (b.id != 0L) "Cambios guardados" else when (m.tipo) {
                    Tipo.GASTO -> "Anotado: ${pe.sencillo.dominio.Dinero.sol(m.centimos)}"
                    Tipo.INGRESO -> "Ingreso anotado"
                    else -> "Aporte guardado en tu alcancía"
                }
            },
            onBorrar = { id -> vm.borrarMovimiento(id); hoja = null; aviso = "Movimiento borrado" },
            onCerrar = { hoja = null },
        )
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, habilitado: Boolean = true, onClick: () -> Unit) {
    val p = Tema.p
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.dp, p.linea, CircleShape)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, descripcion, tint = if (habilitado) p.texto2 else p.linea2, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun BarraInferior(actual: Pestana, onPestana: (Pestana) -> Unit, onAnotar: () -> Unit, modifier: Modifier) {
    val p = Tema.p
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(p.carta)
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Pestanita(Iconos.mes, "Mi mes", actual == Pestana.MES, Modifier.weight(1f)) { onPestana(Pestana.MES) }
            Pestanita(Iconos.boleta, "Boleta", actual == Pestana.BOLETA, Modifier.weight(1f)) { onPestana(Pestana.BOLETA) }
            Spacer(Modifier.width(76.dp))
            Pestanita(Iconos.analisis, "Análisis", actual == Pestana.ANALISIS, Modifier.weight(1f)) { onPestana(Pestana.ANALISIS) }
            Pestanita(Iconos.metas, "Metas", actual == Pestana.METAS, Modifier.weight(1f)) { onPestana(Pestana.METAS) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.linea))
        // La moneda: única acción principal de la app.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(64.dp)
                .shadow(10.dp, CircleShape, ambientColor = p.sol, spotColor = p.sol)
                .clip(CircleShape)
                .background(p.papel)
                .padding(3.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(p.solClaro, p.sol)))
                .border(3.dp, p.solClaro.copy(alpha = .8f), CircleShape)
                .clickable(onClick = onAnotar)
                .semantics { contentDescription = "Anotar movimiento" },
            contentAlignment = Alignment.Center,
        ) { Icon(Iconos.mas, null, tint = p.sobreSol, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun Pestanita(icono: ImageVector, texto: String, activa: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = Tema.p
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { selected = activa }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, null, tint = if (activa) p.marca else p.tenue, modifier = Modifier.size(22.dp))
        Text(texto, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = if (activa) p.texto else p.tenue)
    }
}
