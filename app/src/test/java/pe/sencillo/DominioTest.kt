package pe.sencillo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import pe.sencillo.datos.Movimiento
import pe.sencillo.dominio.Analizador
import pe.sencillo.dominio.Catalogo
import pe.sencillo.dominio.Dinero
import pe.sencillo.dominio.Nivel
import pe.sencillo.dominio.Recomendador
import pe.sencillo.dominio.Tipo
import pe.sencillo.ingesta.MovimientoPropuesto
import pe.sencillo.ingesta.ValidadorDePropuestas
import pe.sencillo.seguridad.Cifrado
import pe.sencillo.seguridad.RespaldoInvalido
import java.time.LocalDate
import java.time.YearMonth

class DineroTest {
    @Test fun formatea_en_soles() {
        assertEquals("S/ 1,234.50", Dinero.sol(123450))
        assertEquals("−S/ 12.05", Dinero.sol(-1205))
        assertEquals("S/ 1,235", Dinero.sol0(123450))
    }

    @Test fun lee_lo_que_se_escribe_en_el_teclado() {
        assertEquals(1250L, Dinero.deTexto("12.5"))
        assertEquals(1205L, Dinero.deTexto("12.05"))
        assertEquals(1200L, Dinero.deTexto("12."))
        assertEquals(250000L, Dinero.deTexto("2,500"))
        assertNull(Dinero.deTexto("12.345"))
        assertNull(Dinero.deTexto("abc"))
        assertNull(Dinero.deTexto("-5"))
        assertNull(Dinero.deTexto("12345678"))
    }

    @Test fun vuelve_a_texto_editable() {
        assertEquals("12.5", Dinero.aTexto(1250))
        assertEquals("12", Dinero.aTexto(1200))
        assertEquals("12.05", Dinero.aTexto(1205))
    }

    @Test fun sobres_base_respetan_la_meta_de_ahorro() {
        val s = Catalogo.sobresBase(300000, 20)
        val total = s.values.sum()
        assertTrue("total $total", total in 235000..245000) // 80% de S/ 3000, con redondeo a decenas
    }
}

class AnalisisTest {
    private val octubre = YearMonth.of(2026, 10)
    private fun gasto(dia: Int, soles: Int, cat: String, pago: String = "yape") =
        Movimiento(tipo = Tipo.GASTO, centimos = soles * 100L, categoria = cat, nota = "", dia = octubre.atDay(dia).toEpochDay(), pago = pago)

    @Test fun calcula_disponible_proyeccion_y_hormiga() {
        val movs = listOf(
            gasto(1, 750, "vivienda", "debito"),
            gasto(2, 12, "comida"), gasto(3, 12, "comida"), gasto(4, 12, "comida"), gasto(5, 8, "comida"),
            gasto(6, 100, "mercado", "efectivo"),
            Movimiento(tipo = Tipo.AHORRO, centimos = 30000, categoria = "libre", nota = "", dia = octubre.atDay(1).toEpochDay(), pago = null),
            Movimiento(tipo = Tipo.INGRESO, centimos = 20000, categoria = "cachuelo", nota = "", dia = octubre.atDay(5).toEpochDay(), pago = null),
        )
        val a = Analizador.analizar(octubre, LocalDate.of(2026, 10, 10), 300000, 20, movs, null)
        assertEquals(320000L, a.ingreso)
        assertEquals(89400L, a.gastado)
        assertEquals(320000L - 89400L - 30000L, a.disponible)
        // fijos (vivienda) se cuentan una vez; variables (144) al ritmo de 10 de 31 días
        assertEquals(75000L + 14400L * 31 / 10, a.proyeccion)
        assertEquals(256000L, a.limite)
        assertEquals(22, a.diasRestantes)
        assertEquals(4, a.hormiga.size)
        assertEquals(89400L, a.acumulado[10])
        assertEquals(75000L, a.acumulado[1])
    }

    @Test fun mes_futuro_no_tiene_dia() {
        val a = Analizador.analizar(YearMonth.of(2026, 11), LocalDate.of(2026, 10, 28), 300000, 20, emptyList(), null)
        assertEquals(0, a.dia)
        assertEquals(Nivel.BIEN, Analizador.estado(a).nivel)
    }

    @Test fun recomienda_cuando_un_sobre_se_pasa() {
        val movs = (1..20).map { gasto(it, 15, "comida") }
        val a = Analizador.analizar(octubre, LocalDate.of(2026, 10, 20), 300000, 20, movs, null)
        val r = Recomendador.recomendar(a, mapOf("comida" to 20000L), 30, tieneFondoEmergencia = false)
        assertTrue(r.any { it.titulo.startsWith("Comida fuera se pasó") })
        assertTrue(r.any { it.titulo.contains("fondo de emergencia") })
        assertEquals(Nivel.ALERTA, r.first().nivel)
    }
}

class CifradoTest {
    private val clave = "una clave larga".toCharArray()

    @Test fun ida_y_vuelta() {
        val datos = "{\"app\":\"sencillo\"}".toByteArray()
        val c = Cifrado.cifrar(datos, clave.copyOf(), iteraciones = 100_000)
        assertArrayEquals(datos, Cifrado.descifrar(c, clave.copyOf()))
    }

    @Test fun clave_incorrecta_falla() {
        val c = Cifrado.cifrar("hola".toByteArray(), clave.copyOf(), iteraciones = 100_000)
        try { Cifrado.descifrar(c, "otra clave".toCharArray()); fail() } catch (e: RespaldoInvalido) { }
    }

    @Test fun archivo_modificado_falla() {
        val c = Cifrado.cifrar("hola".toByteArray(), clave.copyOf(), iteraciones = 100_000)
        c[c.size - 1] = (c[c.size - 1].toInt() xor 1).toByte()
        try { Cifrado.descifrar(c, clave.copyOf()); fail() } catch (e: RespaldoInvalido) { }
        val d = Cifrado.cifrar("hola".toByteArray(), clave.copyOf(), iteraciones = 100_000)
        d[6] = (d[6].toInt() xor 1).toByte() // cabecera (iteraciones)
        try { Cifrado.descifrar(d, clave.copyOf()); fail() } catch (e: RespaldoInvalido) { }
    }

    @Test fun rechaza_basura() {
        try { Cifrado.descifrar(ByteArray(10), clave); fail() } catch (e: RespaldoInvalido) { }
    }
}

class IngestaTest {
    private val hoy = LocalDate.of(2026, 10, 28)

    @Test fun limpia_y_valida_propuestas() {
        val ok = ValidadorDePropuestas.validar(
            MovimientoPropuesto(Tipo.GASTO, 1250, "comida", "Menú‮\n", hoy, "prueba"), hoy,
        )
        ok as ValidadorDePropuestas.Resultado.Valida
        assertEquals("Menú", ok.propuesta.nota)
        assertEquals("comida", ok.propuesta.categoriaSugerida)

        val catRara = ValidadorDePropuestas.validar(MovimientoPropuesto(Tipo.GASTO, 100, "../../x", "", hoy, "p"), hoy)
        assertNull((catRara as ValidadorDePropuestas.Resultado.Valida).propuesta.categoriaSugerida)

        assertTrue(ValidadorDePropuestas.validar(MovimientoPropuesto(Tipo.GASTO, 0, null, "", hoy, "p"), hoy) is ValidadorDePropuestas.Resultado.Rechazada)
        assertTrue(ValidadorDePropuestas.validar(MovimientoPropuesto("X", 100, null, "", hoy, "p"), hoy) is ValidadorDePropuestas.Resultado.Rechazada)
        assertTrue(ValidadorDePropuestas.validar(MovimientoPropuesto(Tipo.GASTO, 100, null, "", hoy.plusDays(5), "p"), hoy) is ValidadorDePropuestas.Resultado.Rechazada)
    }
}
