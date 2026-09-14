package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun EdgeEngineDialog(
  currentRateHz: Int,
  onRateSelected: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
        .testTag("edge_engine_dialog"),
      colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
      shape = RoundedCornerShape(20.dp),
      border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SpaceNavyBorder))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Title
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Edge Engine",
            tint = CyanAccent
          )
          Column {
            Text(
              text = "Edge Deployable Software Engine",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "FOG IMU & External Sensor Fusion (200 Hz)",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }
        }

        Divider(color = SpaceNavyBorder)

        // Processing Pipeline Mode Selector
        Text(
          text = "Select Engine Deployment Profile:",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ProfileOptionCard(
            title = "Smartphone IMU",
            rate = "10 Hz Rate",
            desc = "Consumer MEMS sensor profile",
            selected = currentRateHz == 10,
            onClick = { onRateSelected(10) },
            modifier = Modifier.weight(1f)
          )
          ProfileOptionCard(
            title = "Edge FOG Engine",
            rate = "200 Hz Rate",
            desc = "Tactical Fiber Optic Gyroscope",
            selected = currentRateHz == 200,
            onClick = { onRateSelected(200) },
            modifier = Modifier.weight(1f)
          )
        }

        // Edge Architecture Specifications
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceNavySurface)
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Edge Engine Architecture & Interface",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent
          )

          SpecRow(title = "Language & Bindings", value = "C++20 / Rust with JNI & ONNX Runtime")
          SpecRow(title = "Hardware Support", value = "Smartphone IMU, CAN-Bus, External RS422 FOG")
          SpecRow(title = "Inference Footprint", value = "< 1.4 MB compiled binary, 4% CPU on ARM64")
          SpecRow(title = "Zero Speed Update (ZUPT)", value = "Standstill neural detector & adaptive threshold")
          SpecRow(title = "Map-Matching Format", value = "Offline OSM GeoPackage / Topological Graph")
        }

        // C++ Sample Code Snippet for Edge Export
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SpaceNavyDark)
            .padding(10.dp)
        ) {
          Text(
            text = "// Edge FOG Pipeline API (200Hz Loop)",
            fontSize = 9.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "IdrEdgeEngine engine;\n" +
                "engine.init(lat, lon, heading_deg);\n" +
                "while (fog_sensor.read(&sample)) {\n" +
                "  auto out = engine.step(sample.accel, sample.gyro, dt_200hz);\n" +
                "  if (gnss.has_fix()) engine.fuse_gnss(gnss.fix);\n" +
                "}",
            fontSize = 10.sp,
            color = MetricText,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
          )
        }

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edge_engine_dialog_close_button"),
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SpaceNavyDark),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ProfileOptionCard(
  title: String,
  rate: String,
  desc: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      width = if (selected) 2.dp else 1.dp,
      color = if (selected) CyanAccent else SpaceNavyBorder
    ),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = if (selected) SpaceNavySurface else SpaceNavyDark
    ),
    contentPadding = PaddingValues(10.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) CyanAccent else TextPrimary
      )
      Text(
        text = rate,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (selected) GnssActiveGreen else TextSecondary,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = desc,
        fontSize = 9.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun SpecRow(title: String, value: String) {
  Column {
    Text(text = title, fontSize = 9.sp, color = TextMuted)
    Text(text = value, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
  }
}
