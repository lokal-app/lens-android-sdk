package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.LogEntry
import kotlinx.coroutines.flow.StateFlow

/** Repository interface for log capture and storage. */
interface LogRepository {
  /** Flow of all captured log entries. */
  val logs: StateFlow<List<LogEntry>>

  /** Adds a new log entry. */
  fun addLog(entry: LogEntry)

  /** Clears all captured logs. */
  fun clear()

  /** Sets the maximum number of logs to retain. */
  fun setMaxLogs(max: Int)
}
