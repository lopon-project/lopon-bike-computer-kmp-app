package ru.lopon.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) 
