package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.AnalyticsLogEntry
import com.lokalapps.lens.internal.data.model.UserPropertyEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for storing and retrieving analytics logs.
 *
 * Used by Lens Analytics Inspector to capture and display analytics events and user properties in
 * real-time.
 *
 * ## Thread Safety
 * Implementations must be thread-safe as events may be logged from multiple threads simultaneously.
 *
 * ## Memory Management
 * Implementations should limit the number of stored entries to prevent excessive memory usage
 * during long sessions.
 */
interface AnalyticsLogRepository {

  /**
   * Logs an analytics event.
   *
   * @param entry The event to log
   */
  fun logEvent(entry: AnalyticsLogEntry)

  /**
   * Logs a user property update.
   *
   * @param entry The user property entry to log
   */
  fun logUserProperty(entry: UserPropertyEntry)

  /**
   * Gets all logged events as a reactive flow.
   *
   * The flow emits a new list whenever events are added or cleared. Events are ordered by timestamp
   * (newest first).
   *
   * @return Flow of event list
   */
  fun getEvents(): Flow<List<AnalyticsLogEntry>>

  /**
   * Gets all logged user property updates as a reactive flow.
   *
   * The flow emits a new list whenever properties are added or cleared. Entries are ordered by
   * timestamp (newest first).
   *
   * @return Flow of user property list
   */
  fun getUserProperties(): Flow<List<UserPropertyEntry>>

  /** Clears all logged events and user properties. */
  fun clear()

  /** Clears only events (keeps user properties). */
  fun clearEvents()

  /** Clears only user properties (keeps events). */
  fun clearUserProperties()
}
