package com.lokalapps.lens.internal.domain.usecase.exceptions

import com.lokalapps.lens.internal.data.repository.ExceptionLogRepository

/** Use case for clearing captured exceptions. */
class ClearExceptionsUseCase(private val repository: ExceptionLogRepository) {
  /** Clears all captured exceptions. */
  operator fun invoke() {
    repository.clear()
  }
}
