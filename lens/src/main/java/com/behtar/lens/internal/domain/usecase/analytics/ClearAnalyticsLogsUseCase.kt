package com.behtar.lens.internal.domain.usecase.analytics

import com.behtar.lens.internal.data.repository.AnalyticsLogRepository

/**
 * Use case for clearing analytics logs.
 *
 * Provides options to clear all logs or just events/properties.
 */
class ClearAnalyticsLogsUseCase(private val repository: AnalyticsLogRepository) {
  /** Clears all analytics logs (events and user properties). */
  operator fun invoke() {
    repository.clear()
  }

  /** Clears only event logs (keeps user properties). */
  fun clearEvents() {
    repository.clearEvents()
  }

  /** Clears only user property logs (keeps events). */
  fun clearUserProperties() {
    repository.clearUserProperties()
  }
}
