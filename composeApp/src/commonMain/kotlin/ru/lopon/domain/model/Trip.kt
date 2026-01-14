package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель поездки, содержащая метаданные и агрегированную информацию.
 *
 * @property id Уникальный идентификатор поездки (UUID).
 * @property startTimeUtc Время начала поездки в миллисекундах UTC.
 * @property endTimeUtc Время завершения поездки в миллисекундах UTC, null если поездка активна.
 * @property mode Режим навигации, использованный в поездке.
 * @property distanceMeters Общая пройденная дистанция в метрах.
 */
@Serializable
data class Trip(
    val id: String,
    val startTimeUtc: Long,
    val endTimeUtc: Long? = null,
    val mode: NavigationMode,
    val distanceMeters: Double = 0.0
) {
    /**
     * Проверяет, завершена ли поездка.
     * @return true, если поездка завершена (endTimeUtc != null).
     */
    val isFinished: Boolean
        get() = endTimeUtc != null

    /**
     * Вычисляет продолжительность поездки в миллисекундах.
     * @param currentTimeUtc Текущее время для активных поездок.
     * @return Продолжительность в миллисекундах.
     */
    fun durationMs(currentTimeUtc: Long): Long {
        val end = endTimeUtc ?: currentTimeUtc
        return (end - startTimeUtc).coerceAtLeast(0)
    }
}

