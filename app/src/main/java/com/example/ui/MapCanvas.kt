package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun MapCanvas(
  currentPos: GeoPoint,
  headingDeg: Float,
  mode: NavigationMode,
  roadSegments: List<RoadSegment>,
  trajectoryHistory: List<TrajectoryBreadcrumb>,
  rawHistory: List<GeoPoint>,
  groundTruthHistory: List<GeoPoint>,
  isGnssDenied: Boolean,
  modifier: Modifier = Modifier
) {
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = modifier
      .background(SpaceNavyDark)
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
          panOffset = Offset(
            x = panOffset.x + pan.x,
            y = panOffset.y + pan.y
          )
        }
      }
      .testTag("map_canvas_view")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasCenter = Offset(size.width / 2f, size.height / 2f)
      val effectiveCenter = Offset(canvasCenter.x + panOffset.x, canvasCenter.y + panOffset.y)

      // Dynamic scale: meters to pixels
      val metersToPixels = (size.minDimension / 500f) * zoomScale

      fun geoToCanvas(geo: GeoPoint): Offset {
        // Flat projection relative to current vehicle position
        val r = 6371000.0
        val lat0 = Math.toRadians(currentPos.lat)
        val dxMeters = Math.toRadians(geo.lon - currentPos.lon) * r * cos(lat0)
        val dyMeters = Math.toRadians(geo.lat - currentPos.lat) * r

        // Invert Y for screen coordinates (North is up)
        val screenX = effectiveCenter.x + (dxMeters * metersToPixels).toFloat()
        val screenY = effectiveCenter.y - (dyMeters * metersToPixels).toFloat()
        return Offset(screenX, screenY)
      }

      // 1. Draw subtle background radar grid
      drawGridLines(effectiveCenter, metersToPixels)

      // 2. Draw Road Network Segments
      for (segment in roadSegments) {
        val startScreen = geoToCanvas(segment.start)
        val endScreen = geoToCanvas(segment.end)

        // Road roadbed
        val roadColor = if (segment.isTunnel) TunnelGrey else RoadAsphalt
        val roadBorderColor = if (segment.isTunnel) CyanAccent.copy(alpha = 0.4f) else RoadMarking.copy(alpha = 0.5f)

        drawLine(
          color = roadColor,
          start = startScreen,
          end = endScreen,
          strokeWidth = 28f * zoomScale,
          cap = StrokeCap.Round
        )

        // Road boundary stripes
        drawLine(
          color = roadBorderColor,
          start = startScreen,
          end = endScreen,
          strokeWidth = 30f * zoomScale,
          cap = StrokeCap.Round,
          pathEffect = if (segment.isTunnel) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null
        )

        // Center line
        drawLine(
          color = if (segment.isTunnel) CyanAccent.copy(alpha = 0.7f) else RoadMarking,
          start = startScreen,
          end = endScreen,
          strokeWidth = 2f * zoomScale,
          cap = StrokeCap.Round,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
      }

      // 3. Draw Ground Truth Trajectory (Gold)
      if (groundTruthHistory.size > 1) {
        val path = Path()
        for (i in groundTruthHistory.indices) {
          val pt = geoToCanvas(groundTruthHistory[i])
          if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(
          path = path,
          color = GroundTruthGold.copy(alpha = 0.65f),
          style = Stroke(
            width = 3.5f * zoomScale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
          )
        )
      }

      // 4. Draw Raw IMU Unfiltered Trajectory (Red Drift)
      if (rawHistory.size > 1) {
        val rawPath = Path()
        for (i in rawHistory.indices) {
          val pt = geoToCanvas(rawHistory[i])
          if (i == 0) rawPath.moveTo(pt.x, pt.y) else rawPath.lineTo(pt.x, pt.y)
        }
        drawPath(
          path = rawPath,
          color = RawDriftRed.copy(alpha = 0.55f),
          style = Stroke(
            width = 2.5f * zoomScale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
          )
        )
      }

      // 5. Draw Intelligent Dead Reckoning Trajectory (Glowing Cyan)
      if (trajectoryHistory.size > 1) {
        val idrPath = Path()
        for (i in trajectoryHistory.indices) {
          val pt = geoToCanvas(trajectoryHistory[i].point)
          if (i == 0) idrPath.moveTo(pt.x, pt.y) else idrPath.lineTo(pt.x, pt.y)
        }
        // Glow effect
        drawPath(
          path = idrPath,
          color = DeadReckoningCyan.copy(alpha = 0.25f),
          style = Stroke(width = 8f * zoomScale)
        )
        // Core line
        drawPath(
          path = idrPath,
          color = if (mode == NavigationMode.INTELLIGENT_DEAD_RECKONING) DeadReckoningCyan else GnssActiveGreen,
          style = Stroke(width = 4f * zoomScale, cap = StrokeCap.Round)
        )
      }

      // 6. Draw Vehicle Icon at Effective Center
      val vehiclePos = effectiveCenter
      rotate(degrees = headingDeg, pivot = vehiclePos) {
        // Headlight beam
        val beamPath = Path().apply {
          moveTo(vehiclePos.x, vehiclePos.y)
          lineTo(vehiclePos.x - 30f * zoomScale, vehiclePos.y - 120f * zoomScale)
          lineTo(vehiclePos.x + 30f * zoomScale, vehiclePos.y - 120f * zoomScale)
          close()
        }
        drawPath(
          path = beamPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              (if (isGnssDenied) CyanAccent else GnssActiveGreen).copy(alpha = 0.35f),
              Color.Transparent
            ),
            startY = vehiclePos.y,
            endY = vehiclePos.y - 120f * zoomScale
          )
        )

        // Vehicle Chassis
        val carLength = 36f * zoomScale
        val carWidth = 18f * zoomScale

        // Uncertainty ellipse if in dead reckoning
        if (isGnssDenied) {
          drawOval(
            color = CyanAccent.copy(alpha = 0.2f),
            topLeft = Offset(vehiclePos.x - carWidth * 1.5f, vehiclePos.y - carLength * 1.5f),
            size = androidx.compose.ui.geometry.Size(carWidth * 3f, carLength * 3f),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
          )
        }

        // Body rectangle with rounded corners
        drawRoundRect(
          color = SpaceNavyDark,
          topLeft = Offset(vehiclePos.x - carWidth / 2f, vehiclePos.y - carLength / 2f),
          size = androidx.compose.ui.geometry.Size(carWidth, carLength),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
          color = if (isGnssDenied) CyanAccent else GnssActiveGreen,
          topLeft = Offset(vehiclePos.x - carWidth / 2f, vehiclePos.y - carLength / 2f),
          size = androidx.compose.ui.geometry.Size(carWidth, carLength),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
          style = Stroke(width = 2.5f * zoomScale)
        )

        // Windshield
        drawLine(
          color = Color.White.copy(alpha = 0.8f),
          start = Offset(vehiclePos.x - carWidth * 0.35f, vehiclePos.y - carLength * 0.2f),
          end = Offset(vehiclePos.x + carWidth * 0.35f, vehiclePos.y - carLength * 0.2f),
          strokeWidth = 2.5f * zoomScale
        )
      }

      // 7. Draw Compass Rose
      drawCompass(size.width - 45.dp.toPx(), 45.dp.toPx())
    }

    // Legend overlay in corner
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(12.dp)
        .background(SpaceNavyCard.copy(alpha = 0.88f), MaterialTheme.shapes.small)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      androidx.compose.foundation.layout.Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
      ) {
        LegendRow(color = DeadReckoningCyan, label = "AI-ML Dead Reckoning (NHC + Map)")
        LegendRow(color = GroundTruthGold, label = "Ground Truth (IO-VNBD Ref)")
        LegendRow(color = RawDriftRed, label = "Raw IMU (Uncorrected Drift)")
        LegendRow(color = TunnelGrey, label = "Tunnel / Blackout Corridor")
      }
    }
  }
}

@Composable
private fun LegendRow(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .background(color, androidx.compose.foundation.shape.CircleShape)
    )
    Text(
      text = label,
      color = TextSecondary,
      fontSize = 10.sp,
      fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
  }
}

private fun DrawScope.drawGridLines(center: Offset, metersToPixels: Float) {
  val stepPixels = 100f * metersToPixels // 100 meter rings
  val gridColor = SpaceNavyBorder.copy(alpha = 0.35f)

  for (radius in listOf(stepPixels, stepPixels * 2, stepPixels * 3)) {
    drawCircle(
      color = gridColor,
      center = center,
      radius = radius,
      style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f))
    )
  }
}

private fun DrawScope.drawCompass(cx: Float, cy: Float) {
  val center = Offset(cx, cy)
  val radius = 22f

  drawCircle(
    color = SpaceNavyCard.copy(alpha = 0.8f),
    center = center,
    radius = radius
  )
  drawCircle(
    color = SpaceNavyBorder,
    center = center,
    radius = radius,
    style = Stroke(width = 1.5f)
  )

  // North needle
  val pathNorth = Path().apply {
    moveTo(cx, cy - radius + 4f)
    lineTo(cx - 5f, cy)
    lineTo(cx + 5f, cy)
    close()
  }
  drawPath(path = pathNorth, color = SaffronOrange)

  // South needle
  val pathSouth = Path().apply {
    moveTo(cx, cy + radius - 4f)
    lineTo(cx - 5f, cy)
    lineTo(cx + 5f, cy)
    close()
  }
  drawPath(path = pathSouth, color = TextMuted)
}
