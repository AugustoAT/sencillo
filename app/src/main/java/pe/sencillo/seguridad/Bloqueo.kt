package pe.sencillo.seguridad

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Bloqueo de la app con la huella, el rostro o el PIN/patrón del celular.
 * Ahorro+ nunca guarda ni ve la huella ni el PIN: eso lo maneja Android.
 */
object Bloqueo {
    private val autenticadores: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** True si el celular tiene huella, rostro o al menos un bloqueo de pantalla configurado. */
    fun disponible(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(autenticadores) == BiometricManager.BIOMETRIC_SUCCESS

    fun pedir(
        actividad: FragmentActivity,
        alAbrir: () -> Unit,
        alFallar: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = alAbrir()
                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    if (codigo != BiometricPrompt.ERROR_USER_CANCELED &&
                        codigo != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        codigo != BiometricPrompt.ERROR_CANCELED
                    ) alFallar(mensaje.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Abrir Ahorro+")
            .setSubtitle("Usa tu huella, tu rostro o el bloqueo de tu celular")
            .setAllowedAuthenticators(autenticadores)
            .build()
        prompt.authenticate(info)
    }
}
