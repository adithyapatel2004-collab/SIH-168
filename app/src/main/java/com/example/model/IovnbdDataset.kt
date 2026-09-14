package com.example.model

/**
 * Benchmark scenarios curated from the IO-VNBD
 * (Inertial and Odometry benchmark dataset for ground vehicle positioning)
 * as specified in ISRO Problem Statement 26168.
 */
data class BenchmarkScenario(
  val id: String,
  val title: String,
  val locationName: String,
  val description: String,
  val totalDistanceMeters: Float,
  val outageLengthMeters: Float,
  val averageSpeedKmh: Float,
  val roadSegments: List<RoadSegment>,
  val simulatedTrajectory: List<TrajectoryWaypoint>
)

data class TrajectoryWaypoint(
  val timeSec: Float,
  val point: GeoPoint,
  val speedKmh: Float,
  val headingDeg: Float,
  val isGnssAvailable: Boolean,
  val isPothole: Boolean = false,
  val isStoplight: Boolean = false
)

object IovnbdRepository {

  // Base coordinates (e.g. Bangalore ISRO Satellite Centre / Urban Corridor)
  private const val BASE_LAT = 12.9716
  private const val BASE_LON = 77.5946

  val scenarios: List<BenchmarkScenario> by lazy {
    listOf(
      createTunnelScenario(),
      createUrbanCanyonScenario(),
      createUndergroundParkingScenario(),
      createForestedHighwayScenario()
    )
  }

  /**
   * Scenario 1: Long Underground Tunnel Outage (ISRO Primary Benchmark: <100m drift over 1km)
   */
  private fun createTunnelScenario(): BenchmarkScenario {
    val startPt = GeoPoint(12.97159, 77.59456)
    val tunnelEntry = startPt.computeOffset(250.0, 45.0)
    val tunnelMid = tunnelEntry.computeOffset(600.0, 48.0)
    val tunnelExit = tunnelMid.computeOffset(600.0, 45.0)
    val finishPt = tunnelExit.computeOffset(300.0, 45.0)

    val segments = listOf(
      RoadSegment("seg-1", "Approach Highway A4", startPt, tunnelEntry, speedLimitKmh = 70f, isTunnel = false),
      RoadSegment("seg-2", "Underground Coastal Express Tunnel", tunnelEntry, tunnelMid, speedLimitKmh = 60f, isTunnel = true),
      RoadSegment("seg-3", "Underground Tunnel Section 2", tunnelMid, tunnelExit, speedLimitKmh = 60f, isTunnel = true),
      RoadSegment("seg-4", "Exit Viaduct Ramp", tunnelExit, finishPt, speedLimitKmh = 50f, isTunnel = false)
    )

    val waypoints = mutableListOf<TrajectoryWaypoint>()
    val totalSteps = 100
    for (i in 0..totalSteps) {
      val t = i / totalSteps.toFloat()
      val distAlongPath = t * 1750.0
      val isGnssBlocked = distAlongPath in 250.0..1450.0
      val currentPt = when {
        distAlongPath < 250.0 -> {
          val f = (distAlongPath / 250.0).coerceIn(0.0, 1.0)
          startPt.computeOffset(distAlongPath, 45.0)
        }
        distAlongPath < 850.0 -> {
          val f = ((distAlongPath - 250.0) / 600.0).coerceIn(0.0, 1.0)
          tunnelEntry.computeOffset((distAlongPath - 250.0), 48.0)
        }
        distAlongPath < 1450.0 -> {
          val f = ((distAlongPath - 850.0) / 600.0).coerceIn(0.0, 1.0)
          tunnelMid.computeOffset((distAlongPath - 850.0), 45.0)
        }
        else -> {
          tunnelExit.computeOffset((distAlongPath - 1450.0), 45.0)
        }
      }
      waypoints.add(
        TrajectoryWaypoint(
          timeSec = t * 105f,
          point = currentPt,
          speedKmh = if (i < 5) 30f + i * 6f else 60f,
          headingDeg = 45f + (if (distAlongPath in 250.0..850.0) 3f else 0f),
          isGnssAvailable = !isGnssBlocked,
          isPothole = (i == 42 || i == 68),
          isStoplight = false
        )
      )
    }

    return BenchmarkScenario(
      id = "iovnbd_tunnel_1200m",
      title = "IO-VNBD #1: Long Underground Tunnel",
      locationName = "Pragati Maidan / Coastal Highway Tunnel",
      description = "Simulates entering a 1200m deep underground tunnel at 60 km/h with 100% GNSS blackout. Benchmark: drift < 100m (<10%).",
      totalDistanceMeters = 1750f,
      outageLengthMeters = 1200f,
      averageSpeedKmh = 60f,
      roadSegments = segments,
      simulatedTrajectory = waypoints
    )
  }

  /**
   * Scenario 2: Dense Urban Canyon with Multi-Story Towers, Multipath, and ZUPT Stoplights
   */
  private fun createUrbanCanyonScenario(): BenchmarkScenario {
    val p0 = GeoPoint(12.9352, 77.6245)
    val p1 = p0.computeOffset(300.0, 90.0) // East along tech corridor
    val p2 = p1.computeOffset(400.0, 0.0)  // North canyon
    val p3 = p2.computeOffset(350.0, 270.0) // West towards metro pillar

    val segments = listOf(
      RoadSegment("uc-1", "Outer Ring Road Avenue", p0, p1, speedLimitKmh = 45f),
      RoadSegment("uc-2", "Skyscraper Corridor (GPS Jammed)", p1, p2, speedLimitKmh = 40f, isUnderpass = true),
      RoadSegment("uc-3", "Metro Pillar Shadow Expressway", p2, p3, speedLimitKmh = 50f)
    )

    val waypoints = mutableListOf<TrajectoryWaypoint>()
    val steps = 90
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val dist = t * 1050.0
      val isBlocked = dist in 250.0..850.0
      val isStoplight = (i in 28..34) // Stoplight standstill for ZUPT verification
      val pt = when {
        dist < 300.0 -> p0.computeOffset(dist, 90.0)
        dist < 700.0 -> p1.computeOffset(dist - 300.0, 0.0)
        else -> p2.computeOffset(dist - 700.0, 270.0)
      }
      val heading = when {
        dist < 300.0 -> 90f
        dist < 700.0 -> 0f
        else -> 270f
      }
      waypoints.add(
        TrajectoryWaypoint(
          timeSec = t * 120f,
          point = pt,
          speedKmh = if (isStoplight) 0f else 42f,
          headingDeg = heading,
          isGnssAvailable = !isBlocked,
          isPothole = (i == 55),
          isStoplight = isStoplight
        )
      )
    }

    return BenchmarkScenario(
      id = "iovnbd_urban_canyon",
      title = "IO-VNBD #2: High-Rise Urban Canyon",
      locationName = "Cyber Corridor Skyscraper Avenue",
      description = "Severe multipath and RF shadow between 40-story towers. Tests Zero-Velocity Update (ZUPT) and non-holonomic constraints.",
      totalDistanceMeters = 1050f,
      outageLengthMeters = 600f,
      averageSpeedKmh = 40f,
      roadSegments = segments,
      simulatedTrajectory = waypoints
    )
  }

  /**
   * Scenario 3: Multi-Level Spiral Underground Parking
   */
  private fun createUndergroundParkingScenario(): BenchmarkScenario {
    val p0 = GeoPoint(12.9800, 77.5800)
    val p1 = p0.computeOffset(100.0, 180.0)
    val p2 = p1.computeOffset(150.0, 270.0)
    val p3 = p2.computeOffset(200.0, 0.0)

    val segments = listOf(
      RoadSegment("pkg-1", "Basement Ramp Incline B1", p0, p1, speedLimitKmh = 25f, isTunnel = true),
      RoadSegment("pkg-2", "Multi-Level Spiral Helix B2", p1, p2, speedLimitKmh = 20f, isTunnel = true),
      RoadSegment("pkg-3", "Basement Parking Alley B3", p2, p3, speedLimitKmh = 20f, isTunnel = true)
    )

    val waypoints = mutableListOf<TrajectoryWaypoint>()
    val steps = 80
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val dist = t * 450.0
      val isBlocked = dist > 50.0 // Almost entire parking basement is blocked
      val pt = when {
        dist < 100.0 -> p0.computeOffset(dist, 180.0)
        dist < 250.0 -> p1.computeOffset(dist - 100.0, 270.0)
        else -> p2.computeOffset(dist - 250.0, 0.0)
      }
      waypoints.add(
        TrajectoryWaypoint(
          timeSec = t * 90f,
          point = pt,
          speedKmh = 18f,
          headingDeg = if (dist < 100.0) 180f else if (dist < 250.0) 270f else 0f,
          isGnssAvailable = !isBlocked,
          isPothole = false,
          isStoplight = (i in 15..20)
        )
      )
    }

    return BenchmarkScenario(
      id = "iovnbd_parking_spiral",
      title = "IO-VNBD #3: Multi-Level Underground Parking",
      locationName = "Terminal Multi-Deck Basement B1-B3",
      description = "Continuous spiral turns beneath 3 layers of reinforced concrete. Tests continuous gyroscope bias tracking and low-speed dead reckoning.",
      totalDistanceMeters = 450f,
      outageLengthMeters = 400f,
      averageSpeedKmh = 18f,
      roadSegments = segments,
      simulatedTrajectory = waypoints
    )
  }

  /**
   * Scenario 4: Forested Mountain Canopy Outage
   */
  private fun createForestedHighwayScenario(): BenchmarkScenario {
    val p0 = GeoPoint(12.8200, 77.4500)
    val p1 = p0.computeOffset(500.0, 30.0)
    val p2 = p1.computeOffset(800.0, 60.0)
    val p3 = p2.computeOffset(600.0, 25.0)

    val segments = listOf(
      RoadSegment("for-1", "Ghat Approach Road", p0, p1, speedLimitKmh = 60f),
      RoadSegment("for-2", "Dense Canopy Forest Reserve", p1, p2, speedLimitKmh = 50f, isTunnel = false),
      RoadSegment("for-3", "Valley Pass Clearing", p2, p3, speedLimitKmh = 55f)
    )

    val waypoints = mutableListOf<TrajectoryWaypoint>()
    val steps = 90
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val dist = t * 1900.0
      val isBlocked = dist in 400.0..1400.0
      val pt = when {
        dist < 500.0 -> p0.computeOffset(dist, 30.0)
        dist < 1300.0 -> p1.computeOffset(dist - 500.0, 60.0)
        else -> p2.computeOffset(dist - 1300.0, 25.0)
      }
      waypoints.add(
        TrajectoryWaypoint(
          timeSec = t * 130f,
          point = pt,
          speedKmh = 52f,
          headingDeg = if (dist < 500.0) 30f else if (dist < 1300.0) 60f else 25f,
          isGnssAvailable = !isBlocked,
          isPothole = (i == 48 || i == 70),
          isStoplight = false
        )
      )
    }

    return BenchmarkScenario(
      id = "iovnbd_forested_highway",
      title = "IO-VNBD #4: Forest Canopy Dense Outage",
      locationName = "Western Ghats Mountain Pass",
      description = "Dense wet tree canopy attenuating L1/L5 GNSS signals for 1.0 km. Tests AI forward speed estimation through road surface variations.",
      totalDistanceMeters = 1900f,
      outageLengthMeters = 1000f,
      averageSpeedKmh = 52f,
      roadSegments = segments,
      simulatedTrajectory = waypoints
    )
  }
}
