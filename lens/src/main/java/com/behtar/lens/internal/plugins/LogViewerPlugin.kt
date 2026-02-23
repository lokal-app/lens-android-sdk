package com.behtar.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.internal.data.repository.LogRepository
import com.behtar.lens.internal.data.repository.LogRepositoryImpl
import com.behtar.lens.internal.interceptors.LensTimberTree
import com.behtar.lens.internal.presentation.logviewer.LogViewerScreen
import timber.log.Timber

/**
 * Log Viewer plugin for Lens.
 *
 * Captures and displays Timber logs in real-time:
 * - Filter by log level (Verbose, Debug, Info, Warn, Error)
 * - Search logs by message or tag
 * - Clear logs functionality
 * - Expandable log entries with stack traces
 *
 * The plugin plants a custom Timber.Tree on initialization to capture all log calls made throughout
 * the app.
 */
class LogViewerPlugin : ComposableLensPlugin {

  override val id = "log_viewer"
  override val name = "Logs"
  override val icon = R.drawable.ic_lens_logs
  override val description = "View and filter Timber logs in real-time"
  override val priority = 80

  private val logRepository: LogRepository = LogRepositoryImpl()
  private var timberTree: LensTimberTree? = null

  override fun onInitialize(context: Context) {
    // Plant our custom Timber tree to capture logs
    timberTree = LensTimberTree(logRepository)
    Timber.plant(timberTree!!)
    Timber.d("LogViewerPlugin: Initialized and capturing logs")
  }

  override fun onEnabled() {
    Timber.d("LogViewerPlugin: Enabled")
  }

  override fun onDisabled() {
    Timber.d("LogViewerPlugin: Disabled")
  }

  @Composable
  override fun Content() {
    LogViewerScreen(logRepository)
  }
}
