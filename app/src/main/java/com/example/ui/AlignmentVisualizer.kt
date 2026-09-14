package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AlignmentAngles
import com.example.model.MountAlignmentStatus
import com.example.model.MountPositionPreset
import com.example.model.VibrationSpectrum
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Tech In-Vehicle Mount Alignment & Calibration Studio.
 *
 * Visualizes and controls the automatic determination of smartphone
 * Pitch, Roll, and Yaw relative to the vehicle's driving direction.
 */
@Composable
fun AlignmentVisualizerDialog(
  alignment: AlignmentAngles,
  vibration: VibrationSpectrum,
  onPresetSelected: (MountPositionPreset) -> Unit,
  onSimulateSlip: () -> Unit,
  onResetCalibration: () -> Unit,
  onDismiss: () -> Unit
) {
  var showDcmMatrix by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .testTag("alignment_dialog"),
      colors = CardDefaults.cardColors(containerColor = SpaceNavyDark),
      shape = RoundedCornerShape(24.dp),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = androidx.compose.ui.graphics.SolidColor(if (alignment.isSlipDetected) SaffronOrange else SpaceNavyBorder)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (alignment.isSlipDetected) SaffronOrange.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (alignment.isSlipDetected) Icons.Default.Warning else Icons.Default.Tune,
                contentDescription = "Alignment Engine",
                tint = if (alignment.isSlipDetected) SaffronOrange else CyanAccent,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "In-Vehicle Mount Calibration",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Automatic Body-to-Chassis Coordinate Leveling (PS 26168)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("alignment_dialog_close_icon")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondary
            )
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = SpaceNavyBorder
        )

        // Scrollable Body
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // 1. Mount Position Presets Bar
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "MOUNTING POSITION PRESETS",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextMuted,
              letterSpacing = 1.sp
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MountPositionPreset.entries.forEach { preset ->
                val isSelected = alignment.preset == preset
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SpaceNavyCard,
                  border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CyanAccent else SpaceNavyBorder)
                  ),
                  modifier = Modifier
                    .clickable { onPresetSelected(preset) }
                    .testTag("preset_${preset.name.lowercase()}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = when (preset) {
                        MountPositionPreset.AUTO_DETECT -> Icons.Default.AutoMode
                        MountPositionPreset.DASHBOARD_FLAT -> Icons.Default.DirectionsCar
                        MountPositionPreset.WINDSHIELD_CRADLE -> Icons.Default.Navigation
                        MountPositionPreset.AIR_VENT_MAGNETIC -> Icons.Default.Air
                        MountPositionPreset.CONSOLE_CUPHOLDER -> Icons.Default.LocalDrink
                        MountPositionPreset.LANDSCAPE_CRADLE -> Icons.Default.ScreenRotation
                      },
                      contentDescription = null,
                      tint = if (isSelected) CyanAccent else TextSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = preset.label,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) CyanAccent else TextPrimary
                    )
                  }
                }
              }
            }
          }

          // 2. Status & Watchdog Banner
          val statusColor = when (alignment.status) {
            MountAlignmentStatus.ALIGNED -> GnssActiveGreen
            MountAlignmentStatus.CALIBRATING -> SaffronOrange
            MountAlignmentStatus.SLIP_RECALIBRATING -> SaffronOrange
            MountAlignmentStatus.UNALIGNED -> RawDriftRed
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SpaceNavyCard)
              .border(
                1.dp,
                if (alignment.isSlipDetected) SaffronOrange else SpaceNavyBorder,
                RoundedCornerShape(12.dp)
              )
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                )
                Text(
                  text = alignment.status.displayName.uppercase(),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = statusColor
                )
              }

              Text(
                text = String.format("%.0f%% Calibrated", alignment.calibrationProgress * 100f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
              )
            }

            Text(
              text = alignment.stageDescription,
              fontSize = 11.sp,
              color = TextSecondary
            )

            LinearProgressIndicator(
              progress = { alignment.calibrationProgress.coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = if (alignment.isSlipDetected) SaffronOrange else CyanAccent,
              trackColor = SpaceNavySurface
            )

            // Watchdog Line
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = if (alignment.isSlipDetected) SaffronOrange else GnssActiveGreen,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = if (alignment.isSlipDetected) "CRADLE SLIP WATCHDOG: RE-ALIGNMENT TRIGGERED" else "CRADLE SLIP WATCHDOG: ARMED (14° Auto-Realign)",
                  fontSize = 10.sp,
                  color = if (alignment.isSlipDetected) SaffronOrange else GnssActiveGreen,
                  fontWeight = FontWeight.SemiBold
                )
              }

              Text(
                text = "Stability: ${(alignment.mountStabilityScore * 100f).toInt()}%",
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          // 3. Graphic Horizon & 3D Mount Canvas
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(SpaceNavyBorder)
            )
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              MountHorizonCanvas(
                pitchDeg = alignment.pitchDeg,
                rollDeg = alignment.rollDeg,
                yawDeg = alignment.yawDeg,
                modifier = Modifier.fillMaxSize()
              )

              // Overlay tags
              Column(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .padding(10.dp)
              ) {
                Text(
                  text = "ATTITUDE LEVELING HORIZON",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = CyanAccent,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "Vehicle Frame ↔ Phone IMU Frame",
                  fontSize = 8.sp,
                  color = TextMuted
                )
              }

              Column(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(10.dp),
                horizontalAlignment = Alignment.End
              ) {
                Text(
                  text = "Gravity: ${String.format("%.2f", alignment.gravityMagnitude)} m/s²",
                  fontSize = 9.sp,
                  color = TextSecondary,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  text = "Confidence: ${(alignment.confidence * 100f).toInt()}%",
                  fontSize = 9.sp,
                  color = GnssActiveGreen,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          // 4. Detailed Angle Metric Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AngleMetricCard(
              label = "PITCH (θ)",
              deg = alignment.pitchDeg,
              sub = "Tilt Up / Down",
              icon = Icons.Default.Height,
              modifier = Modifier.weight(1f)
            )
            AngleMetricCard(
              label = "ROLL (ϕ)",
              deg = alignment.rollDeg,
              sub = "Lateral Tilt",
              icon = Icons.Default.SwapHoriz,
              modifier = Modifier.weight(1f)
            )
            AngleMetricCard(
              label = "YAW (ψ)",
              deg = alignment.yawDeg,
              sub = "Forward Heading",
              icon = Icons.Default.Explore,
              modifier = Modifier.weight(1f)
            )
          }

          // 5. Multi-Stage Automated Calibration Pipeline Stepper
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(SpaceNavyCard)
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "CALIBRATION PIPELINE STAGES",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )

            PipelineStageRow(
              stageNumber = "1",
              title = "Static Gravity-Vector Leveling (Pitch & Roll)",
              subtitle = "Quasi-static window integration to isolate 1G vertical gravity",
              isComplete = alignment.calibrationProgress >= 0.2f
            )

            PipelineStageRow(
              stageNumber = "2",
              title = "Dynamic Forward Acceleration PCA (Yaw)",
              subtitle = "Eigen-covariance analysis on dynamic longitudinal vehicle impulses",
              isComplete = alignment.calibrationProgress >= 0.7f
            )

            PipelineStageRow(
              stageNumber = "3",
              title = "Centripetal Turn Cross-Validation",
              subtitle = "Right-hand rule sign check against cornering yaw rate (ωz)",
              isComplete = alignment.calibrationProgress >= 0.95f
            )

            PipelineStageRow(
              stageNumber = "4",
              title = "Cradle Slip & Mount Shift Watchdog",
              subtitle = "Continuous 14° angular divergence tracking for instant re-alignment",
              isComplete = true
            )
          }

          // 6. Direction Cosine Matrix (DCM) 3x3 Expandable Card
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SpaceNavyCard)
              .clickable { showDcmMatrix = !showDcmMatrix }
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.GridOn,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "3D Transformation Matrix R_b^v (DCM)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
              }

              Icon(
                imageVector = if (showDcmMatrix) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary
              )
            }

            if (showDcmMatrix) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Maps raw sensor accelerations from phone body frame [x_b, y_b, z_b] to vehicle chassis frame [X_forward, Y_lateral, Z_down]:",
                fontSize = 10.sp,
                color = TextSecondary
              )
              Spacer(modifier = Modifier.height(6.dp))

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(SpaceNavySurface)
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                alignment.rotationMatrix.forEachIndexed { rowIdx, row ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                  ) {
                    row.forEach { valItem ->
                      Text(
                        text = String.format("%+6.3f", valItem),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (valItem > 0.5f) CyanAccent else TextPrimary
                      )
                    }
                  }
                }
              }
            }
          }

          // 7. Road Vibration & Shock Spectrum
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SpaceNavyCard)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "AI VIBRATION & SHOCK FILTERING",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextMuted,
              letterSpacing = 1.sp
            )

            SpectrumBar(
              label = "Engine Combustion Harmonics (15-45 Hz)",
              value = vibration.engineHarmonicPower,
              color = SaffronOrange
            )
            SpectrumBar(
              label = "Chassis & Road Roughness (RMS)",
              value = (vibration.roadRoughnessRms / 2.0f).coerceIn(0f, 1f),
              color = CyanAccent
            )
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 10.dp),
          color = SpaceNavyBorder
        )

        // Bottom Action Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Simulate Slip Button
          OutlinedButton(
            onClick = onSimulateSlip,
            modifier = Modifier
              .weight(1f)
              .testTag("simulate_mount_slip_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronOrange),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(SaffronOrange.copy(alpha = 0.7f))
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ScreenRotation,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Simulate Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // Reset Calibration Button
          OutlinedButton(
            onClick = onResetCalibration,
            modifier = Modifier
              .weight(1f)
              .testTag("reset_calibration_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(SpaceNavyBorder)
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Re-Align", fontSize = 11.sp)
          }

          // Done / Close Button
          Button(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .testTag("alignment_dialog_close_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = CyanAccent,
              contentColor = SpaceNavyDark
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Custom Canvas drawing an Artificial Horizon / Attitude Visualizer
 * combined with a side-by-side Phone in Vehicle Mount Graphic.
 */
@Composable
private fun MountHorizonCanvas(
  pitchDeg: Float,
  rollDeg: Float,
  yawDeg: Float,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Background Sky/Ground Horizon Box on Left half
    val horizonW = w * 0.48f
    val horizonCenter = Offset(horizonW / 2f + 12f, h / 2f)

    // Clip to rounded horizon box
    drawRoundRect(
      color = SpaceNavySurface,
      topLeft = Offset(12f, 12f),
      size = Size(horizonW - 12f, h - 24f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )

    // Artificial Horizon Line & Pitch Ladder
    rotate(degrees = -rollDeg, pivot = horizonCenter) {
      val pitchOffset = (pitchDeg / 90f) * (h * 0.35f)

      // Sky Line (Cyan)
      drawLine(
        color = CyanAccent,
        start = Offset(horizonCenter.x - 50f, horizonCenter.y + pitchOffset),
        end = Offset(horizonCenter.x + 50f, horizonCenter.y + pitchOffset),
        strokeWidth = 3f,
        cap = StrokeCap.Round
      )

      // Pitch Ladder ticks
      for (angle in listOf(-30, -15, 15, 30)) {
        val yPos = horizonCenter.y + pitchOffset - (angle / 90f) * (h * 0.35f)
        val tickWidth = if (angle % 30 == 0) 30f else 18f
        drawLine(
          color = TextMuted,
          start = Offset(horizonCenter.x - tickWidth, yPos),
          end = Offset(horizonCenter.x + tickWidth, yPos),
          strokeWidth = 1.5f,
          cap = StrokeCap.Round
        )
      }
    }

    // Vehicle Fixed Center Reticle (Gold/Saffron Crosshair)
    drawLine(
      color = SaffronOrange,
      start = Offset(horizonCenter.x - 24f, horizonCenter.y),
      end = Offset(horizonCenter.x - 8f, horizonCenter.y),
      strokeWidth = 3f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = SaffronOrange,
      start = Offset(horizonCenter.x + 8f, horizonCenter.y),
      end = Offset(horizonCenter.x + 24f, horizonCenter.y),
      strokeWidth = 3f,
      cap = StrokeCap.Round
    )
    drawCircle(
      color = SaffronOrange,
      radius = 3.5f,
      center = horizonCenter
    )

    // RIGHT HALF: 3D Isometric Phone in Mount Graphic
    val mountCenterX = w * 0.74f
    val mountCenterY = h / 2f

    // Vehicle Dashboard Profile
    val dashPath = Path().apply {
      moveTo(mountCenterX - 60f, mountCenterY + 45f)
      lineTo(mountCenterX + 60f, mountCenterY + 45f)
      lineTo(mountCenterX + 80f, mountCenterY + 55f)
      lineTo(mountCenterX - 80f, mountCenterY + 55f)
      close()
    }
    drawPath(dashPath, color = SpaceNavyBorder)

    // Phone Holder Base
    drawRect(
      color = SpaceNavySurface,
      topLeft = Offset(mountCenterX - 12f, mountCenterY + 25f),
      size = Size(24f, 20f)
    )

    // Tilted Phone Body (Rotated by Pitch and Roll)
    rotate(degrees = pitchDeg * 0.6f, pivot = Offset(mountCenterX, mountCenterY)) {
      // Phone Body
      drawRoundRect(
        color = CyanAccent,
        topLeft = Offset(mountCenterX - 24f, mountCenterY - 45f),
        size = Size(48f, 75f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(width = 2.5f)
      )

      // Screen Fill
      drawRoundRect(
        color = CyanAccent.copy(alpha = 0.15f),
        topLeft = Offset(mountCenterX - 21f, mountCenterY - 42f),
        size = Size(42f, 69f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
      )

      // Vehicle Forward Driving Vector Arrow
      val arrowStart = Offset(mountCenterX, mountCenterY - 15f)
      val arrowEnd = Offset(mountCenterX + 45f, mountCenterY - 30f)
      drawLine(
        color = GnssActiveGreen,
        start = arrowStart,
        end = arrowEnd,
        strokeWidth = 3f,
        cap = StrokeCap.Round
      )
      // Arrow head
      drawCircle(
        color = GnssActiveGreen,
        radius = 4f,
        center = arrowEnd
      )
    }

    // Vehicle Forward Direction Label
    drawLine(
      color = GnssActiveGreen,
      start = Offset(mountCenterX - 50f, mountCenterY - 55f),
      end = Offset(mountCenterX - 25f, mountCenterY - 55f),
      strokeWidth = 2f
    )
  }
}

@Composable
private fun PipelineStageRow(
  stageNumber: String,
  title: String,
  subtitle: String,
  isComplete: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(if (isComplete) GnssActiveGreen.copy(alpha = 0.2f) else SpaceNavySurface),
      contentAlignment = Alignment.Center
    ) {
      if (isComplete) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = GnssActiveGreen,
          modifier = Modifier.size(14.dp)
        )
      } else {
        Text(
          text = stageNumber,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextMuted
        )
      }
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isComplete) TextPrimary else TextSecondary
      )
      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun AngleMetricCard(
  label: String,
  deg: Float,
  sub: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(SpaceNavyCard)
      .border(1.dp, SpaceNavyBorder, RoundedCornerShape(12.dp))
      .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = CyanAccent,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = label,
        fontSize = 10.sp,
        color = TextMuted,
        fontWeight = FontWeight.Bold
      )
    }

    Text(
      text = String.format("%+.1f°", deg),
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary,
      fontFamily = FontFamily.Monospace
    )

    Text(
      text = sub,
      fontSize = 9.sp,
      color = TextSecondary
    )
  }
}

@Composable
private fun SpectrumBar(label: String, value: Float, color: Color) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = label, fontSize = 10.sp, color = TextSecondary)
      Text(
        text = String.format("%.0f%%", value * 100f),
        fontSize = 10.sp,
        color = TextPrimary,
        fontFamily = FontFamily.Monospace
      )
    }
    LinearProgressIndicator(
      progress = { value.coerceIn(0f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = color,
      trackColor = SpaceNavySurface
    )
  }
}
