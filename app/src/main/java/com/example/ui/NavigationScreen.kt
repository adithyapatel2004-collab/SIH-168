package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeadReckoningViewModel
import com.example.engine.NavigationFusionOutput
import com.example.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(viewModel: DeadReckoningViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val output = uiState.fusionOutput

  val currentPos = output?.currentPosition ?: GeoPoint(12.97159, 77.59456)
  val heading = output?.headingDeg ?: 45f
  val mode = output?.mode ?: NavigationMode.GNSS_AIDED_INS
  val isGnssDenied = output?.isGnssDenied ?: false
  val metrics = output?.metrics

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("navigation_main_screen"),
    containerColor = SpaceNavyDark,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "IDR Navigation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Surface(
                color = SaffronOrange.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "ISRO 26168",
                  color = SaffronOrange,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Intelligent Dead Reckoning & GNSS+INS Fusion",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        },
        actions = {
          // NavIC & GPS Satellite Pill
          Surface(
            color = SpaceNavySurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(end = 6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(if (isGnssDenied) RawDriftRed else GnssActiveGreen, CircleShape)
              )
              Text(
                text = if (isGnssDenied) "BLACKOUT" else "NavIC (5) + GPS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGnssDenied) RawDriftRed else GnssActiveGreen
              )
            }
          }

          // Mount Calibration Dialog Button
          IconButton(
            onClick = { viewModel.setShowAlignmentDialog(true) },
            modifier = Modifier.testTag("mount_alignment_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Mount Alignment",
              tint = CyanAccent
            )
          }

          // Sensor Stream Monitor Dialog Button
          IconButton(
            onClick = { viewModel.setShowSensorStreamDialog(true) },
            modifier = Modifier.testTag("sensor_monitor_button")
          ) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = "High-Freq Sensor Streams",
              tint = GnssActiveGreen
            )
          }

          // Benchmark Report Dialog Button
          IconButton(
            onClick = { viewModel.setShowBenchmarkDialog(true) },
            modifier = Modifier.testTag("benchmark_report_button")
          ) {
            Icon(
              imageVector = Icons.Default.Assessment,
              contentDescription = "IO-VNBD Benchmark",
              tint = SaffronOrange
            )
          }

          // Edge Engine Specs Dialog Button
          IconButton(
            onClick = { viewModel.setShowEdgeExportDialog(true) },
            modifier = Modifier.testTag("edge_engine_button")
          ) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = "Edge Engine",
              tint = TextSecondary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceNavyDark)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 1. Mode Selector Chips & Scenario Selector
      ScenarioAndModeBar(
        activeScenario = uiState.activeScenario,
        isLive = uiState.isLiveSensorMode,
        onScenarioSelect = { viewModel.loadScenario(it) },
        onToggleLive = { viewModel.setLiveSensorMode(it) },
        hasHardwareSensors = uiState.hasHardwareSensors
      )

      // 2. High-Tech Telemetry HUD Bar
      TelemetryHud(
        output = output,
        rateHz = uiState.updateRateHz
      )

      // 3. Central Map Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        MapCanvas(
          currentPos = currentPos,
          headingDeg = heading,
          mode = mode,
          roadSegments = uiState.activeScenario.roadSegments,
          trajectoryHistory = viewModel.getTrajectoryHistory(),
          rawHistory = viewModel.getRawHistory(),
          groundTruthHistory = viewModel.getGroundTruthHistory(),
          isGnssDenied = isGnssDenied,
          modifier = Modifier.fillMaxSize()
        )

        // Seamless Transition Alert Pill (appears when transitioning into dead reckoning)
        if (isGnssDenied) {
          Surface(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 12.dp)
              .testTag("gnss_outage_banner"),
            color = RawDriftRed.copy(alpha = 0.9f),
            shape = RoundedCornerShape(20.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                contentDescription = "GNSS Outage",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "GNSS DENIED • DEAD RECKONING ACTIVE (<8ms switch)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 4. Bottom Mission Control Panel
      MissionControlPanel(
        isPlaying = uiState.isPlaying,
        isManualGnssCut = uiState.isManualGnssCut,
        playbackSpeed = uiState.playbackSpeed,
        updateRateHz = uiState.updateRateHz,
        onTogglePlay = { viewModel.togglePlayPause() },
        onRestart = { viewModel.restartScenario() },
        onToggleGnssCut = { viewModel.toggleManualGnssCut() },
        onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
        onRateSelected = { viewModel.setUpdateRateHz(it) }
      )
    }

    // Dialogs
    if (uiState.showAlignmentDialog) {
      val alignment = output?.alignment ?: com.example.model.AlignmentAngles()
      val vibration = output?.vibration ?: com.example.model.VibrationSpectrum()
      AlignmentVisualizerDialog(
        alignment = alignment,
        vibration = vibration,
        onPresetSelected = { viewModel.setMountPreset(it) },
        onSimulateSlip = { viewModel.simulateMountSlip() },
        onResetCalibration = { viewModel.resetMountCalibration() },
        onDismiss = { viewModel.setShowAlignmentDialog(false) }
      )
    }

    if (uiState.showSensorStreamDialog) {
      SensorStreamMonitorDialog(
        diagnostics = uiState.sensorDiagnostics,
        activeSamplingRate = uiState.sensorSamplingRate,
        onSelectRate = { viewModel.setSensorSamplingRate(it) },
        onDismiss = { viewModel.setShowSensorStreamDialog(false) }
      )
    }

    if (uiState.showBenchmarkDialog) {
      BenchmarkEvaluationDialog(
        metrics = metrics,
        onDismiss = { viewModel.setShowBenchmarkDialog(false) }
      )
    }

    if (uiState.showEdgeExportDialog) {
      EdgeEngineDialog(
        currentRateHz = uiState.updateRateHz,
        onRateSelected = { viewModel.setUpdateRateHz(it) },
        onDismiss = { viewModel.setShowEdgeExportDialog(false) }
      )
    }
  }
}

@Composable
private fun ScenarioAndModeBar(
  activeScenario: BenchmarkScenario,
  isLive: Boolean,
  onScenarioSelect: (BenchmarkScenario) -> Unit,
  onToggleLive: (Boolean) -> Unit,
  hasHardwareSensors: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SpaceNavyCard)
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Live Hardware Mode Chip
    FilterChip(
      selected = isLive,
      onClick = { onToggleLive(true) },
      label = { Text("Live IMU Sensors", fontSize = 11.sp) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Sensors,
          contentDescription = "Live Sensors",
          modifier = Modifier.size(14.dp)
        )
      },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = CyanAccent,
        selectedLabelColor = SpaceNavyDark,
        containerColor = SpaceNavySurface,
        labelColor = TextSecondary
      ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.testTag("live_sensor_chip")
    )

    // Benchmark Scenarios
    for (scenario in IovnbdRepository.scenarios) {
      val isSelected = (!isLive && activeScenario.id == scenario.id)
      FilterChip(
        selected = isSelected,
        onClick = {
          onToggleLive(false)
          onScenarioSelect(scenario)
        },
        label = { Text(scenario.title, fontSize = 11.sp) },
        leadingIcon = {
          Icon(
            imageVector = if (scenario.id.contains("tunnel")) Icons.Default.DirectionsTransit else Icons.Default.Navigation,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = SaffronOrange,
          selectedLabelColor = SpaceNavyDark,
          containerColor = SpaceNavySurface,
          labelColor = TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("scenario_chip_${scenario.id}")
      )
    }
  }
}

@Composable
private fun TelemetryHud(
  output: NavigationFusionOutput?,
  rateHz: Int
) {
  val speedKmh = output?.speedKmh ?: 0f
  val metrics = output?.metrics
  val mode = output?.mode ?: NavigationMode.GNSS_AIDED_INS

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = SpaceNavySurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceNavyBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Speed Gauge
      Column {
        Text(text = "AI SPEED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = String.format("%.1f", speedKmh),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = " km/h",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }
      }

      // Drift Metric (ISRO Benchmark: <10%)
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "ISRO DRIFT (<10%)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = if (metrics != null && metrics.outageDistanceMeters > 5f) {
              String.format("%.1fm (%.1f%%)", metrics.driftErrorMeters, metrics.driftPercentage)
            } else {
              "0.0m (0%)"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (metrics?.isWithinBenchmark != false) GnssActiveGreen else RawDriftRed,
            fontFamily = FontFamily.Monospace
          )
          if (metrics?.isWithinBenchmark != false) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Benchmark Met",
              tint = GnssActiveGreen,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      // Mode & Frequency
      Column(horizontalAlignment = Alignment.End) {
        Surface(
          color = when (mode) {
            NavigationMode.GNSS_AIDED_INS -> GnssActiveGreen.copy(alpha = 0.2f)
            NavigationMode.INTELLIGENT_DEAD_RECKONING -> CyanAccent.copy(alpha = 0.2f)
            NavigationMode.STANDSTILL_ZUPT -> SaffronOrange.copy(alpha = 0.2f)
          },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = mode.displayName,
            color = when (mode) {
              NavigationMode.GNSS_AIDED_INS -> GnssActiveGreen
              NavigationMode.INTELLIGENT_DEAD_RECKONING -> CyanAccent
              NavigationMode.STANDSTILL_ZUPT -> SaffronOrange
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Text(
          text = "${rateHz}Hz FUSION",
          fontSize = 10.sp,
          color = TextSecondary,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun MissionControlPanel(
  isPlaying: Boolean,
  isManualGnssCut: Boolean,
  playbackSpeed: Float,
  updateRateHz: Int,
  onTogglePlay: () -> Unit,
  onRestart: () -> Unit,
  onToggleGnssCut: () -> Unit,
  onSpeedSelected: (Float) -> Unit,
  onRateSelected: (Int) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = SpaceNavyCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceNavyBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // GNSS Outage Simulation Button (PRIMARY INTERACTION)
        Button(
          onClick = onToggleGnssCut,
          modifier = Modifier
            .weight(1.5f)
            .height(44.dp)
            .testTag("toggle_gnss_cut_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isManualGnssCut) GnssActiveGreen else RawDriftRed,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isManualGnssCut) Icons.Default.GpsFixed else Icons.Default.GpsOff,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = if (isManualGnssCut) "RESTORE GNSS" else "CUT GNSS (TEST)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }

        // Play/Pause Button
        FilledTonalIconButton(
          onClick = onTogglePlay,
          modifier = Modifier.testTag("play_pause_button")
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = CyanAccent
          )
        }

        // Restart Button
        FilledTonalIconButton(
          onClick = onRestart,
          modifier = Modifier.testTag("restart_button")
        ) {
          Icon(
            imageVector = Icons.Default.Replay,
            contentDescription = "Restart",
            tint = TextSecondary
          )
        }

        // Rate Switcher (10Hz vs 200Hz Edge)
        OutlinedButton(
          onClick = { onRateSelected(if (updateRateHz == 10) 200 else 10) },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.testTag("rate_switch_button")
        ) {
          Text(
            text = if (updateRateHz == 10) "10Hz" else "200Hz",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Secondary row: Playback Speed selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Simulation Speed:", fontSize = 11.sp, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          for (speed in listOf(1.0f, 2.0f, 5.0f)) {
            val isSelected = (playbackSpeed == speed)
            Surface(
              onClick = { onSpeedSelected(speed) },
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) CyanAccent else SpaceNavySurface,
              modifier = Modifier.testTag("speed_button_${speed.toInt()}x")
            ) {
              Text(
                text = "${speed.toInt()}x",
                color = if (isSelected) SpaceNavyDark else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}
