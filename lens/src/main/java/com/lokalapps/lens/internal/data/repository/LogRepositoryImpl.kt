package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [LogRepository].
 *
 * Uses a circular buffer approach to limit memory usage. Default max logs is 1000 entries.
 */
class LogRepositoryImpl : LogRepository {

  private var maxLogs = DEFAULT_MAX_LOGS
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  override fun addLog(entry: LogEntry) {
    val currentLogs = _logs.value.toMutableList()
    currentLogs.add(0, entry) // Add at beginning (newest first)

    // Trim to max size
    if (currentLogs.size > maxLogs) {
      _logs.value = currentLogs.take(maxLogs)
    } else {
      _logs.value = currentLogs
    }
  }

  override fun clear() {
    _logs.value = emptyList()
  }

  override fun setMaxLogs(max: Int) {
    maxLogs = max.coerceAtLeast(MIN_LOGS)
    // Trim existing logs if needed
    if (_logs.value.size > maxLogs) {
      _logs.value = _logs.value.take(maxLogs)
    }
  }

  companion object {
    private const val DEFAULT_MAX_LOGS = 1000
    private const val MIN_LOGS = 100
  }
}
