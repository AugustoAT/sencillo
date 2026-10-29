package pe.sencillo.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.sencillo.ui.tema.Tema
import kotlin.math.roundToInt

/** Las zonas que el tutorial puede resaltar. */
object Zona {
    const val SALDO = "saldo"
    const val ANOTAR = "anotar"
    const val LIMITES = "limites"
    const val ANALISIS = "analisis"
}

private data class Paso(val zona: String, val titulo: String, val texto: String)

private val PASOS = listOf(
    Paso(Zona.SALDO, "Lo que te queda", "Es tu sueldo del mes menos lo que ya gastaste y ahorraste. Cambia cada vez que anotas algo."),
    Paso(Zona.ANOTAR, "Anota en el momento", "Cada vez que gastes, recibas plata o ahorres, toca Anotar. Pones el monto, eliges en qué fue y listo."),
    Paso(Zona.LIMITES, "Tus límites por categoría", "Es lo máximo que quieres gastar en cada cosa al mes. La barra se pone ámbar cuando te acercas y roja si te pasas. Los cambias en Editar."),
    Paso(Zona.ANALISIS, "Mira cómo vas", "En Análisis ves a dónde se fue tu plata y sugerencias para ahorrar más."),
)

/** Estado del tutorial: dónde está cada zona en pantalla y en qué paso vamos (-1 = cerrado). */
class EstadoTutorial {
    val zonas = mutableStateMapOf<String, Rect>()
    var paso by mutableIntStateOf(-1)
    val activo: Boolean get() = paso >= 0
    fun empezar() { paso = 0 }
}

val LocalTutorial = staticCompositionLocalOf<EstadoTutorial?> { null }

/** Registra este elemento como una zona que el tutorial puede resaltar. */
fun Modifier.zonaTutorial(clave: String): Modifier = composed {
    val t = LocalTutorial.current
    if (t == null) this else this.onGloballyPositioned { t.zonas[clave] = it.boundsInRoot() }
}

/** Preferencia local (privada de la app, fuera de cualquier respaldo en la nube). */
object PreferenciasTutorial {
    private const val ARCHIVO = "interfaz"
    private const val CLAVE = "tutorial_visto"
    fun visto(ctx: Context) = ctx.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean(CLAVE, false)
    fun marcarVisto(ctx: Context) = ctx.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, true).apply()
}

@Composable
fun CapaTutorial(t: EstadoTutorial, onTerminar: () -> Unit) {
    if (!t.activo) return
    val p = Tema.p
    val paso = PASOS[t.paso]
    val ultimo = t.paso == PASOS.lastIndex
    val margen = with(LocalDensity.current) { 8.dp.toPx() }
    BackHandler { onTerminar() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Bloquea los toques a la app mientras se muestra el tutorial.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val alto = constraints.maxHeight.toFloat()
        val zona = t.zonas[paso.zona]?.takeIf { it.top >= 0f && it.bottom <= alto && it.height > 0f }
        val hueco = zona?.let { Rect(it.left - margen, it.top - margen, it.right + margen, it.bottom + margen) }

        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = .62f))
            hueco?.let {
                drawRoundRect(
                    Color.Transparent, topLeft = it.topLeft, size = it.size,
                    cornerRadius = CornerRadius(14.dp.toPx()), blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    p.sobreMarca, topLeft = it.topLeft, size = it.size,
                    cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(2.dp.toPx()),
                )
            }
        }

        // La tarjeta va debajo de la zona si esta queda arriba, y encima si queda abajo.
        var altoTarjeta by remember { mutableIntStateOf(0) }
        val separacion = with(LocalDensity.current) { 12.dp.toPx() }
        val y = when {
            hueco == null -> (alto - altoTarjeta) / 2f
            hueco.center.y < alto / 2f -> hueco.bottom + separacion
            else -> hueco.top - separacion - altoTarjeta
        }.coerceIn(0f, maxOf(0f, alto - altoTarjeta))

        Column(
            Modifier
                .offset { IntOffset(0, y.roundToInt()) }
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .onSizeChanged { altoTarjeta = it.height }
                .clip(RoundedCornerShape(16.dp))
                .background(p.carta)
                .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 6.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text("${t.paso + 1} de ${PASOS.size}", fontSize = 12.5.sp, color = p.texto2)
            Text(paso.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
            Text(paso.texto, fontSize = 15.sp, color = p.texto2, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                if (!ultimo) TextButton(onClick = onTerminar) { Text("Saltar", color = p.texto2) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { if (ultimo) onTerminar() else t.paso++ }) {
                    Text(if (ultimo) "Empezar a usarla" else "Siguiente", color = p.marca, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
