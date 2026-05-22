package com.lokalapps.lens.internal.presentation.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.R
import kotlin.math.roundToInt

/**
 * Lens floating bubble that appears in the corner of every screen.
 *
 * Features:
 * - Draggable to any position
 * - Tap to open Lens dashboard
 * - Semi-transparent when idle, fully visible when interacting
 * - Positioned at bottom-right by default
 *
 * This bubble is injected into each Activity's content view, so it doesn't require
 * SYSTEM_ALERT_WINDOW permission.
 */
@Composable
internal fun LensBubble(onClick: () -> Unit, modifier: Modifier = Modifier) {
  // Track container size for drag bounds
  var containerSize by remember { mutableStateOf(IntSize.Zero) }

  // Bubble size in pixels (density-aware)
  val density = LocalDensity.current
  val bubbleSizePx = remember(density) { with(density) { 56.dp.toPx() }.toInt() }

  // Drag offset from initial position (bottom-right)
  var dragOffsetX by remember { mutableFloatStateOf(0f) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  var isDragging by remember { mutableStateOf(false) }

  // Animate opacity: more visible when dragging
  val alpha by
      animateFloatAsState(targetValue = if (isDragging) 1f else 0.8f, label = "bubble_alpha")

  // Animate scale: slightly larger when dragging
  val scale by
      animateFloatAsState(targetValue = if (isDragging) 1.1f else 1f, label = "bubble_scale")

  Box(
      modifier = modifier.fillMaxSize().onSizeChanged { containerSize = it },
      contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier =
                Modifier.padding(end = 16.dp, bottom = 100.dp)
                    .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                    .scale(scale)
                    .alpha(alpha)
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                      detectDragGestures(
                          onDragStart = { isDragging = true },
                          onDragEnd = { isDragging = false },
                          onDragCancel = { isDragging = false },
                          onDrag = { change, dragAmount ->
                            change.consume()
                            // Allow dragging within reasonable bounds
                            val maxDragX = containerSize.width - bubbleSizePx - 32
                            val maxDragY = containerSize.height - bubbleSizePx - 200

                            dragOffsetX =
                                (dragOffsetX + dragAmount.x).coerceIn(-maxDragX.toFloat(), 0f)
                            dragOffsetY =
                                (dragOffsetY + dragAmount.y).coerceIn(-maxDragY.toFloat(), 100f)
                          })
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
            contentAlignment = Alignment.Center) {
              Icon(
                  painter = painterResource(id = R.drawable.ic_lens_aperture),
                  contentDescription = "Lens",
                  tint = Color.White,
                  modifier = Modifier.size(28.dp))
            }
      }
}
