package com.lokalapps.lens.internal.interceptors

import com.lokalapps.lens.internal.data.model.LogEntry
import com.lokalapps.lens.internal.data.repository.LogRepository
import timber.log.Timber

/**
 * Custom Timber.Tree that captures all log messages for Lens Log Viewer.
 *
 * This tree intercepts all Timber log calls and stores them in the [LogRepository] for real-time
 * viewing in the Lens dashboard.
 *
 * Note: This tree does NOT print logs to logcat - it only captures them. The standard DebugTree
 * should be planted alongside this one for normal logging.
 */
class LensTimberTree(private val logRepository: LogRepository) : Timber.Tree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val entry = LogEntry(priority = priority, tag = tag, message = message, throwable = t)
    logRepository.addLog(entry)
  }
}
