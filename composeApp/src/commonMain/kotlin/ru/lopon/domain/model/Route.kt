package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Географическая координата (широта, долгота).
 *
 * @property latitude Широта в градусах (-90 до +90).
 * @property longitude Долгота в градусах (-180 до +180).
 */
@Serializable
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

/**
 * Маршрут, представляющий линию из географических координат.
 *
 * @property id Уникальный идентификатор маршрута (UUID).
 * @property name Человекочитаемое название маршрута.
 * @property points Список координат точек маршрута в порядке следования.
 */
@Serializable
data class Route(
    val id: String,
    val name: String,
    val points: List<GeoCoordinate>
) {
    /**
     * Проверяет, содержит ли маршрут достаточно точек для навигации.
     * @return true, если маршрут содержит минимум 2 точки.
     */
    val isValid: Boolean
        get() = points.size >= 2

    /**
     * Возвращает количество точек в маршруте.
     */
    val pointCount: Int
        get() = points.size

    /**
     * Возвращает начальную точку маршрута.
     * @return Первая координата или null, если маршрут пуст.
     */
    val startPoint: GeoCoordinate?
        get() = points.firstOrNull()

    /**
     * Возвращает конечную точку маршрута.
     * @return Последняя координата или null, если маршрут пуст.
     */
    val endPoint: GeoCoordinate?
        get() = points.lastOrNull()
}

