package pe.sencillo

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import pe.sencillo.ingesta.AtajoDelLauncher
import pe.sencillo.seguridad.Bloqueo
import pe.sencillo.ui.AppViewModel
import pe.sencillo.ui.EstadoUi
import pe.sencillo.ui.Raiz
import pe.sencillo.ui.tema.SencilloTema

class MainActivity : FragmentActivity() {

    private val vm: AppViewModel by viewModels { AppViewModel.Fabrica((application as SencilloApp).repositorio) }

    private var desbloqueada by mutableStateOf(false)
    private var salioEn = 0L
    private var pidiendo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Seguro por defecto: sin capturas y con el contenido tapado en "recientes" hasta leer la preferencia.
        // Solo la versión de desarrollo (debug) permite capturas, para poder revisar el diseño.
        if (!BuildConfig.DEBUG) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.estado.collect { e ->
                    val ocultar = !BuildConfig.DEBUG && ((e as? EstadoUi.Listo)?.perfil?.ocultarPantalla ?: true)
                    if (ocultar) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        manejarIntent(intent)
        setContent {
            SencilloTema {
                Raiz(vm, desbloqueada, onDesbloquear = ::pedirDesbloqueo)
            }
        }
    }

    private fun pedirDesbloqueo() {
        if (pidiendo || desbloqueada) return
        if (!Bloqueo.disponible(this)) {
            // Si el celular ya no tiene ningún bloqueo configurado, no hay con qué verificar:
            // se abre, porque cualquiera con el celular en la mano ya tiene acceso a él.
            desbloqueada = true
            return
        }
        pidiendo = true
        Bloqueo.pedir(
            this,
            alAbrir = { pidiendo = false; desbloqueada = true },
            alFallar = { msg -> pidiendo = false; Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
        )
        // Si la persona cancela, el botón "Desbloquear" vuelve a pedirlo.
        window.decorView.postDelayed({ pidiendo = false }, 1500)
    }

    override fun onStop() {
        super.onStop()
        salioEn = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Vuelve a bloquear si la app estuvo más de 30 segundos en segundo plano.
        if (salioEn > 0 && SystemClock.elapsedRealtime() - salioEn > 30_000) desbloqueada = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manejarIntent(intent)
    }

    /** Solo se acepta la acción exacta del atajo. No se lee ningún dato (extras) que venga en el intent. */
    private fun manejarIntent(intent: Intent?) {
        if (intent?.action == AtajoDelLauncher.ACCION) vm.pedirAnotar.value = true
    }
}
