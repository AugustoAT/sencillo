package pe.sencillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.sencillo.dominio.Dinero
import pe.sencillo.seguridad.Bloqueo
import pe.sencillo.ui.tema.Fuentes
import pe.sencillo.ui.tema.Tema

@Composable
fun Bienvenida(onEmpezar: (sueldo: Long, diaPago: Int, ahorroPct: Int, bloqueo: Boolean) -> Unit) {
    val p = Tema.p
    val ctx = LocalContext.current
    val bloqueoDisponible = remember { Bloqueo.disponible(ctx) }
    var sueldo by rememberSaveable { mutableStateOf("") }
    var dia by rememberSaveable { mutableStateOf("30") }
    var ahorro by rememberSaveable { mutableIntStateOf(20) }
    var bloqueo by rememberSaveable { mutableStateOf(bloqueoDisponible) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.papel)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Moneda("S/", 56.dp)
        Text("Sencillo", fontFamily = Fuentes.billete, fontSize = 44.sp, letterSpacing = (-0.03).em)
        Text(
            "Tu sueldo, tus gastos y tu ahorro del mes, en soles. Anota cada gasto en segundos y mira a dónde se va tu plata.",
            fontSize = 17.sp, lineHeight = 24.sp, color = p.texto2,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = sueldo,
            onValueChange = { v -> sueldo = v.filter { it.isDigit() || it == '.' }.take(10); error = null },
            label = { Text("¿Cuánto recibes al mes?") },
            prefix = { Text("S/ ") },
            supportingText = { Text("Tu sueldo neto: lo que te llega después de AFP u ONP.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = dia,
            onValueChange = { v -> dia = v.filter(Char::isDigit).take(2); error = null },
            label = { Text("¿Qué día te pagan?") },
            supportingText = { Text("Por ejemplo 15 si es quincena o 30 si es fin de mes.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            Text("¿Cuánto quieres ahorrar cada mes?", fontWeight = FontWeight.Medium, color = p.texto2)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 15, 20, 30).forEach { v -> Ficha("$v%", ahorro == v, onClick = { ahorro = v }) }
            }
            Text(
                "Lo recomendado es 20%. Si hoy no llegas, empieza con 10% y súbelo luego.",
                fontSize = 13.sp, color = p.tenue, modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (bloqueoDisponible) Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pedir huella o PIN al abrir", fontWeight = FontWeight.SemiBold)
                Text("Para que nadie más vea tus cuentas si presta tu celular.", fontSize = 13.sp, color = p.texto2)
            }
            Switch(bloqueo, { bloqueo = it }, colors = SwitchDefaults.colors(checkedTrackColor = p.marca, checkedThumbColor = p.sobreMarca))
        }
        Tarjeta {
            Row(Modifier.padding(14.dp)) {
                Icon(Iconos.candado, null, tint = p.marca, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tus datos se quedan en este celular. Sencillo no usa internet, no tiene cuentas ni publicidad, y no pide permisos.",
                    fontSize = 14.sp, color = p.texto2,
                )
            }
        }
        error?.let { Text(it, color = p.alertaTexto, fontSize = 14.sp) }
        BotonPrincipal("Armar mis sobres", {
            val s = Dinero.deTexto(sueldo)
            val d = dia.toIntOrNull()
            error = when {
                s == null || s < 10000 -> "Escribe tu sueldo mensual en soles, por ejemplo 2500."
                d == null || d !in 1..31 -> "El día de pago va del 1 al 31."
                else -> null
            }
            if (error == null) onEmpezar(s!!, d!!, ahorro, bloqueo && bloqueoDisponible)
        }, Modifier.fillMaxWidth())
        Text("Repartimos tu sueldo en sobres con la regla 50/30/20. Después los puedes ajustar.", fontSize = 13.sp, color = p.tenue)
    }
}
