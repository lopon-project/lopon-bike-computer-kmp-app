package ru.lopon.core

// Для будущего тестирования
interface TimeProvider {
    fun currentTimeMillis(): Long
}

class RealTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}

