@file:OptIn(ExperimentalTextApi::class)

package pe.sencillo.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pe.sencillo.R

/*
 * Identidad: una libreta de cuentas bien llevada. Superficies planas sobre papel, sin gradientes,
 * sombras ni adornos. Las secciones se separan con espacio y títulos, no con cajas. El color solo
 * comunica: verde azulado para acciones, ámbar y rojo para avisos, y las series de los gráficos.
 * Lo único que levanta la voz es el monto de lo que te queda.
 */
@Immutable
data class Paleta(
    val papel: Color, val carta: Color, val texto: Color, val texto2: Color, val tenue: Color,
    val linea: Color, val linea2: Color,
    val marca: Color, val sobreMarca: Color, val marcaSuave: Color,
    val sol: Color, val solClaro: Color, val sobreSol: Color,
    val billete: Color, val billete2: Color, val billeteLinea: Color, val billeteTexto: Color,
    // Series de gráficos (validadas para daltonismo) y estados.
    val nec: Color, val gus: Color, val aho: Color, val pista: Color,
    val bien: Color, val bienTexto: Color, val ojo: Color, val ojoTexto: Color, val alerta: Color, val alertaTexto: Color,
    val oscuro: Boolean,
)

val PaletaClara = Paleta(
    papel = Color(0xFFEEF2EE), carta = Color(0xFFFFFFFF), texto = Color(0xFF13221E), texto2 = Color(0xFF47554F),
    tenue = Color(0xFF5E6B66), linea = Color(0xFFDCE3DE), linea2 = Color(0xFFC3CDC7),
    marca = Color(0xFF0F5A52), sobreMarca = Color(0xFFFFFFFF), marcaSuave = Color(0xFFD9EAE4),
    sol = Color(0xFFB8861E), solClaro = Color(0xFFE4B850), sobreSol = Color(0xFF2A1D03),
    billete = Color(0xFFDFEDE6), billete2 = Color(0xFFCFE3DA), billeteLinea = Color(0x330F5A52), billeteTexto = Color(0xFF0A3D37),
    nec = Color(0xFF2A78D6), gus = Color(0xFFEB6834), aho = Color(0xFF1BAF7A), pista = Color(0xFFE4E9E5),
    bien = Color(0xFF0CA30C), bienTexto = Color(0xFF006300), ojo = Color(0xFFFAB219), ojoTexto = Color(0xFF8A5A00),
    alerta = Color(0xFFD03B3B), alertaTexto = Color(0xFFB42626),
    oscuro = false,
)

val PaletaOscura = Paleta(
    papel = Color(0xFF0C1311), carta = Color(0xFF141D1A), texto = Color(0xFFE6EEEA), texto2 = Color(0xFFA6B3AD),
    tenue = Color(0xFF7F8C87), linea = Color(0xFF22302B), linea2 = Color(0xFF33423C),
    marca = Color(0xFF5CC0AF), sobreMarca = Color(0xFF04201B), marcaSuave = Color(0xFF16332E),
    sol = Color(0xFFE0B04A), solClaro = Color(0xFFF3CD72), sobreSol = Color(0xFF2A1D03),
    billete = Color(0xFF112C27), billete2 = Color(0xFF163830), billeteLinea = Color(0x2982D7C3), billeteTexto = Color(0xFFD6F0E7),
    nec = Color(0xFF3987E5), gus = Color(0xFFD95926), aho = Color(0xFF199E70), pista = Color(0xFF22302B),
    bien = Color(0xFF0CA30C), bienTexto = Color(0xFF3DC23D), ojo = Color(0xFFFAB219), ojoTexto = Color(0xFFF2C14E),
    alerta = Color(0xFFD03B3B), alertaTexto = Color(0xFFFF7E7E),
    oscuro = true,
)

private val LocalPaleta = staticCompositionLocalOf { PaletaClara }

object Tema {
    val p: Paleta
        @Composable @ReadOnlyComposable get() = LocalPaleta.current
}

private fun archivo(peso: Int, ancho: Float) = FontFamily(
    Font(
        R.font.archivo_var,
        FontWeight(peso),
        variationSettings = FontVariation.Settings(FontVariation.weight(peso), FontVariation.width(ancho)),
    ),
)

private fun albert(peso: Int) = Font(
    R.font.albert_sans_var,
    FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

object Fuentes {
    /** Archivo semiexpandido y pesado: solo para el monto de lo que te queda. Es lo único que levanta la voz. */
    val monto = archivo(800, 112f)
    /** Archivo de ancho normal: títulos de pantalla y de sección. */
    val titulo = archivo(700, 100f)
    /** Archivo para cifras medianas (resúmenes). */
    val cifra = archivo(650, 100f)
    val texto = FontFamily(albert(400), albert(500), albert(600), albert(700))
}

/** Cifras de ancho fijo para que los montos se alineen en listas y tablas. */
const val CIFRAS_TABULARES = "tnum"

@Composable
fun SencilloTema(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) PaletaOscura else PaletaClara
    val esquema = if (p.oscuro) darkColorScheme(
        primary = p.marca, onPrimary = p.sobreMarca, secondary = p.sol, onSecondary = p.sobreSol,
        background = p.papel, onBackground = p.texto, surface = p.carta, onSurface = p.texto,
        surfaceVariant = p.papel, onSurfaceVariant = p.texto2, outline = p.linea2, outlineVariant = p.linea,
        error = p.alertaTexto, surfaceContainerLow = p.carta, surfaceContainerHigh = p.carta, surfaceContainerHighest = p.papel,
    ) else lightColorScheme(
        primary = p.marca, onPrimary = p.sobreMarca, secondary = p.sol, onSecondary = p.sobreSol,
        background = p.papel, onBackground = p.texto, surface = p.carta, onSurface = p.texto,
        surfaceVariant = p.papel, onSurfaceVariant = p.texto2, outline = p.linea2, outlineVariant = p.linea,
        error = p.alertaTexto, surfaceContainerLow = p.carta, surfaceContainerHigh = p.carta, surfaceContainerHighest = p.papel,
    )
    val base = TextStyle(fontFamily = Fuentes.texto, color = p.texto)
    val tipos = Typography(
        bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp, color = p.texto2),
        labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontFamily = Fuentes.titulo, fontSize = 26.sp, lineHeight = 30.sp, color = p.texto),
        titleMedium = TextStyle(fontFamily = Fuentes.titulo, fontSize = 19.sp, lineHeight = 24.sp, color = p.texto),
        titleSmall = TextStyle(fontFamily = Fuentes.titulo, fontSize = 16.sp, lineHeight = 21.sp, color = p.texto),
    )
    CompositionLocalProvider(LocalPaleta provides p) {
        MaterialTheme(colorScheme = esquema, typography = tipos, content = content)
    }
}
