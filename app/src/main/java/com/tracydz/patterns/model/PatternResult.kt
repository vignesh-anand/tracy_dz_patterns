package com.tracydz.patterns.model

import com.google.android.gms.maps.model.LatLng

data class PatternWaypoint(
    val position: LatLng,
    val altitudeFt: Int,
    val label: String
)

data class PatternLeg(
    val start: LatLng,
    val end: LatLng,
    val name: String
)

data class PatternResult(
    val waypoints: List<PatternWaypoint>,
    val legs: List<PatternLeg>
)
