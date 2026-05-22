package com.lokalapps.lens.internal.domain.usecase.network

import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import com.lokalapps.lens.internal.data.repository.NetworkLogRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for retrieving network logs.
 *
 * Provides access to the reactive stream of network logs. The UI layer should collect this flow to
 * observe changes.
 */
class GetNetworkLogsUseCase(private val repository: NetworkLogRepository) {
  /** Returns the flow of network logs. */
  operator fun invoke(): StateFlow<List<NetworkLogEntry>> = repository.logs

  /**
   * Searches logs by query string.
   *
   * @param query Search query to filter logs
   * @return Filtered list of network logs
   */
  fun search(query: String): List<NetworkLogEntry> = repository.searchLogs(query)

  /**
   * Filters logs by status type.
   *
   * @param statusType The status type to filter by
   * @return Filtered list of network logs
   */
  fun filterByStatus(statusType: NetworkLogEntry.StatusType): List<NetworkLogEntry> =
      repository.filterByStatus(statusType)

  /** Gets statistics about logged requests. */
  fun getStats(): NetworkLogRepository.NetworkStats = repository.getStats()
}
