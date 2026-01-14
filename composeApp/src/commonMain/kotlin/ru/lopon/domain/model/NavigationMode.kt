package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Режим навигации, определяющий источник данных для отслеживания позиции.
 *
 * @property Sensor Режим с использованием только BLE-датчика колеса.
 *                  Позиция определяется по маршруту на основе пройденной дистанции.
 * @property Hybrid Гибридный режим: BLE-датчик для дистанции, GPS для позиции.
 *                  Обеспечивает максимальную точность при наличии GPS-сигнала.
 * @property Gps Режим с использованием только GPS.
 *
 */
@Serializable
sealed class NavigationMode {

    /**
     * Режим навигации только по BLE-датчику колеса.
     * Позиция на маршруте вычисляется по пройденной дистанции.
     */
    @Serializable
    data object Sensor : NavigationMode()

    /**
     * Гибридный режим: BLE-датчик + GPS.
     * Дистанция считается по датчику, позиция корректируется по GPS.
     */
    @Serializable
    data object Hybrid : NavigationMode()

    /**
     * Режим навигации только по GPS.
     * Используется при отсутствии BLE-датчика.
     */
    @Serializable
    data object Gps : NavigationMode()
}

