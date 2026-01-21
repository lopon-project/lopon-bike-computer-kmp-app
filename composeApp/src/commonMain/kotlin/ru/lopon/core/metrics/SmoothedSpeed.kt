package ru.lopon.core.metrics

data class SmoothedSpeed(
    val current: Double,
    val average: Double,
    val max: Double
)
