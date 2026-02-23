package com.behtar.lens.internal.plugins.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Built-in plugin that provides real-time performance metrics:
 * - FPS monitoring via [Choreographer][android.view.Choreographer]
 * - Jank frame detection (>16.67ms frames)
 * - JVM heap and native memory tracking
 * - Historical graphs for FPS and memory
 *
 * Monitoring starts when the plugin is initialized and runs continuously until the plugin is
 * destroyed, so metrics are available when the user opens the dashboard.
 */
internal class PerformancePlugin : ComposableLensPlugin {

  override val id = "performance"
  override val name = "Performance"
  override val icon = R.drawable.ic_lens_performance
  override val description = "Real-time FPS, memory, and jank monitoring"
  override val priority = 85

  private var scope: CoroutineScope? = null
  internal var fpsMonitor: FpsMonitor? = null
    private set

  internal var memoryMonitor: MemoryMonitor? = null
    private set

  private val mainHandler = Handler(Looper.getMainLooper())

  override fun onInitialize(context: Context) {
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fpsMonitor = FpsMonitor()
    memoryMonitor = MemoryMonitor(scope!!)

    // FPS monitor must post to Choreographer from main thread
    mainHandler.post { fpsMonitor?.start() }
    memoryMonitor?.start()

    Timber.d("PerformancePlugin: Initialized — monitoring started")
  }

  override fun onDestroy() {
    mainHandler.post { fpsMonitor?.stop() }
    memoryMonitor?.stop()
    scope?.cancel()
    scope = null
    fpsMonitor = null
    memoryMonitor = null
    Timber.d("PerformancePlugin: Destroyed — monitoring stopped")
  }

  @Composable
  override fun Content() {
    PerformanceScreen(
        fpsMonitor = fpsMonitor,
        memoryMonitor = memoryMonitor,
    )
  }
}
