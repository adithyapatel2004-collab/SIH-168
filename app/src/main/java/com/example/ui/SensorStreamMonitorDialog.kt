package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.IndividualSensorStatus
import com.example.model.SensorSamplingRate
import com.example.model.SensorServiceDiagnostics
import com.example.model.Vector3D
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStreamMonitorDialog(
  diagnostics: SensorServiceDiagnostics,
  activeSamplingRate: SensorSamplingRate,
  onSelectRate: (SensorSamplingRate) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.92f)
        .clip(RoundedCornerShape(20.dp))
        .border(1.dp, SpaceNavyBorder, RoundedCornerShape(20.dp))
        .testTag("sensor_stream_monitor_dialog"),
      color = SpaceNavyDark
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = "Sensor Manager",
                tint = CyanAccent,
                modifier = Modifier.size(24.dp)
              )
            }
            Column {
              Text(
                text = "Android SensorManager Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "High-Frequency Raw IMU Streams (≥ 50 Hz)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_sensor_monitor_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Compliance Banner (≥ 50Hz Guarantee)
        Surface(
          color = if (diagnostics.averageThroughputHz >= 48f || diagnostics.isHighFrequencyAchieved) {
            GnssActiveGreen.copy(alpha = 0.12f)
          } else {
            SaffronOrange.copy(alpha = 0.12f)
          },
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (diagnostics.averageThroughputHz >= 48f || diagnostics.isHighFrequencyAchieved) {
              GnssActiveGreen.copy(alpha = 0.5f)
            } else {
              SaffronOrange.copy(alpha = 0.5f)
            }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(
                    if (diagnostics.averageThroughputHz >= 48f || diagnostics.isHighFrequencyAchieved) {
                      GnssActiveGreen
                    } else {
                      SaffronOrange
                    },
                    CircleShape
                  )
              )
              Column {
                Text(
                  text = if (diagnostics.averageThroughputHz >= 48f || diagnostics.isHighFrequencyAchieved) {
                    "HIGH FREQUENCY COMPLIANT (≥ 50 Hz)"
                  } else {
                    "SAMPLING INITIALIZING..."
                  },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (diagnostics.averageThroughputHz >= 48f || diagnostics.isHighFrequencyAchieved) {
                    GnssActiveGreen
                  } else {
                    SaffronOrange
                  }
                )
                Text(
                  text = "Throughput: ${String.format(Locale.US, "%.1f", diagnostics.averageThroughputHz)} Hz • Jitter: ${String.format(Locale.US, "%.2f", diagnostics.meanJitterMs)} ms • HandlerThread: Isolated",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Surface(
              color = SpaceNavySurface,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "${diagnostics.totalEventsCombined} pkts",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sampling Rate Selector (50Hz, 100Hz, 200Hz)
        Text(
          text = "Target Hardware Sampling Frequency:",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SensorSamplingRate.entries.forEach { rate ->
            val isSelected = rate == activeSamplingRate
            FilterChip(
              selected = isSelected,
              onClick = { onSelectRate(rate) },
              label = {
                Text(
                  text = rate.label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = if (isSelected) {
                {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                }
              } else null,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanAccent.copy(alpha = 0.25f),
                selectedLabelColor = CyanAccent,
                selectedLeadingIconColor = CyanAccent,
                containerColor = SpaceNavySurface,
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = if (isSelected) CyanAccent else SpaceNavyBorder,
                selectedBorderColor = CyanAccent,
                enabled = true,
                selected = isSelected
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable sensor channels
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Channel 1: Accelerometer
          SensorChannelCard(
            title = "Raw Accelerometer (Sensor.TYPE_ACCELEROMETER)",
            subtitle = "Linear specific force & gravity reference vector (m/s²)",
            status = diagnostics.accelStatus,
            accentColor = CyanAccent,
            icon = Icons.AutoMirrored.Filled.ShowChart,
            unit = "m/s²"
          )

          // Channel 2: Gyroscope
          SensorChannelCard(
            title = "Raw Gyroscope (Sensor.TYPE_GYROSCOPE)",
            subtitle = "Tri-axial angular rotation velocities (deg/s, rad/s)",
            status = diagnostics.gyroStatus,
            accentColor = SaffronOrange,
            icon = Icons.Default.Sync,
            unit = "°/s"
          )

          // Channel 3: Magnetometer
          SensorChannelCard(
            title = "Raw Magnetometer (Sensor.TYPE_MAGNETIC_FIELD)",
            subtitle = "Ambient geomagnetic flux density (microtesla, µT)",
            status = diagnostics.magStatus,
            accentColor = GroundTruthGold,
            icon = Icons.Default.Explore,
            unit = "µT"
          )

          // Background HandlerThread Details Card
          Surface(
            color = SpaceNavyCard,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceNavyBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Speed,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Architecture: High-Priority HandlerThread Isolation",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
              Text(
                text = "Capturing 50-200 Hz sensor streams on Android's Main Looper causes UI hitching and missed deadlines. This service registers callbacks to a dedicated background HandlerThread (Process.THREAD_PRIORITY_URGENT_AUDIO) and uses non-blocking concurrent Flows with backpressure buffers.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SensorChannelCard(
  title: String,
  subtitle: String,
  status: IndividualSensorStatus,
  accentColor: Color,
  icon: ImageVector,
  unit: String
) {
  Surface(
    color = SpaceNavyCard,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceNavyBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Channel Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(18.dp)
            )
          }
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              color = TextMuted
            )
          }
        }

        // Live Frequency Badge
        Surface(
          color = if (status.currentFrequencyHz >= 48f) GnssActiveGreen.copy(alpha = 0.2f) else SpaceNavySurface,
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (status.currentFrequencyHz >= 48f) GnssActiveGreen else SpaceNavyBorder
          )
        ) {
          Text(
            text = "${String.format(Locale.US, "%.1f", status.currentFrequencyHz)} Hz",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = if (status.currentFrequencyHz >= 48f) GnssActiveGreen else accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Live 3-Axis Readouts (X, Y, Z) and Norm
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AxisValuePill(label = "X", value = status.lastValues.x, unit = unit, color = RawDriftRed, modifier = Modifier.weight(1f))
        AxisValuePill(label = "Y", value = status.lastValues.y, unit = unit, color = GnssActiveGreen, modifier = Modifier.weight(1f))
        AxisValuePill(label = "Z", value = status.lastValues.z, unit = unit, color = CyanAccent, modifier = Modifier.weight(1f))
        AxisValuePill(label = "|V|", value = status.lastValues.magnitude(), unit = unit, color = GroundTruthGold, modifier = Modifier.weight(1.1f))
      }

      // Sensor Hardware Metadata
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Sensor: ${status.name} (${status.vendor})",
          fontSize = 10.sp,
          color = TextMuted
        )
        Text(
          text = "Min Delay: ${status.minDelayUs} µs • Max Rate: ${String.format(Locale.US, "%.0f", status.maxFrequencyHz)} Hz",
          fontSize = 10.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
private fun AxisValuePill(
  label: String,
  value: Float,
  unit: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SpaceNavySurface,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
      Text(
        text = String.format(Locale.US, "%+6.2f", value),
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = unit,
        fontSize = 9.sp,
        color = TextMuted
      )
    }
  }
}
