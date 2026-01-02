package com.franciscor.pruebauni.Dataclass

data class Carta(
    val id: String,
    var numero: Int?=0,
    var color : String?="",
    var isReverse : Boolean? = false,
    var isSpecial : Boolean? = false,
)
