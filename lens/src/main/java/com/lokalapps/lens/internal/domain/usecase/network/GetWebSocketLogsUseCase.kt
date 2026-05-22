package com.lokalapps.lens.internal.domain.usecase.network

import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import com.lokalapps.lens.internal.data.repository.WebSocketLogRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for retrieving WebSocket connection logs.
 *
 * Provides access to the reactive stream of WebSocket connections.
 */
class GetWebSocketLogsUseCase(private val repository: WebSocketLogRepository) {
  /** Returns the flow of WebSocket connections. */
  operator fun invoke(): StateFlow<List<WebSocketLogEntry>> = repository.connections

  /** Gets the count of active connections. */
  fun getActiveCount(): Int = repository.activeCount
}
