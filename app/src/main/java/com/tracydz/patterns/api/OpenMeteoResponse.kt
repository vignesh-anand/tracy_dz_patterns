package com.tracydz.patterns.api

data class OpenMeteoResponse(
    val current: CurrentWeather?
)

data class CurrentWeather(
    val wind_speed_10m: Double?,
    val wind_direction_10m: Double?
)
