package ru.lopon.platform


interface FileStorage {
    suspend fun readText(path: String): Result<String>

    suspend fun writeText(path: String, content: String): Result<Unit>

    suspend fun exists(path: String): Boolean

    suspend fun delete(path: String): Result<Unit>

    suspend fun listFiles(directory: String): List<String>
}

