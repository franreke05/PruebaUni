package com.franciscor.pruebauni

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform