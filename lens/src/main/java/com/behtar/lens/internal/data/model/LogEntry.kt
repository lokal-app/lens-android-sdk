package com.behtar.lens.internal.data.model

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a captured log entry from Timber.
 *
 * @property id Unique identifier for the log entry
 * @property timestamp When the log was recorded
 * @property priority Log level (Log.VERBOSE, Log.DEBUG, etc.)
 * @property tag The log tag
 * @property message The log message
 * @property throwable Optional throwable if logged with an exception
 */
data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwable: Throwable? = null
) {
  /** Human-readable timestamp format (HH:mm:ss.SSS). */
  val formattedTime: String
    get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

  /** Short priority name for display (V, D, I, W, E). */
  val priorityChar: Char
    get() =
        when (priority) {
          Log.VERBOSE -> 'V'
          Log.DEBUG -> 'D'
          Log.INFO -> 'I'
          Log.WARN -> 'W'
          Log.ERROR -> 'E'
          Log.ASSERT -> 'A'
          else -> '?'
        }

  /** Full priority name for display. */
  val priorityName: String
    get() =
        when (priority) {
          Log.VERBOSE -> "Verbose"
          Log.DEBUG -> "Debug"
          Log.INFO -> "Info"
          Log.WARN -> "Warn"
          Log.ERROR -> "Error"
          Log.ASSERT -> "Assert"
          else -> "Unknown"
        }
}

/** Enum for log priority levels used in filtering. */
enum class LogLevel(val priority: Int, val label: String) {
  VERBOSE(Log.VERBOSE, "Verbose"),
  DEBUG(Log.DEBUG, "Debug"),
  INFO(Log.INFO, "Info"),
  WARN(Log.WARN, "Warn"),
  ERROR(Log.ERROR, "Error");

  companion object {
    fun fromPriority(priority: Int): LogLevel? = entries.find { it.priority == priority }
  }
}
