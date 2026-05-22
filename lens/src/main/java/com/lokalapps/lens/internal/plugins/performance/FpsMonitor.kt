package com.lokalapps.lens.internal.plugins.performance

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors real-time FPS using [Choreographer.FrameCallback].
 *
 * Calculates FPS every second by counting vsync callbacks and detects "jank" frames — frames whose
 * duration exceeds [JANK_THRESHOLD_NS] (16.67ms at 60Hz).
 */
internal class FpsMonitor {

  companion object {
    /**
     * Duration threshold in nanoseconds above which a frame is considered janky (1 frame at 60 Hz).
     */
    private const val JANK_THRESHOLD_NS = 16_666_667L // ~16.67ms

    /** How often to recalculate FPS, in nanoseconds (1 second). */
    private const val SAMPLE_INTERVAL_NS = 1_000_000_000L

    /** Maximum number of historical FPS samples to retain. */
    private const val MAX_HISTORY = 60
  }

  private val _fps = MutableStateFlow(0)
  /** Current FPS (updated every second). */
  val fps: StateFlow<Int> = _fps.asStateFlow()

  private val _jankFrames = MutableStateFlow(0)
  /** Total jank frames since monitoring started. */
  val jankFrames: StateFlow<Int> = _jankFrames.asStateFlow()

  private val _fpsHistory = MutableStateFlow<List<Int>>(emptyList())
  /** Rolling FPS history (last [MAX_HISTORY] samples, one per second). */
  val fpsHistory: StateFlow<List<Int>> = _fpsHistory.asStateFlow()

  private var isRunning = false
  private var lastFrameTimeNs = 0L
  private var frameCount = 0
  private var sampleStartNs = 0L
  private var totalJankFrames = 0

  private val frameCallback =
      object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
          if (!isRunning) return

          if (lastFrameTimeNs > 0) {
            val frameDuration = frameTimeNanos - lastFrameTimeNs
            if (frameDuration > JANK_THRESHOLD_NS) {
              totalJankFrames++
              _jankFrames.value = totalJankFrames
            }
          }

          frameCount++
          lastFrameTimeNs = frameTimeNanos

          // Initialize sample start on first frame
          if (sampleStartNs == 0L) {
            sampleStartNs = frameTimeNanos
          }

          // Calculate FPS every second
          val elapsed = frameTimeNanos - sampleStartNs
          if (elapsed >= SAMPLE_INTERVAL_NS) {
            val currentFps = (frameCount * 1_000_000_000L / elapsed).toInt()
            _fps.value = currentFps

            val history = _fpsHistory.value.toMutableList()
            history.add(currentFps)
            if (history.size > MAX_HISTORY) {
              history.removeAt(0)
            }
            _fpsHistory.value = history

            // Reset counters for next sample
            frameCount = 0
            sampleStartNs = frameTimeNanos
          }

          // Schedule next callback
          Choreographer.getInstance().postFrameCallback(this)
        }
      }

  /** Start monitoring FPS. Must be called from the main thread. */
  fun start() {
    if (isRunning) return
    isRunning = true
    lastFrameTimeNs = 0L
    frameCount = 0
    sampleStartNs = 0L
    totalJankFrames = 0
    _jankFrames.value = 0
    _fpsHistory.value = emptyList()
    Choreographer.getInstance().postFrameCallback(frameCallback)
  }

  /** Stop monitoring FPS. Must be called from the main thread. */
  fun stop() {
    isRunning = false
    Choreographer.getInstance().removeFrameCallback(frameCallback)
  }
}
