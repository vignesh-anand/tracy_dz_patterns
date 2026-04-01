package com.tracydz.patterns.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.tracydz.patterns.model.PatternResult
import com.tracydz.patterns.ui.theme.LegBase
import com.tracydz.patterns.ui.theme.LegDownwind
import com.tracydz.patterns.ui.theme.LegFinal

private val LEG_COLORS = listOf(LegDownwind, LegBase, LegFinal)

private val WAYPOINT_HUES = mapOf(
    1000 to BitmapDescriptorFactory.HUE_CYAN,
    600 to BitmapDescriptorFactory.HUE_YELLOW,
    300 to BitmapDescriptorFactory.HUE_GREEN,
)

@Composable
fun PatternOverlay(pattern: PatternResult) {
    pattern.legs.forEachIndexed { index, leg ->
        Polyline(
            points = listOf(leg.start, leg.end),
            color = LEG_COLORS.getOrElse(index) { Color.White },
            width = 10f,
            geodesic = true
        )
    }

    pattern.waypoints.forEach { wp ->
        if (wp.altitudeFt > 0) {
            val hue = WAYPOINT_HUES[wp.altitudeFt] ?: BitmapDescriptorFactory.HUE_AZURE
            Marker(
                state = MarkerState(position = wp.position),
                title = wp.label,
                snippet = wp.label,
                icon = BitmapDescriptorFactory.defaultMarker(hue),
                alpha = 0.85f
            )
        }
    }
}
