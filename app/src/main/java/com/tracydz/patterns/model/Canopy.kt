package com.tracydz.patterns.model

import java.util.UUID

data class Canopy(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val airspeedKts: Double,
    val glideRatio: Double
)
