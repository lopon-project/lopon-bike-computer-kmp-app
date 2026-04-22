package ru.lopon.core

class FakeTimeProvider(var time: Long = 0L) : TimeProvider {
    override fun currentTimeMillis(): Long = time

    fun advance(ms: Long) {
        time += ms
    }
}

