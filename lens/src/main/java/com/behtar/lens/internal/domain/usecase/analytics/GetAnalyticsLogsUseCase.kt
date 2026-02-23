package com.behtar.lens.internal.domain.usecase.analytics

import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.data.repository.AnalyticsLogRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving analytics event logs.
 *
 * Returns a reactive flow that emits updated lists whenever new events are logged or the logs are
 * cleared.
 */
class GetAnalyticsLogsUseCase(private val repository: AnalyticsLogRepository) {
  /**
   * Gets all logged analytics events as a reactive flow.
   *
   * Events are ordered by timestamp (newest first).
   *
   * @return Flow emitting list of [AnalyticsLogEntry]
   */
  operator fun invoke(): Flow<List<AnalyticsLogEntry>> {
    return repository.getEvents()
  }
}
