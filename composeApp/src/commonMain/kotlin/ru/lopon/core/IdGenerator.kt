package ru.lopon.core

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


// Для будущего тестирования
interface IdGenerator {
    fun generateId(): String
}

class UuidGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun generateId(): String = Uuid.random().toString()
}

