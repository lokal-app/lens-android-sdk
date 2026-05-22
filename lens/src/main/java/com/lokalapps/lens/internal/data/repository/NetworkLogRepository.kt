package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for network log entries.
 *
 * Provides a reactive stream of network logs for the UI to observe. Implementations should maintain
 * a bounded buffer to prevent memory issues.
 */
interface NetworkLogRepository {

  /** Observable stream of network log entries, newest first */
  val logs: StateFlow<List<NetworkLogEntry>>

  /** Current count of log entries */
  val entryCount: Int

  /** Adds a new network log entry (for request start). */
  fun addEntry(entry: NetworkLogEntry)

  /**
   * Updates an existing entry (for response completion).
   *
   * @param id The ID of the entry to update
   * @param updater Function that transforms the existing entry
   */
  fun updateEntry(id: String, updater: (NetworkLogEntry) -> NetworkLogEntry)

  /** Clears all log entries. */
  fun clear()

  /** Gets a specific entry by ID. */
  fun getEntry(id: String): NetworkLogEntry?

  /** Filters logs by search query. */
  fun searchLogs(query: String): List<NetworkLogEntry>

  /** Filters logs by status type. */
  fun filterByStatus(statusType: NetworkLogEntry.StatusType): List<NetworkLogEntry>

  /** Gets statistics about logged requests. */
  fun getStats(): NetworkStats

  /** Statistics about network requests. */
  data class NetworkStats(
      val totalRequests: Int,
      val successfulRequests: Int,
      val failedRequests: Int,
      val averageDurationMs: Long,
      val totalBytesReceived: Long
  )
}
