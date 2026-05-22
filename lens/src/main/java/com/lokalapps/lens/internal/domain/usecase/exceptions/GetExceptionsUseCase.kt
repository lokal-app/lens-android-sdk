package com.lokalapps.lens.internal.domain.usecase.exceptions

import com.lokalapps.lens.internal.data.model.ExceptionLogEntry
import com.lokalapps.lens.internal.data.repository.ExceptionLogRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for retrieving captured exceptions.
 *
 * Provides access to the reactive stream of exceptions.
 */
class GetExceptionsUseCase(private val repository: ExceptionLogRepository) {
  /** Returns the flow of captured exceptions. */
  operator fun invoke(): StateFlow<List<ExceptionLogEntry>> = repository.exceptions

  /** Gets the count of captured exceptions. */
  fun getCount(): Int = repository.count
}
