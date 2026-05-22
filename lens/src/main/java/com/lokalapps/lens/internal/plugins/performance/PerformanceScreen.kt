package com.lokalapps.lens.internal.plugins.performance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Compose screen that displays real-time performance metrics gathered by [FpsMonitor] and
 * [MemoryMonitor].
 */
@Composable
internal fun PerformanceScreen(
    fpsMonitor: FpsMonitor?,
    memoryMonitor: MemoryMonitor?,
) {
  val fps by (fpsMonitor?.fps ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
  val jankFrames by
      (fpsMonitor?.jankFrames ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
  val fpsHistory by
      (fpsMonitor?.fpsHistory ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<Int>()))
          .collectAsState()
  val memSnapshot by
      (memoryMonitor?.snapshot
              ?: kotlinx.coroutines.flow.MutableStateFlow(MemoryMonitor.MemorySnapshot(0, 0, 0, 0)))
          .collectAsState()
  val memHistory by
      (memoryMonitor?.usedHeapHistory
              ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<Long>()))
          .collectAsState()

  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // ── FPS Section ──────────────────────────────────────
    MetricSection(title = "Frame Rate") {
      MetricRow("Current FPS", "$fps")
      MetricRow("Jank Frames", "$jankFrames")
      MetricRow(
          "Status",
          when {
            fps >= 55 -> "Smooth"
            fps >= 45 -> "Acceptable"
            fps >= 30 -> "Sluggish"
            fps > 0 -> "Poor"
            else -> "Measuring..."
          },
      )

      if (fpsHistory.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FPS History (last ${fpsHistory.size}s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SparklineChart(
            data = fpsHistory.map { it.toFloat() },
            maxValue = 65f,
            lineColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(80.dp),
        )
      }
    }

    // ── Memory Section ───────────────────────────────────
    MetricSection(title = "Memory") {
      MetricRow("Used Heap", formatBytes(memSnapshot.usedHeapBytes))
      MetricRow("Total Heap", formatBytes(memSnapshot.totalHeapBytes))
      MetricRow("Max Heap", formatBytes(memSnapshot.maxHeapBytes))
      MetricRow("Native Heap", formatBytes(memSnapshot.nativeHeapBytes))

      val heapPercent =
          if (memSnapshot.maxHeapBytes > 0) {
            (memSnapshot.usedHeapBytes * 100 / memSnapshot.maxHeapBytes).toInt()
          } else {
            0
          }
      MetricRow("Heap Usage", "$heapPercent%")

      if (memHistory.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Heap History (last ${memHistory.size}s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SparklineChart(
            data = memHistory.map { it.toFloat() },
            maxValue = memSnapshot.maxHeapBytes.toFloat().coerceAtLeast(1f),
            lineColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth().height(80.dp),
        )
      }
    }
  }
}

/** Card-based section with a title header, matching AppInfoScreen style. */
@Composable
private fun MetricSection(
    title: String,
    content: @Composable () -> Unit,
) {
  Card(
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
          ),
      modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

/** Label + monospace value row, matching AppInfoScreen InfoRow style. */
@Composable
private fun MetricRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/**
 * Minimal sparkline chart that draws a polyline of [data] points.
 *
 * @param data Float values to plot.
 * @param maxValue The Y-axis ceiling (values above this are clamped).
 * @param lineColor Stroke color for the polyline.
 */
@Composable
private fun SparklineChart(
    data: List<Float>,
    maxValue: Float,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    if (data.size < 2) return@Canvas

    val width = size.width
    val height = size.height
    val stepX = width / (data.size - 1).coerceAtLeast(1)

    // Draw grid lines at 25%, 50%, 75%
    val gridColor = lineColor.copy(alpha = 0.15f)
    for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
      val y = height * (1f - fraction)
      drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(width, y),
          strokeWidth = 1f,
      )
    }

    // Draw sparkline path
    val path = Path()
    data.forEachIndexed { index, value ->
      val x = index * stepX
      val normalized = (value / maxValue).coerceIn(0f, 1f)
      val y = height * (1f - normalized)
      if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 2.dp.toPx()),
    )
  }
}

/** Formats bytes into a human-readable string (KB, MB, GB). */
private fun formatBytes(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> "%.2f GB".format(gb)
    mb >= 1.0 -> "%.1f MB".format(mb)
    kb >= 1.0 -> "%.1f KB".format(kb)
    else -> "$bytes B"
  }
}
