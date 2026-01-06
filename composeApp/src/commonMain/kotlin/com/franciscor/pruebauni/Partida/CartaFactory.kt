package com.franciscor.pruebauni.Partida

import com.franciscor.pruebauni.Dataclass.Carta
import com.franciscor.pruebauni.Dataclass.CartaEspecial
import kotlin.random.Random

private val cartaColores = listOf("rojo", "amarillo", "verde", "azul")

fun crearCartaAleatoria(specialChancePercent: Int = 25): Carta {
    val chance = specialChancePercent.coerceIn(0, 100)
    val isSpecial = Random.nextInt(100) < chance
    if (!isSpecial) {
        return Carta(
            imagen = "",
            numero = Random.nextInt(0, 10),
            color = cartaColores.random(),
            isSpecial = false,
            specialType = null
        )
    }

    val specialType = CartaEspecial.values().random()
    val color = when (specialType) {
        CartaEspecial.MAS_DOS,
        CartaEspecial.REVERSA -> cartaColores.random()
        CartaEspecial.MAS_CUATRO,
        CartaEspecial.CAMBIO_COLOR -> "wild"
    }
    return Carta(
        imagen = "",
        numero = null,
        color = color,
        isSpecial = true,
        specialType = specialType
    )
}
