package com.franciscor.pruebauni.Dataclass

data class baraja(
    val id: String,
    val listaCartas: List<Carta>,
    val numeroCartas: Int,
    var porcentaje_C_especial: Double,
)
