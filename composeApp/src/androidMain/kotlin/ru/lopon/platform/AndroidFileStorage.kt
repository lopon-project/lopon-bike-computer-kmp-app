package ru.lopon.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidFileStorage(
    private val context: Context
) : FileStorage {

    private fun resolveFile(path: String): File = File(context.filesDir, path)

    override suspend fun readText(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = resolveFile(path)
            if (!file.exists()) {
                Result.failure(java.io.FileNotFoundException("File not found: $path"))
            } else {
                Result.success(file.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeText(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveFile(path)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        resolveFile(path).exists()
    }

    override suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveFile(path)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(directory: String): List<String> = withContext(Dispatchers.IO) {
        val dir = resolveFile(directory)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
