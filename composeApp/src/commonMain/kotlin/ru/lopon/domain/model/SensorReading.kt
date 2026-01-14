package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Данные от BLE-датчика колеса из кастомного 7-байтового пакета Wheel Data.
 *
 * Пакет содержит:
 * - cumulativeRevolutions (4 байта): накопленное количество оборотов
 * - wheelEventTimeUnits (2 байта): время последнего события колеса в единицах 1/1024 секунды (16-bit, с wrap-around)
 * - опционально cadence
 *
 * @property cumulativeRevolutions Накопленное количество оборотов колеса с момента подключения датчика (32-bit).
 * @property wheelEventTimeUnits Время последнего события колеса в единицах 1/1024 секунды (16-bit, 0..65535).
 *                               Используется для расчёта временных интервалов между событиями.
 *                               При переполнении счётчик сбрасывается с 65535 на 0.
 * @property timestampUtc Временная метка получения данных в миллисекундах UTC (опционально, устанавливается платформой).
 *                        Конвертация wheelEventTimeUnits в UTC выполняется на платформенном слое.
 * @property cadence Каденс (обороты педалей в минуту), null если не поддерживается датчиком.
 */
@Serializable
data class SensorReading(
    val cumulativeRevolutions: Long,
    val wheelEventTimeUnits: Int,
    val timestampUtc: Long? = null,
    val cadence: Double? = null
) {
    /**
     * Вычисляет разницу в оборотах относительно предыдущего показания.
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Количество оборотов между показаниями (неотрицательное значение).
     *         Учитывает возможное переполнение 32-битного счётчика датчика.
     */
    fun revolutionsDelta(previousReading: SensorReading): Long {
        val delta = cumulativeRevolutions - previousReading.cumulativeRevolutions
        return if (delta >= 0) delta else delta + (1L shl 32)
    }

    /**
     * Вычисляет временной интервал в единицах wheelEventTimeUnits относительно предыдущего показания.
     *
     * Обрабатывает 16-битное переполнение счётчика времени (wrap-around с 65535 на 0).
     * Согласно спецификации CSC, при переполнении необходимо добавить 0x10000 (65536).
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Интервал в единицах wheelEventTimeUnits (1/1024 секунды), неотрицательное значение.
     */
    fun wheelEventTimeDelta(previousReading: SensorReading): Int {
        var delta = wheelEventTimeUnits - previousReading.wheelEventTimeUnits
        if (delta < 0) {
            delta += 0x10000
        }
        return delta
    }

    /**
     * Вычисляет временной интервал в секундах относительно предыдущего показания.
     *
     * Использует wheelEventTimeUnits с разрешением 1/1024 секунды.
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Интервал в секундах.
     */
    fun timeDeltaSeconds(previousReading: SensorReading): Double {
        val deltaUnits = wheelEventTimeDelta(previousReading)
        return deltaUnits / 1024.0
    }

    /**
     * Вычисляет временной интервал в миллисекундах относительно предыдущего показания (через timestampUtc).
     *
     * Используется только если timestampUtc установлено платформой.
     * Для расчётов на основе данных датчика предпочтительно использовать [timeDeltaSeconds].
     *
     * @param previousReading Предыдущее показание датчика.
     * @return Интервал в миллисекундах, или 0 если timestampUtc не установлено.
     */
    fun timestampDeltaMs(previousReading: SensorReading): Long {
        val current = timestampUtc ?: return 0
        val previous = previousReading.timestampUtc ?: return 0
        return (current - previous).coerceAtLeast(0)
    }
}

