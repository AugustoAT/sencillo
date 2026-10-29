package pe.sencillo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Tipo
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
        Modifier.fillMaxSize().background(p.papel).statusBarsPadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MarcaTexto(32.sp)
        Text("Tus cuentas están protegidas.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
        Text("Usa tu huella, tu rostro o el bloqueo de tu celular para entrar.", color = p.texto2, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        BotonPrincipal("Desbloquear", onDesbloquear, Modifier.padding(top = 24.dp))
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
    val ctx = LocalContext.current
    val pestana by vm.pestana.collectAsStateWithLifecycle()
    val mes by vm.mes.collectAsStateWithLifecycle()
    val pedirAnotar by vm.pedirAnotar.collectAsStateWithLifecycle()
    var hoja by remember { mutableStateOf<Borrador?>(null) }
    var ajustes by rememberSaveable { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var irARecos by remember { mutableIntStateOf(0) }
    val listaAnalisis = rememberLazyListState()
    val tutorial = remember { EstadoTutorial() }

    // La primera vez, después de la bienvenida, se muestra el tutorial.
    LaunchedEffect(Unit) {
        if (!PreferenciasTutorial.visto(ctx)) { delay(400); vm.irA(Pestana.MES); tutorial.empezar() }
    }
    LaunchedEffect(pedirAnotar) {
        if (pedirAnotar) { hoja = Borrador(dia = diaPorDefecto(mes)); vm.pedirAnotar.value = false }
    }
    LaunchedEffect(irARecos) { if (irARecos > 0) { delay(60); listaAnalisis.animateScrollToItem(5) } }
    LaunchedEffect(aviso) { if (aviso != null) { delay(3000); aviso = null } }
    BackHandler(enabled = ajustes != null) { ajustes = null }
    BackHandler(enabled = ajustes == null && pestana != Pestana.MES) { vm.irA(Pestana.MES) }

    CompositionLocalProvider(LocalTutorial provides tutorial) {
        Box(Modifier.fillMaxSize().background(p.papel)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MarcaTexto()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { ajustes = "todo" }) { Icon(Iconos.ajustes, "Ajustes", tint = p.texto2) }
                }
                if (pestana != Pestana.METAS) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${nombreMes(mes).replaceFirstChar { it.uppercase() }} ${mes.year}",
                            style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.irMes(-1) }) { Icon(Iconos.izquierda, "Mes anterior", tint = p.texto) }
                        val puedeSeguir = mes < YearMonth.now().plusMonths(1)
                        IconButton(onClick = { vm.irMes(1) }, enabled = puedeSeguir) {
                            Icon(Iconos.derecha, "Mes siguiente", tint = if (puedeSeguir) p.texto else p.linea2)
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    val editar = { m: pe.sencillo.datos.Movimiento -> hoja = Borrador.de(m) }
                    when (pestana) {
                        Pestana.MES -> PantallaMes(
                            e, onEditar = editar,
                            onVerRecomendaciones = { vm.irA(Pestana.ANALISIS); irARecos++ },
                            onEditarLimites = { ajustes = "limites" },
                            onVerMovimientos = { vm.irA(Pestana.MOVIMIENTOS) },
                        )
                        Pestana.MOVIMIENTOS -> PantallaMovimientos(e, onEditar = editar)
                        Pestana.ANALISIS -> PantallaAnalisis(e, listaAnalisis)
                        Pestana.METAS -> PantallaMetas(
                            e,
                            onAportar = { id -> hoja = Borrador(tipo = Tipo.AHORRO, categoria = "meta:$id", dia = LocalDate.now()) },
                            onGuardar = { vm.guardarMeta(it); aviso = "Meta guardada" },
                            onBorrar = { vm.borrarMeta(it); aviso = "Meta borrada" },
                        )
                    }
                }
                BarraInferior(pestana, vm::irA, onAnotar = { hoja = Borrador(dia = diaPorDefecto(mes)) })
            }

            aviso?.let { texto ->
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 92.dp, start = 24.dp, end = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.texto)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(texto, color = p.papel, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }

            ajustes?.let { modo ->
                PantallaAjustes(
                    e, vm, enfocarLimites = modo == "limites", avisar = { aviso = it }, onCerrar = { ajustes = null },
                    onVerTutorial = { ajustes = null; vm.irA(Pestana.MES); tutorial.empezar() },
                )
            }

            CapaTutorial(tutorial) {
                tutorial.paso = -1
                PreferenciasTutorial.marcarVisto(ctx)
            }
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
                    Tipo.GASTO -> "Gasto anotado: ${Dinero.sol(m.centimos)}"
                    Tipo.INGRESO -> "Ingreso anotado: ${Dinero.sol(m.centimos)}"
                    else -> "Ahorro anotado: ${Dinero.sol(m.centimos)}"
                }
            },
            onBorrar = { id -> vm.borrarMovimiento(id); hoja = null; aviso = "Movimiento borrado" },
            onCerrar = { hoja = null },
        )
    }
}

@Composable
private fun BarraInferior(actual: Pestana, onPestana: (Pestana) -> Unit, onAnotar: () -> Unit) {
    val p = Tema.p
    Column(Modifier.fillMaxWidth().background(p.carta)) {
        Separador()
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Pestanita(Iconos.mes, "Mi mes", actual == Pestana.MES, Modifier.weight(1f)) { onPestana(Pestana.MES) }
            Pestanita(Iconos.movimientos, "Movimientos", actual == Pestana.MOVIMIENTOS, Modifier.weight(1f)) { onPestana(Pestana.MOVIMIENTOS) }
            // La acción principal de la app, con su nombre escrito: anotar un movimiento.
            Column(
                Modifier
                    .weight(1f)
                    .zonaTutorial(Zona.ANOTAR)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAnotar)
                    .semantics { role = Role.Button; contentDescription = "Anotar un gasto, ingreso o ahorro" }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 52.dp, height = 34.dp).clip(CircleShape).background(p.marca),
                    contentAlignment = Alignment.Center,
                ) { Icon(Iconos.mas, null, tint = p.sobreMarca, modifier = Modifier.size(22.dp)) }
                Text("Anotar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.marca, modifier = Modifier.padding(top = 3.dp))
            }
            Pestanita(Iconos.analisis, "Análisis", actual == Pestana.ANALISIS, Modifier.weight(1f).zonaTutorial(Zona.ANALISIS)) { onPestana(Pestana.ANALISIS) }
            Pestanita(Iconos.metas, "Metas", actual == Pestana.METAS, Modifier.weight(1f)) { onPestana(Pestana.METAS) }
        }
    }
}

@Composable
private fun Pestanita(icono: ImageVector, texto: String, activa: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = Tema.p
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { selected = activa; role = Role.Tab }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(CircleShape)
                .background(if (activa) p.marcaSuave else p.carta),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, tint = if (activa) p.marca else p.texto2, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(3.dp))
        Text(texto, fontSize = 12.sp, fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium, color = if (activa) p.texto else p.texto2, maxLines = 1)
    }
}
