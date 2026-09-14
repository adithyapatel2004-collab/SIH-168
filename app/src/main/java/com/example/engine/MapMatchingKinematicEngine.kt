package com.example.engine

import com.example.model.GeoPoint
import com.example.model.RoadSegment
import com.example.model.Vector3D
import kotlin.math.*

/**
 * Advanced Map-Matching & Kinematic Constraints Engine.
 *
 * Enforces:
 * 1. Non-Holonomic Constraints (NHC): Ground vehicle cannot slide sideways (v_lat = 0)
 *    or fly upwards (v_vert = 0).
 * 2. Hidden Markov Model (HMM) & Geometric Corridor Map Matching:
 *    Snaps dead reckoning trajectory to offline road grid and tunnel corridors.
 */
class MapMatchingKinematicEngine {

  private var activeSegment: RoadSegment? = null
  private var lastMatchedPoint: GeoPoint? = null
  private val sigmaDistance = 8.0 // 8 meters standard deviation for road matching
  private val snapStrength = 0.65f // Smooth snap-to-corridor factor

  /**
   * Applies Non-Holonomic Constraints (NHC) to vehicle velocity.
   * Wheeled vehicles satisfy no-skid, no-flight dynamics:
   * v_longitudinal = forwardSpeed
   * v_lateral = 0 (NHC constraint)
   * v_vertical = 0 (NHC constraint)
   */
  fun applyNonHolonomicConstraints(
    forwardSpeedMps: Float,
    headingDeg: Double
  ): VelocityVector {
    val headingRad = Math.toRadians(headingDeg)
    val velNorth = forwardSpeedMps * cos(headingRad)
    val velEast = forwardSpeedMps * sin(headingRad)
    return VelocityVector(
      northMps = velNorth.toFloat(),
      eastMps = velEast.toFloat(),
      downMps = 0f // Vertical NHC
    )
  }

  /**
   * Map-matches an estimated dead-reckoning position against available offline road network segments.
   * Returns the constrained road point, active segment, and perpendicular offset.
   */
  fun matchToRoadNetwork(
    estimatedPoint: GeoPoint,
    vehicleHeadingDeg: Float,
    candidateRoads: List<RoadSegment>
  ): MapMatchResult {
    if (candidateRoads.isEmpty()) {
      return MapMatchResult(
        matchedPoint = estimatedPoint,
        matchedSegment = null,
        crossTrackDistanceMeters = 0f,
        headingDifferenceDeg = 0f,
        confidence = 0.5f
      )
    }

    var bestScore = Double.NEGATIVE_INFINITY
    var bestSegment: RoadSegment? = null
    var bestSnappedPoint: GeoPoint = estimatedPoint
    var bestCrossTrack = 0.0
    var bestHeadingDiff = 0.0

    for (segment in candidateRoads) {
      val projection = projectPointOnSegment(estimatedPoint, segment.start, segment.end)
      val distToRoad = estimatedPoint.distanceToMeters(projection)

      val roadBearing = segment.bearingDeg()
      var headingDiff = abs(vehicleHeadingDeg - roadBearing)
      if (headingDiff > 180.0) headingDiff = 360.0 - headingDiff

      // Only match segments within 35 meters corridor and heading alignment within 65 degrees
      if (distToRoad <= 35.0 && headingDiff <= 65.0) {
        // HMM Emission & Heading score
        val emissionLogProb = -0.5 * (distToRoad * distToRoad) / (sigmaDistance * sigmaDistance)
        val headingScore = cos(Math.toRadians(headingDiff))
        val transitionScore = if (activeSegment?.id == segment.id) 1.5 else 0.0

        val totalScore = emissionLogProb + (headingScore * 2.0) + transitionScore

        if (totalScore > bestScore) {
          bestScore = totalScore
          bestSegment = segment
          bestSnappedPoint = projection
          bestCrossTrack = distToRoad
          bestHeadingDiff = headingDiff
        }
      }
    }

    if (bestSegment != null) {
      activeSegment = bestSegment
      // Smoothly blend dead-reckoning estimate towards road centerline (snap-to-lane)
      val blendedLat = estimatedPoint.lat + (bestSnappedPoint.lat - estimatedPoint.lat) * snapStrength
      val blendedLon = estimatedPoint.lon + (bestSnappedPoint.lon - estimatedPoint.lon) * snapStrength
      val matched = GeoPoint(blendedLat, blendedLon, estimatedPoint.alt)
      lastMatchedPoint = matched

      return MapMatchResult(
        matchedPoint = matched,
        matchedSegment = bestSegment,
        crossTrackDistanceMeters = bestCrossTrack.toFloat(),
        headingDifferenceDeg = bestHeadingDiff.toFloat(),
        confidence = (0.75f + (1.0f - (bestCrossTrack / 35.0).toFloat()) * 0.25f).coerceIn(0f, 1f)
      )
    }

    return MapMatchResult(
      matchedPoint = estimatedPoint,
      matchedSegment = activeSegment,
      crossTrackDistanceMeters = 0f,
      headingDifferenceDeg = 0f,
      confidence = 0.4f
    )
  }

  /**
   * Projects a GeoPoint onto a straight line segment defined by (start, end).
   */
  private fun projectPointOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): GeoPoint {
    // Equirectangular local flat approximation for high accuracy over road scales
    val r = 6371000.0
    val lat0 = Math.toRadians((a.lat + b.lat) / 2.0)

    val ax = Math.toRadians(a.lon) * r * cos(lat0)
    val ay = Math.toRadians(a.lat) * r
    val bx = Math.toRadians(b.lon) * r * cos(lat0)
    val by = Math.toRadians(b.lat) * r
    val px = Math.toRadians(p.lon) * r * cos(lat0)
    val py = Math.toRadians(p.lat) * r

    val abx = bx - ax
    val aby = by - ay
    val apx = px - ax
    val apy = py - ay

    val abLenSq = abx * abx + aby * aby
    if (abLenSq < 1e-4) return a

    val u = ((apx * abx + apy * aby) / abLenSq).coerceIn(0.0, 1.0)
    val projX = ax + u * abx
    val projY = ay + u * aby

    val projLat = Math.toDegrees(projY / r)
    val projLon = Math.toDegrees(projX / (r * cos(lat0)))

    return GeoPoint(projLat, projLon, p.alt)
  }

  fun reset() {
    activeSegment = null
    lastMatchedPoint = null
  }
}

data class VelocityVector(
  val northMps: Float,
  val eastMps: Float,
  val downMps: Float
)

data class MapMatchResult(
  val matchedPoint: GeoPoint,
  val matchedSegment: RoadSegment?,
  val crossTrackDistanceMeters: Float,
  val headingDifferenceDeg: Float,
  val confidence: Float
)
