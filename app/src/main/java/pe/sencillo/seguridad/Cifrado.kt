package pe.sencillo.seguridad

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class RespaldoInvalido(mensaje: String) : Exception(mensaje)

/**
 * Cifra los respaldos con una contraseña que elige la persona.
 *
 * Formato: "SENC1" | iteraciones (int) | sal (16 bytes) | iv (12 bytes) | texto cifrado + etiqueta GCM.
 * La cabecera completa va como dato autenticado (AAD): si alguien la cambia, el descifrado falla.
 * Clave: PBKDF2-HMAC-SHA256 (600 000 iteraciones, recomendación OWASP) -> AES-256-GCM.
 */
object Cifrado {
    private val MAGIA = "SENC1".toByteArray(Charsets.US_ASCII)
    const val ITERACIONES = 600_000
    private const val MIN_ITER = 100_000
    private const val MAX_ITER = 5_000_000 // evita que un archivo manipulado congele el celular
    private const val LARGO_SAL = 16
    private const val LARGO_IV = 12
    private val CABECERA = MAGIA.size + 4 + LARGO_SAL + LARGO_IV

    fun cifrar(datos: ByteArray, clave: CharArray, iteraciones: Int = ITERACIONES): ByteArray {
        val azar = SecureRandom()
        val sal = ByteArray(LARGO_SAL).also(azar::nextBytes)
        val iv = ByteArray(LARGO_IV).also(azar::nextBytes)
        val cabecera = ByteBuffer.allocate(CABECERA).put(MAGIA).putInt(iteraciones).put(sal).put(iv).array()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, derivar(clave, sal, iteraciones), GCMParameterSpec(128, iv))
        c.updateAAD(cabecera)
        return cabecera + c.doFinal(datos)
    }

    fun descifrar(archivo: ByteArray, clave: CharArray): ByteArray {
        if (archivo.size < CABECERA + 16) throw RespaldoInvalido("El archivo no es un respaldo de Sencillo.")
        val b = ByteBuffer.wrap(archivo)
        val magia = ByteArray(MAGIA.size).also { b.get(it) }
        if (!magia.contentEquals(MAGIA)) throw RespaldoInvalido("El archivo no es un respaldo de Sencillo.")
        val iter = b.int
        if (iter !in MIN_ITER..MAX_ITER) throw RespaldoInvalido("El respaldo está dañado.")
        val sal = ByteArray(LARGO_SAL).also { b.get(it) }
        val iv = ByteArray(LARGO_IV).also { b.get(it) }
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, derivar(clave, sal, iter), GCMParameterSpec(128, iv))
            c.updateAAD(archivo, 0, CABECERA)
            c.doFinal(archivo, CABECERA, archivo.size - CABECERA)
        } catch (e: GeneralSecurityException) {
            throw RespaldoInvalido("La contraseña no es correcta o el archivo fue modificado.")
        }
    }

    private fun derivar(clave: CharArray, sal: ByteArray, iteraciones: Int): SecretKeySpec {
        val spec = PBEKeySpec(clave, sal, iteraciones, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
