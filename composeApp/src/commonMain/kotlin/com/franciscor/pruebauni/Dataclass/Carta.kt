package com.franciscor.pruebauni.Dataclass

data class Carta(
    val imagen: String,
    val numero: Int? = null,
    val color: String,
    val isSpecial: Boolean = false,
    val specialType: CartaEspecial? = null,
)

enum class CartaEspecial {
    MAS_DOS,
    MAS_CUATRO,
    REVERSA,
    CAMBIO_COLOR
}
