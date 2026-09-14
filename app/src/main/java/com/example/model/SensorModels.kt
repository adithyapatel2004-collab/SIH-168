package com.example.model

/**
 * Sampling rate configurations for Android SensorManager.
 * The ISRO PS 26168 specification mandates high frequency (>= 50Hz)
 * for vehicle inertial navigation and dead reckoning.
 */
enum class SensorSamplingRate(
  val periodUs: Int,
  val targetHz: Int,
  val label: String,
  val description: String
) {
  RATE_50HZ(
    periodUs = 20_000,
    targetHz = 50,
    label = "50 Hz (Standard)",
    description = "Android SENSOR_DELAY_GAME (20,000 µs interval)"
  ),
  RATE_100HZ(
    periodUs = 10_000,
    targetHz = 100,
    label = "100 Hz (Turbo)",
    description = "High rate kinematic sampling (10,000 µs interval)"
  ),
  RATE_200HZ(
    periodUs = 0, // SENSOR_DELAY_FASTEST
    targetHz = 200,
    label = "200 Hz (Tactical / Max)",
    description = "Sensor hardware fastest reporting (FOG IMU profile)"
  )
}

/**
 * Raw data point emitted directly from Android SensorEventListener.
 */
data class RawSensorDataPoint(
  val sensorType: Int,
  val sensorName: String,
  val timestampNanos: Long,
  val systemTimestampMs: Long,
  val values: FloatArray,
  val x: Float,
  val y: Float,
  val z: Float,
  val accuracy: Int = 3,
  val measuredFrequencyHz: Float = 0f
) {
  fun toVector3D(): Vector3D = Vector3D(x, y, z)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as RawSensorDataPoint
    if (sensorType != other.sensorType) return false
    if (timestampNanos != other.timestampNanos) return false
    if (!values.contentEquals(other.values)) return false
    return true
  }

  override fun hashCode(): Int {
    var result = sensorType
    result = 31 * result + timestampNanos.hashCode()
    result = 31 * result + values.contentHashCode()
    return result
  }
}

/**
 * Synchronized tri-axial frame uniting Accelerometer, Gyroscope, and Magnetometer
 * at high frequency (>= 50Hz).
 */
data class SynchronizedImuFrame(
  val timestampMs: Long,
  val timestampNanos: Long,
  val accel: Vector3D,
  val gyro: Vector3D,
  val mag: Vector3D,
  val accelHz: Float = 0f,
  val gyroHz: Float = 0f,
  val magHz: Float = 0f,
  val effectiveHz: Float = 0f,
  val jitterMs: Float = 0f,
  val sequenceNumber: Long = 0L
) {
  fun toImuSample(): ImuSample = ImuSample(
    timestampMs = timestampMs,
    accel = accel,
    gyro = gyro,
    mag = mag
  )
}

/**
 * Hardware capability and live telemetry for an individual sensor channel.
 */
data class IndividualSensorStatus(
  val sensorType: Int,
  val typeName: String,
  val name: String = "Unknown Sensor",
  val vendor: String = "Generic Vendor",
  val isPresent: Boolean = false,
  val minDelayUs: Int = 0,
  val maxFrequencyHz: Float = 0f,
  val currentFrequencyHz: Float = 0f,
  val totalEventsReceived: Long = 0L,
  val lastValues: Vector3D = Vector3D(),
  val accuracy: Int = 3
)

/**
 * Comprehensive diagnostics of the High Frequency SensorManager Service.
 */
data class SensorServiceDiagnostics(
  val isStreaming: Boolean = false,
  val samplingRate: SensorSamplingRate = SensorSamplingRate.RATE_50HZ,
  val accelStatus: IndividualSensorStatus = IndividualSensorStatus(1, "ACCELEROMETER"),
  val gyroStatus: IndividualSensorStatus = IndividualSensorStatus(4, "GYROSCOPE"),
  val magStatus: IndividualSensorStatus = IndividualSensorStatus(2, "MAGNETOMETER"),
  val totalEventsCombined: Long = 0L,
  val averageThroughputHz: Float = 0f,
  val meanJitterMs: Float = 0f,
  val isHighFrequencyAchieved: Boolean = false // Confirms >= 50Hz requirement
)
