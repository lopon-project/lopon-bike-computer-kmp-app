package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Данные от BLE-датчика колеса (Wheel Data из профиля CSC/Cycling Speed and Cadence).
 *
 * @property cumulativeRevolutions Накопленное количество оборотов колеса с момента подключения датчика.
 * @property timestampUtc Временная метка получения данных в миллисекундах UTC.
 * @property cadence Каденс (обороты педалей в минуту), null если не поддерживается датчиком.
 */
@Serializable
data class SensorReading(
    val cumulativeRevolutions: Long,
    val timestampUtc: Long,
    val cadence: Double? = null
) {
    /**
     * Вычисляет разницу в оборотах относительно предыдущего показания.
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Количество оборотов между показаниями (неотрицательное значение).
     *         Учитывает возможное переполнение счётчика датчика.
     */
    fun revolutionsDelta(previousReading: SensorReading): Long {
        val delta = cumulativeRevolutions - previousReading.cumulativeRevolutions
        // Обрабатываем переполнение счётчика (обычно 16-бит или 32-бит)
        return if (delta >= 0) delta else delta + UInt.MAX_VALUE.toLong() + 1
    }

    /**
     * Вычисляет временной интервал относительно предыдущего показания.
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Интервал в миллисекундах (неотрицательное значение).
     */
    fun timeDeltaMs(previousReading: SensorReading): Long {
        return (timestampUtc - previousReading.timestampUtc).coerceAtLeast(0)
    }
}

