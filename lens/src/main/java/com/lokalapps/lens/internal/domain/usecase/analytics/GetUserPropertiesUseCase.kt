package com.lokalapps.lens.internal.domain.usecase.analytics

import com.lokalapps.lens.internal.data.model.UserPropertyEntry
import com.lokalapps.lens.internal.data.repository.AnalyticsLogRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving user property logs.
 *
 * Returns a reactive flow that emits updated lists whenever new properties are set or the logs are
 * cleared.
 */
class GetUserPropertiesUseCase(private val repository: AnalyticsLogRepository) {
  /**
   * Gets all logged user property entries as a reactive flow.
   *
   * Entries are ordered by timestamp (newest first).
   *
   * @return Flow emitting list of [UserPropertyEntry]
   */
  operator fun invoke(): Flow<List<UserPropertyEntry>> {
    return repository.getUserProperties()
  }
}
