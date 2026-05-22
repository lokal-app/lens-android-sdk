package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.ExceptionLogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for captured exceptions.
 *
 * Holds all exceptions captured by Lens, both:
 * - Uncaught exceptions (from the UncaughtExceptionHandler)
 * - Manually logged exceptions (via [logException])
 */
interface ExceptionLogRepository {

  /** Observable flow of all captured exceptions. Newest exceptions appear first. */
  val exceptions: StateFlow<List<ExceptionLogEntry>>

  /** Count of captured exceptions */
  val count: Int

  /**
   * Adds an exception entry to the repository.
   *
   * @param entry The exception log entry to add
   */
  fun addEntry(entry: ExceptionLogEntry)

  /**
   * Logs a throwable to the repository.
   *
   * @param throwable The exception to log
   * @param isHandled Whether this exception was caught and handled
   * @param additionalInfo Optional extra context
   */
  fun logException(
      throwable: Throwable,
      isHandled: Boolean = false,
      additionalInfo: Map<String, String> = emptyMap()
  )

  /** Clears all captured exceptions. */
  fun clear()
}
