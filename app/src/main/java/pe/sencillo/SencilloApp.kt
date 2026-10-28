package pe.sencillo

import android.app.Application
import pe.sencillo.datos.BaseDatos
import pe.sencillo.datos.Repositorio

class SencilloApp : Application() {
    val repositorio: Repositorio by lazy { Repositorio(BaseDatos.crear(this).dao()) }
}
