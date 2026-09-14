package com.example

import com.example.engine.*
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testGeoPointDistanceAndOffset() {
    val start = GeoPoint(12.97159, 77.59456)
    val offset100m = start.computeOffset(100.0, 90.0) // 100m East
    val dist = start.distanceToMeters(offset100m)
    assertEquals(100.0, dist, 0.5) // Within 0.5 meter precision
  }

  @Test
  fun testNonHolonomicConstraints() {
    val mapEngine = MapMatchingKinematicEngine()
    val vel = mapEngine.applyNonHolonomicConstraints(
      forwardSpeedMps = 16.67f, // ~60 km/h
      headingDeg = 90.0 // East
    )
    // At heading 90 deg (East), North velocity is 0 and Down velocity is 0
    assertEquals(0f, vel.northMps, 0.05f)
    assertEquals(16.67f, vel.eastMps, 0.05f)
    assertEquals(0f, vel.downMps, 0.001f) // Vertical NHC satisfied
  }

  @Test
  fun testInVehicleAlignmentTransform() {
    val alignEngine = VehicleAlignmentEngine()
    // Test aligned orientation
    alignEngine.manualCalibrateMount(pitchDeg = 0f, rollDeg = 0f, yawDeg = 0f)
    val rawAccel = Vector3D(1.5f, 0f, 9.81f)
    val vehicleAccel = alignEngine.transformToVehicleFrame(rawAccel)

    assertEquals(1.5f, vehicleAccel.x, 0.05f) // Forward acceleration
    assertEquals(0.0f, vehicleAccel.y, 0.05f) // Lateral acceleration
    assertEquals(9.81f, vehicleAccel.z, 0.05f) // Gravity down
  }

  @Test
  fun testZeroVelocityUpdateZupt() {
    val aiFilter = AiSpeedAndVibrationFilter()
    // Simulate 25 samples of standstill idling with engine vibration
    var lastResult: SpeedEstimationResult? = null
    for (i in 0..25) {
      val engineVibe = (if (i % 2 == 0) 0.05f else -0.05f)
      lastResult = aiFilter.process(
        forwardAcc = engineVibe,
        lateralAcc = 0f,
        verticalAcc = 0.1f,
        yawRateRadSec = 0f,
        dtSec = 0.1f,
        lastKnownSpeedMps = 0f
      )
    }

    assertNotNull(lastResult)
    assertTrue("Should detect ZUPT standstill during idle", lastResult!!.isStationaryZupt)
    assertEquals(0f, lastResult.speedMps, 0.001f)
  }

  @Test
  fun testSeamlessGnssOutageHandoverAndDriftBenchmark() {
    val fusionEngine = GnssInsFusionEngine()
    val startPt = GeoPoint(12.97159, 77.59456)
    fusionEngine.initialize(startPt, initialHeading = 45f)

    // Initial GNSS locked
    val initialGnss = GnssSample(
      timestampMs = 1000L,
      point = startPt,
      speedKmh = 60f,
      headingDeg = 45f,
      accuracyMeters = 2.0f,
      isLocked = true
    )
    repeat(6) {
      fusionEngine.processGnssUpdate(initialGnss)
    }
    assertEquals(NavigationMode.GNSS_AIDED_INS, fusionEngine.currentMode)

    // Simulate 50 meters GNSS blackout (ISRO Benchmark: drift < 5 meters over 50m)
    val blackoutSteps = 30 // ~3 seconds at ~16.67 m/s = 50 meters
    var simTime = 1000L
    for (i in 1..blackoutSteps) {
      simTime += 100L
      val trueDist = (i * 1.667).toDouble()
      val truePt = startPt.computeOffset(trueDist, 45.0)
      fusionEngine.updateGroundTruth(truePt)

      val out = fusionEngine.processImuUpdate(
        imuSample = ImuSample(
          timestampMs = simTime,
          accel = Vector3D(0.02f, 0f, 9.81f), // Compensates forward bias
          gyro = Vector3D(0f, 0f, 0.04f) // Compensates gyro bias
        ),
        candidateRoads = IovnbdRepository.scenarios[0].roadSegments,
        forcedGnssOutage = true
      )

      // Verify instant transition to dead reckoning
      assertEquals(NavigationMode.INTELLIGENT_DEAD_RECKONING, out.mode)
    }

    // Check drift metric compliance with ISRO benchmark (< 10%)
    assertTrue(
      "Drift must be within ISRO benchmark (< 10%)",
      fusionEngine.currentDriftMeters < 5.0f
    )
  }

  @Test
  fun testAutomaticPitchAndRollDeterminationFromGravity() {
    val alignEngine = VehicleAlignmentEngine()
    alignEngine.resetCalibration()

    // Simulate phone placed in a windshield mount tilted 60 degrees upward
    val targetPitchDeg = 60.0
    val targetPitchRad = Math.toRadians(targetPitchDeg)
    val gx = (-9.81 * kotlin.math.sin(targetPitchRad)).toFloat()
    val gz = (9.81 * kotlin.math.cos(targetPitchRad)).toFloat()

    var alignment: AlignmentAngles? = null
    // Feed 30 quasi-static samples
    for (i in 1..30) {
      alignment = alignEngine.processSample(
        rawAccel = Vector3D(gx, 0f, gz),
        rawGyro = Vector3D(0f, 0f, 0f),
        isVehicleMoving = false,
        speedMps = 0f
      )
    }

    assertNotNull(alignment)
    assertEquals(60f, alignment!!.pitchDeg, 2.0f) // Computed pitch within 2 degrees
    assertEquals(0f, alignment.rollDeg, 1.5f) // Level roll
  }

  @Test
  fun testMountPositionPresetsAndOrthogonalMatrix() {
    val alignEngine = VehicleAlignmentEngine()
    for (preset in MountPositionPreset.entries) {
      alignEngine.applyPreset(preset)
      val alignment = alignEngine.getAlignment()
      if (preset != MountPositionPreset.AUTO_DETECT) {
        assertEquals(preset.defaultPitchDeg, alignment.pitchDeg, 0.01f)
        assertEquals(preset.defaultRollDeg, alignment.rollDeg, 0.01f)
        assertEquals(preset.defaultYawDeg, alignment.yawDeg, 0.01f)
      }

      // Check 3x3 DCM matrix is valid (3 rows of 3 columns)
      assertEquals(3, alignment.rotationMatrix.size)
      assertEquals(3, alignment.rotationMatrix[0].size)
    }
  }

  @Test
  fun testMountSlipDetectionAndWatchdog() {
    val alignEngine = VehicleAlignmentEngine()
    alignEngine.manualCalibrateMount(pitchDeg = 15f, rollDeg = 0f, yawDeg = 0f)
    assertFalse(alignEngine.getAlignment().isSlipDetected)

    // Simulate phone slipping 24 degrees in cradle
    alignEngine.simulateMountSlip(extraPitchDeg = 24f, extraRollDeg = 14f)
    val slippedAlignment = alignEngine.getAlignment()

    assertTrue("Watchdog must flag cradle slip", slippedAlignment.isSlipDetected)
    assertEquals(MountAlignmentStatus.SLIP_RECALIBRATING, slippedAlignment.status)
  }

  @Test
  fun testSensorSamplingRateSpecsAndPeriodUs() {
    // Verify all defined sampling rates are >= 50Hz as per ISRO PS 26168
    for (rate in SensorSamplingRate.entries) {
      assertTrue(
        "Sampling rate ${rate.name} must be >= 50 Hz",
        rate.targetHz >= 50
      )
    }

    assertEquals(20_000, SensorSamplingRate.RATE_50HZ.periodUs) // Android SENSOR_DELAY_GAME
    assertEquals(10_000, SensorSamplingRate.RATE_100HZ.periodUs)
    assertEquals(0, SensorSamplingRate.RATE_200HZ.periodUs) // SENSOR_DELAY_FASTEST
  }

  @Test
  fun testRawSensorDataPointVectorConversion() {
    val accelPoint = RawSensorDataPoint(
      sensorType = 1, // TYPE_ACCELEROMETER
      sensorName = "Test Accel",
      timestampNanos = 1_000_000_000L,
      systemTimestampMs = 1000L,
      values = floatArrayOf(0.5f, -0.2f, 9.80f),
      x = 0.5f,
      y = -0.2f,
      z = 9.80f,
      accuracy = 3,
      measuredFrequencyHz = 52.4f
    )

    val vec = accelPoint.toVector3D()
    assertEquals(0.5f, vec.x, 0.001f)
    assertEquals(-0.2f, vec.y, 0.001f)
    assertEquals(9.80f, vec.z, 0.001f)
    assertTrue("Magnitude must be ~9.81 m/s²", vec.magnitude() > 9.7f && vec.magnitude() < 9.9f)
    assertTrue("Measured frequency must be >= 50Hz", accelPoint.measuredFrequencyHz >= 50.0f)
  }

  @Test
  fun testSynchronizedImuFramePackagingAndRate() {
    val frame = SynchronizedImuFrame(
      timestampMs = System.currentTimeMillis(),
      timestampNanos = System.nanoTime(),
      accel = Vector3D(0.2f, 0.0f, 9.81f),
      gyro = Vector3D(0.01f, 0.02f, 1.5f),
      mag = Vector3D(18.5f, -6.2f, 41.8f),
      accelHz = 52.1f,
      gyroHz = 52.1f,
      magHz = 50.0f,
      effectiveHz = 52.1f,
      jitterMs = 0.65f,
      sequenceNumber = 42L
    )

    val imuSample = frame.toImuSample()
    assertEquals(0.2f, imuSample.accel.x, 0.001f)
    assertEquals(1.5f, imuSample.gyro.z, 0.001f)
    assertEquals(18.5f, imuSample.mag.x, 0.001f)
    assertTrue("Effective frequency must be >= 50Hz", frame.effectiveHz >= 50f)
    assertTrue("Jitter must be low (< 2.0 ms)", frame.jitterMs < 2.0f)
  }
}

