package com.example.model

import kotlin.math.*

data class GeoPoint(
  val lat: Double,
  val lon: Double,
  val alt: Double = 0.0
) {
  /**
   * Approximate Euclidean distance in meters using Equirectangular projection
   */
  fun distanceToMeters(other: GeoPoint): Double {
    val r = 6371000.0 // Earth radius in meters
    val lat1Rad = Math.toRadians(lat)
    val lat2Rad = Math.toRadians(other.lat)
    val dLat = Math.toRadians(other.lat - lat)
    val dLon = Math.toRadians(other.lon - lon)
    val x = dLon * cos((lat1Rad + lat2Rad) / 2.0)
    val y = dLat
    return sqrt(x * x + y * y) * r
  }

  /**
   * Destination point given distance in meters and bearing in degrees
   */
  fun computeOffset(distanceMeters: Double, bearingDeg: Double): GeoPoint {
    val r = 6371000.0
    val distRatio = distanceMeters / r
    val bearingRad = Math.toRadians(bearingDeg)
    val lat1Rad = Math.toRadians(lat)
    val lon1Rad = Math.toRadians(lon)

    val lat2Rad = asin(
      sin(lat1Rad) * cos(distRatio) +
          cos(lat1Rad) * sin(distRatio) * cos(bearingRad)
    )
    val lon2Rad = lon1Rad + atan2(
      sin(bearingRad) * sin(distRatio) * cos(lat1Rad),
      cos(distRatio) - sin(lat1Rad) * sin(lat2Rad)
    )
    return GeoPoint(
      lat = Math.toDegrees(lat2Rad),
      lon = Math.toDegrees(lon2Rad),
      alt = alt
    )
  }
}

data class Vector3D(
  val x: Float = 0f,
  val y: Float = 0f,
  val z: Float = 0f
) {
  fun magnitude(): Float = sqrt(x * x + y * y + z * z)
}

data class ImuSample(
  val timestampMs: Long,
  val accel: Vector3D,
  val gyro: Vector3D,
  val mag: Vector3D = Vector3D(0f, 0f, 0f)
)

data class GnssSample(
  val timestampMs: Long,
  val point: GeoPoint,
  val speedKmh: Float,
  val headingDeg: Float,
  val accuracyMeters: Float,
  val isLocked: Boolean,
  val satelliteCount: Int = 14,
  val navicCount: Int = 4, // ISRO NavIC (IRNSS) satellites
  val hdop: Float = 0.9f
)

enum class NavigationMode(val displayName: String) {
  GNSS_AIDED_INS("GNSS + INS Fusion"),
  INTELLIGENT_DEAD_RECKONING("AI Dead Reckoning (INS)"),
  STANDSTILL_ZUPT("Zero Velocity Update (ZUPT)")
}

enum class MountPositionPreset(
  val label: String,
  val defaultPitchDeg: Float,
  val defaultRollDeg: Float,
  val defaultYawDeg: Float,
  val description: String
) {
  AUTO_DETECT("Auto-Detect Mount", 0f, 0f, 0f, "Live dynamic extraction of pitch, roll, and driving yaw"),
  DASHBOARD_FLAT("Dashboard Flat Pad", 8f, 0f, 0f, "Phone resting flat or slightly tilted on anti-skid dashboard mat"),
  WINDSHIELD_CRADLE("Windshield Suction", 62f, -3f, 12f, "Suction cradle on windshield, tilted up and angled toward driver"),
  AIR_VENT_MAGNETIC("AC Vent Magnetic", 78f, 0f, 18f, "Upright portrait mount clipped on center AC vent slats"),
  CONSOLE_CUPHOLDER("Cup Holder / Console", 84f, 8f, -14f, "Near-vertical angled phone in central cup holder or console tray"),
  LANDSCAPE_CRADLE("Landscape Cradle", 58f, -90f, 8f, "Horizontal 90° rotated mount for widescreen navigation view")
}

enum class MountAlignmentStatus(val displayName: String) {
  UNALIGNED("Pending Calibration"),
  CALIBRATING("Calibrating Mount..."),
  ALIGNED("Vehicle Aligned (Dynamic)"),
  SLIP_RECALIBRATING("Mount Shift Detected (Re-aligning)")
}

data class AlignmentAngles(
  val pitchDeg: Float = 0f,
  val rollDeg: Float = 0f,
  val yawDeg: Float = 0f,
  val status: MountAlignmentStatus = MountAlignmentStatus.ALIGNED,
  val confidence: Float = 0.98f,
  val preset: MountPositionPreset = MountPositionPreset.AUTO_DETECT,
  val calibrationProgress: Float = 1.0f,
  val isSlipDetected: Boolean = false,
  val stageDescription: String = "Vehicle Coordinate Frame Locked",
  val gravityMagnitude: Float = 9.81f,
  val mountStabilityScore: Float = 0.98f,
  val forwardAccResidual: Float = 0f,
  val lateralAccResidual: Float = 0f,
  val rotationMatrix: List<List<Float>> = listOf(
    listOf(1f, 0f, 0f),
    listOf(0f, 1f, 0f),
    listOf(0f, 0f, 1f)
  )
)

data class RoadSegment(
  val id: String,
  val name: String,
  val start: GeoPoint,
  val end: GeoPoint,
  val speedLimitKmh: Float = 60f,
  val isTunnel: Boolean = false,
  val isUnderpass: Boolean = false
) {
  fun bearingDeg(): Double {
    val lat1 = Math.toRadians(start.lat)
    val lon1 = Math.toRadians(start.lon)
    val lat2 = Math.toRadians(end.lat)
    val lon2 = Math.toRadians(end.lon)
    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val bearing = Math.toDegrees(atan2(y, x))
    return (bearing + 360.0) % 360.0
  }
}

data class DriftMetrics(
  val outageDurationSec: Float = 0f,
  val outageDistanceMeters: Float = 0f,
  val driftErrorMeters: Float = 0f,
  val driftPercentage: Float = 0f,
  val rawDriftErrorMeters: Float = 0f,
  val isWithinBenchmark: Boolean = true // ISRO Benchmark: < 10% drift
)

data class VibrationSpectrum(
  val engineHarmonicPower: Float = 0.12f,
  val roadRoughnessRms: Float = 0.25f,
  val potholeShockDetected: Boolean = false,
  val frequencyPeakHz: Float = 28.5f
)

data class TrajectoryBreadcrumb(
  val point: GeoPoint,
  val mode: NavigationMode,
  val speedKmh: Float
)
