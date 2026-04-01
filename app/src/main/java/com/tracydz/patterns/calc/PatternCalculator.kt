package com.tracydz.patterns.calc

import com.google.android.gms.maps.model.LatLng
import com.tracydz.patterns.model.Canopy
import com.tracydz.patterns.model.PatternLeg
import com.tracydz.patterns.model.PatternResult
import com.tracydz.patterns.model.PatternWaypoint
import com.tracydz.patterns.model.WindData
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PatternCalculator {

    private const val KTS_TO_FT_PER_SEC = 1.6878
    private const val FT_PER_DEG_LAT = 364000.0

    private const val ENTRY_ALT = 1000
    private const val BASE_ALT = 600
    private const val FINAL_ALT = 300
    private const val TARGET_ALT = 0

    /**
     * Calculate a right-hand landing pattern working backwards from the target.
     *
     * @param canopy     canopy performance profile
     * @param wind       current wind conditions
     * @param target     landing target LatLng
     * @param landingHeadingDeg  direction the pilot is FLYING on final (0=north, 90=east, etc.)
     */
    fun calculate(
        canopy: Canopy,
        wind: WindData,
        target: LatLng,
        landingHeadingDeg: Double
    ): PatternResult {
        val airspeedFtS = canopy.airspeedKts * KTS_TO_FT_PER_SEC
        val descentRateFtS = if (canopy.glideRatio > 0) airspeedFtS / canopy.glideRatio else airspeedFtS

        val windSpeedFtS = wind.speedKts * KTS_TO_FT_PER_SEC
        val windTowardDeg = wind.directionFrom + 180.0

        // Right-hand pattern ground track headings (always 90° turns)
        val finalHeading = landingHeadingDeg
        val baseHeading = landingHeadingDeg - 90.0
        val downwindHeading = landingHeadingDeg + 180.0

        // Final: 300ft -> 0ft
        val finalDisp = legDisplacement(finalHeading, airspeedFtS, descentRateFtS,
            (FINAL_ALT - TARGET_ALT).toDouble(), windSpeedFtS, windTowardDeg)

        // Base: 600ft -> 300ft
        val baseDisp = legDisplacement(baseHeading, airspeedFtS, descentRateFtS,
            (BASE_ALT - FINAL_ALT).toDouble(), windSpeedFtS, windTowardDeg)

        // Downwind: 1000ft -> 600ft
        val downwindDisp = legDisplacement(downwindHeading, airspeedFtS, descentRateFtS,
            (ENTRY_ALT - BASE_ALT).toDouble(), windSpeedFtS, windTowardDeg)

        // Build points backwards from target
        val targetPos = target
        val finalTurnPos = offsetLatLng(targetPos, -finalDisp.first, -finalDisp.second)
        val baseTurnPos = offsetLatLng(finalTurnPos, -baseDisp.first, -baseDisp.second)
        val entryPos = offsetLatLng(baseTurnPos, -downwindDisp.first, -downwindDisp.second)

        val waypoints = listOf(
            PatternWaypoint(entryPos, ENTRY_ALT, "Entry ${ENTRY_ALT}ft"),
            PatternWaypoint(baseTurnPos, BASE_ALT, "Base ${BASE_ALT}ft"),
            PatternWaypoint(finalTurnPos, FINAL_ALT, "Final ${FINAL_ALT}ft"),
            PatternWaypoint(targetPos, TARGET_ALT, "Target")
        )

        val legs = listOf(
            PatternLeg(entryPos, baseTurnPos, "Downwind"),
            PatternLeg(baseTurnPos, finalTurnPos, "Base"),
            PatternLeg(finalTurnPos, targetPos, "Final")
        )

        return PatternResult(waypoints, legs)
    }

    /**
     * Bearing in degrees (0-360) from one point to another.
     */
    fun bearingBetween(from: LatLng, to: LatLng): Double {
        if (from.latitude == to.latitude && from.longitude == to.longitude) return 0.0
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * Offset a LatLng by (eastFt, northFt).
     */
    fun offsetLatLng(origin: LatLng, eastFt: Double, northFt: Double): LatLng {
        val latOffset = northFt / FT_PER_DEG_LAT
        val lngOffset = eastFt / (FT_PER_DEG_LAT * cos(Math.toRadians(origin.latitude)))
        return LatLng(origin.latitude + latOffset, origin.longitude + lngOffset)
    }

    /**
     * Returns (eastFt, northFt) ground displacement for one pattern leg.
     * Ground track heading is fixed (90° turns preserved). Wind only affects
     * the distance covered along that heading via groundspeed changes.
     * Pilot crabs to maintain ground track; crosswind reduces forward component.
     */
    private fun legDisplacement(
        groundTrackHeadingDeg: Double,
        airspeedFtS: Double,
        descentRateFtS: Double,
        altLossFt: Double,
        windSpeedFtS: Double,
        windTowardDeg: Double
    ): Pair<Double, Double> {
        val timeSec = if (descentRateFtS > 0) altLossFt / descentRateFtS else 0.0
        val headingRad = Math.toRadians(groundTrackHeadingDeg)

        val angleDiffRad = Math.toRadians(windTowardDeg - groundTrackHeadingDeg)
        val wAlong = windSpeedFtS * cos(angleDiffRad)
        val wAcross = windSpeedFtS * sin(angleDiffRad)

        val vForward = if (abs(wAcross) < airspeedFtS) {
            sqrt(airspeedFtS * airspeedFtS - wAcross * wAcross)
        } else {
            0.0
        }

        val groundSpeed = maxOf(vForward + wAlong, 0.0)
        val distance = groundSpeed * timeSec

        return Pair(distance * sin(headingRad), distance * cos(headingRad))
    }
}
