package pe.sencillo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Íconos de trazo propios (24x24), para no cargar la librería completa de íconos. */
object Iconos {
    private fun trazo(nombre: String, vararg trazos: String, grosor: Float = 1.8f): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            trazos.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = grosor,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private const val CIRCULO = "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0"

    val mes = trazo("mes", "M5.5,6h13a2.5,2.5 0 0,1 2.5,2.5v7a2.5,2.5 0 0,1 -2.5,2.5h-13a2.5,2.5 0 0,1 -2.5,-2.5v-7a2.5,2.5 0 0,1 2.5,-2.5z", "M9.4,12a2.6,2.6 0 1,0 5.2,0a2.6,2.6 0 1,0 -5.2,0", "M6.5,9.5v0.01M17.5,14.5v0.01")
    val boleta = trazo("boleta", "M6,3h12v18l-2,-1.4l-2,1.4l-2,-1.4l-2,1.4l-2,-1.4l-2,1.4z", "M9,8h6M9,12h6M9,16h3")
    val analisis = trazo("analisis", "M4,19h16", "M4,15l5,-5l4,3l7,-7", "M16,6h4v4")
    val metas = trazo("metas", "M5,11a7,6 0 0,1 13.5,-1.5H20v4h-1.6A7,6 0 0,1 16,17v2.5h-2.5V18h-3v1.5H8V17a7,6 0 0,1 -3,-6z", "M10,7.5h4")
    val mas = trazo("mas", "M12,5v14M5,12h14", grosor = 2.6f)
    val ajustes = trazo("ajustes", "M4,7h10M18,7h2M4,17h4M12,17h8", "M14,7a2,2 0 1,0 4,0a2,2 0 1,0 -4,0", "M8,17a2,2 0 1,0 4,0a2,2 0 1,0 -4,0")
    val izquierda = trazo("izquierda", "M15,6l-6,6l6,6", grosor = 2f)
    val derecha = trazo("derecha", "M9,6l6,6l-6,6", grosor = 2f)
    val cerrar = trazo("cerrar", "M6,6l12,12M18,6L6,18", grosor = 2f)
    val retroceso = trazo("retroceso", "M9,6h11v12H9l-6,-6z", "M12.5,9.5l5,5M17.5,9.5l-5,5")
    val alerta = trazo("alerta", "M12,3l9.5,17h-19z", "M12,10v4M12,17.5v0.01", grosor = 2f)
    val ojo = trazo("ojo", CIRCULO, "M12,7.5v5.5M12,16.5v0.01", grosor = 2f)
    val bien = trazo("bien", CIRCULO, "M8,12.5l2.7,2.7L16,9.8", grosor = 2f)
    val candado = trazo("candado", "M6,11h12v9H6z", "M8.5,11V8a3.5,3.5 0 0,1 7,0v3", "M12,15v2")
}
