package ru.lopon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform