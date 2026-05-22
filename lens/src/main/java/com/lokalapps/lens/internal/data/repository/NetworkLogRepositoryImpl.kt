package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Standalone implementation of [NetworkLogRepository].
 *
 * Manages HTTP network request/response logs with thread-safe operations. Uses [MutableStateFlow]
 * for reactive updates and maintains a circular buffer of log entries to prevent unbounded memory
 * growth.
 *
 * @see NetworkLogRepository for interface contract
 */
class NetworkLogRepositoryImpl : NetworkLogRepository {

  private val _logs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
  override val logs: StateFlow<List<NetworkLogEntry>> = _logs.asStateFlow()

  override val entryCount: Int
    get() = _logs.value.size

  override fun addEntry(entry: NetworkLogEntry) {
    _logs.update { currentLogs ->
      val newLogs = listOf(entry) + currentLogs
      // Keep max 500 entries to prevent memory issues
      if (newLogs.size > MAX_ENTRIES) {
        newLogs.take(MAX_ENTRIES)
      } else {
        newLogs
      }
    }
  }

  override fun updateEntry(id: String, updater: (NetworkLogEntry) -> NetworkLogEntry) {
    _logs.update { currentLogs ->
      currentLogs.map { entry -> if (entry.id == id) updater(entry) else entry }
    }
  }

  override fun clear() {
    _logs.value = emptyList()
  }

  override fun getEntry(id: String): NetworkLogEntry? {
    return _logs.value.find { it.id == id }
  }

  override fun searchLogs(query: String): List<NetworkLogEntry> {
    if (query.isBlank()) return _logs.value

    val lowerQuery = query.lowercase()
    return _logs.value.filter { entry ->
      entry.url.lowercase().contains(lowerQuery) ||
          entry.host.lowercase().contains(lowerQuery) ||
          entry.path.lowercase().contains(lowerQuery) ||
          entry.method.lowercase().contains(lowerQuery) ||
          entry.requestBody?.lowercase()?.contains(lowerQuery) == true ||
          entry.responseBody?.lowercase()?.contains(lowerQuery) == true
    }
  }

  override fun filterByStatus(statusType: NetworkLogEntry.StatusType): List<NetworkLogEntry> {
    return _logs.value.filter { it.statusType == statusType }
  }

  override fun getStats(): NetworkLogRepository.NetworkStats {
    val currentLogs = _logs.value.filter { !it.isInProgress }

    val totalRequests = currentLogs.size
    val successfulRequests = currentLogs.count { it.isSuccessful }
    val failedRequests = currentLogs.count { it.errorMessage != null || it.responseCode >= 400 }
    val averageDurationMs =
        if (currentLogs.isNotEmpty()) {
          currentLogs.map { it.durationMs }.average().toLong()
        } else 0L
    val totalBytesReceived = currentLogs.sumOf { it.responseBodySize }

    return NetworkLogRepository.NetworkStats(
        totalRequests = totalRequests,
        successfulRequests = successfulRequests,
        failedRequests = failedRequests,
        averageDurationMs = averageDurationMs,
        totalBytesReceived = totalBytesReceived)
  }

  companion object {
    private const val MAX_ENTRIES = 500
  }
}
