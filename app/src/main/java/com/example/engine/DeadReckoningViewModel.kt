package com.example.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.sensors.SensorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

data class NavigationUiState(
  val fusionOutput: NavigationFusionOutput? = null,
  val activeScenario: BenchmarkScenario = IovnbdRepository.scenarios[0],
  val isLiveSensorMode: Boolean = false,
  val isPlaying: Boolean = true,
  val playbackSpeed: Float = 1.0f,
  val updateRateHz: Int = 10, // 10Hz smartphone or 200Hz Edge engine
  val isManualGnssCut: Boolean = false,
  val simulationProgress: Float = 0f,
  val showAlignmentDialog: Boolean = false,
  val showBenchmarkDialog: Boolean = false,
  val showEdgeExportDialog: Boolean = false,
  val showSensorStreamDialog: Boolean = false,
  val sensorSamplingRate: SensorSamplingRate = SensorSamplingRate.RATE_50HZ,
  val sensorDiagnostics: SensorServiceDiagnostics = SensorServiceDiagnostics(),
  val hasHardwareSensors: Boolean = false
)

class DeadReckoningViewModel(application: Application) : AndroidViewModel(application) {

  private val sensorService = SensorService(application)
  private val fusionEngine = GnssInsFusionEngine()

  private val _uiState = MutableStateFlow(
    NavigationUiState(
      hasHardwareSensors = sensorService.hasHardwareSensors()
    )
  )
  val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

  private var loopJob: Job? = null
  private var simStepIndex = 0

  init {
    loadScenario(_uiState.value.activeScenario)
    startSimulationLoop()
    observeSensorDiagnostics()
  }

  private fun observeSensorDiagnostics() {
    viewModelScope.launch {
      sensorService.highFrequencyService.diagnostics.collect { diag ->
        _uiState.update { it.copy(sensorDiagnostics = diag) }
      }
    }
  }

  fun loadScenario(scenario: BenchmarkScenario) {
    simStepIndex = 0
    val startPt = scenario.simulatedTrajectory.firstOrNull()?.point ?: GeoPoint(12.97159, 77.59456)
    val startHeading = scenario.simulatedTrajectory.firstOrNull()?.headingDeg ?: 45f
    fusionEngine.initialize(startPt, startHeading)

    _uiState.update {
      it.copy(
        activeScenario = scenario,
        simulationProgress = 0f,
        isManualGnssCut = false
      )
    }
  }

  fun togglePlayPause() {
    val newPlaying = !_uiState.value.isPlaying
    _uiState.update { it.copy(isPlaying = newPlaying) }
    if (newPlaying) {
      if (!_uiState.value.isLiveSensorMode) {
        startSimulationLoop()
      }
    } else {
      loopJob?.cancel()
    }
  }

  fun restartScenario() {
    loadScenario(_uiState.value.activeScenario)
    if (_uiState.value.isPlaying) {
      startSimulationLoop()
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    _uiState.update { it.copy(playbackSpeed = speed) }
  }

  fun setUpdateRateHz(hz: Int) {
    _uiState.update { it.copy(updateRateHz = hz) }
  }

  fun toggleManualGnssCut() {
    _uiState.update { it.copy(isManualGnssCut = !it.isManualGnssCut) }
  }

  fun setLiveSensorMode(enabled: Boolean) {
    loopJob?.cancel()
    _uiState.update { it.copy(isLiveSensorMode = enabled) }
    if (enabled) {
      startLiveSensorLoop()
    } else {
      loadScenario(_uiState.value.activeScenario)
      startSimulationLoop()
    }
  }

  fun setShowAlignmentDialog(show: Boolean) {
    _uiState.update { it.copy(showAlignmentDialog = show) }
  }

  fun setMountPreset(preset: MountPositionPreset) {
    fusionEngine.alignmentEngine.applyPreset(preset)
  }

  fun simulateMountSlip() {
    fusionEngine.alignmentEngine.simulateMountSlip(extraPitchDeg = 24f, extraRollDeg = 14f)
  }

  fun resetMountCalibration() {
    fusionEngine.alignmentEngine.resetCalibration()
  }

  fun setShowBenchmarkDialog(show: Boolean) {
    _uiState.update { it.copy(showBenchmarkDialog = show) }
  }

  fun setShowEdgeExportDialog(show: Boolean) {
    _uiState.update { it.copy(showEdgeExportDialog = show) }
  }

  fun setShowSensorStreamDialog(show: Boolean) {
    _uiState.update { it.copy(showSensorStreamDialog = show) }
  }

  fun setSensorSamplingRate(rate: SensorSamplingRate) {
    _uiState.update { it.copy(sensorSamplingRate = rate) }
    if (_uiState.value.isLiveSensorMode) {
      startLiveSensorLoop()
    }
  }

  fun getTrajectoryHistory(): List<TrajectoryBreadcrumb> = fusionEngine.trajectoryHistory
  fun getRawHistory(): List<GeoPoint> = fusionEngine.rawHistory
  fun getGroundTruthHistory(): List<GeoPoint> = fusionEngine.groundTruthHistory

  private fun startSimulationLoop() {
    loopJob?.cancel()
    loopJob = viewModelScope.launch {
      val scenario = _uiState.value.activeScenario
      val waypoints = scenario.simulatedTrajectory
      if (waypoints.isEmpty()) return@launch

      val baseDtMs = 100L // 10 Hz base

      while (isActive) {
        if (!_uiState.value.isPlaying) {
          delay(100L)
          continue
        }

        val rateHz = _uiState.value.updateRateHz
        val speedMultiplier = _uiState.value.playbackSpeed
        val sleepTimeMs = ((1000f / (rateHz * speedMultiplier)).toLong()).coerceAtLeast(5L)

        // Advance simulation waypoint
        if (simStepIndex >= waypoints.size - 1) {
          simStepIndex = 0 // Loop scenario smoothly
          fusionEngine.initialize(waypoints[0].point, waypoints[0].headingDeg)
        } else {
          simStepIndex++
        }

        val wp = waypoints[simStepIndex]
        val progress = simStepIndex.toFloat() / (waypoints.size - 1)

        // Generate synthetic realistic vehicle IMU signals based on waypoint kinematics
        val now = System.currentTimeMillis()
        val speedMps = wp.speedKmh / 3.6f

        // Simulated vehicle dynamics with engine vibration & chassis resonance
        val engineVibe = (sin(now * 0.15) * 0.12f + sin(now * 0.35) * 0.08f).toFloat()
        val potholeSpike = if (wp.isPothole) 5.8f else 0f
        val accelFwd = (if (wp.speedKmh > 0f) 0.15f else -0.3f) + engineVibe

        // Centripetal acceleration: a_lat = v^2 / R or v * dHeading/dt
        val headingDelta = if (simStepIndex > 0) wp.headingDeg - waypoints[simStepIndex - 1].headingDeg else 0f
        val gyroYaw = (headingDelta * 10f) + (sin(now * 0.05) * 0.02f).toFloat()
        val accelLat = (speedMps * Math.toRadians(gyroYaw.toDouble())).toFloat()

        val imuSample = ImuSample(
          timestampMs = now,
          accel = Vector3D(accelFwd, accelLat, 9.81f + potholeSpike + engineVibe * 0.5f),
          gyro = Vector3D(0.01f, 0.01f, gyroYaw)
        )

        // GNSS Sample
        val isGnssDenied = !wp.isGnssAvailable || _uiState.value.isManualGnssCut
        val gnssSample = GnssSample(
          timestampMs = now,
          point = wp.point,
          speedKmh = wp.speedKmh,
          headingDeg = wp.headingDeg,
          accuracyMeters = if (isGnssDenied) 999f else 2.5f,
          isLocked = !isGnssDenied,
          satelliteCount = if (isGnssDenied) 0 else 16,
          navicCount = if (isGnssDenied) 0 else 5,
          hdop = if (isGnssDenied) 9.9f else 0.8f
        )

        fusionEngine.updateGroundTruth(wp.point)
        fusionEngine.processGnssUpdate(gnssSample)

        val output = fusionEngine.processImuUpdate(
          imuSample = imuSample,
          candidateRoads = scenario.roadSegments,
          forcedGnssOutage = _uiState.value.isManualGnssCut
        )

        _uiState.update {
          it.copy(
            fusionOutput = output,
            simulationProgress = progress
          )
        }

        delay(sleepTimeMs)
      }
    }
  }

  private fun startLiveSensorLoop() {
    loopJob?.cancel()
    loopJob = viewModelScope.launch {
      launch {
        sensorService.liveGnssFlow().collect { gnss ->
          fusionEngine.processGnssUpdate(gnss)
        }
      }
      launch {
        sensorService.liveImuFlow(_uiState.value.sensorSamplingRate).collect { imu ->
          val output = fusionEngine.processImuUpdate(
            imuSample = imu,
            candidateRoads = _uiState.value.activeScenario.roadSegments,
            forcedGnssOutage = _uiState.value.isManualGnssCut
          )
          _uiState.update { it.copy(fusionOutput = output) }
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    loopJob?.cancel()
    sensorService.shutdown()
  }
}
