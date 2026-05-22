package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for WebSocket connection logs.
 *
 * Stores WebSocket connections and their messages for inspection in the Lens Network Inspector.
 */
interface WebSocketLogRepository {

  /** Observable flow of all WebSocket connections. Newest connections appear first. */
  val connections: StateFlow<List<WebSocketLogEntry>>

  /** Count of active connections */
  val activeCount: Int

  /**
   * Creates a new WebSocket connection entry.
   *
   * @param url The WebSocket URL
   * @return The ID of the created entry
   */
  fun onConnecting(url: String): String

  /** Marks a connection as open. */
  fun onOpen(id: String)

  /** Adds a message to a connection. */
  fun onMessage(
      id: String,
      direction: WebSocketLogEntry.Message.Direction,
      type: WebSocketLogEntry.Message.Type,
      content: String
  )

  /** Marks a connection as closing. */
  fun onClosing(id: String, code: Int, reason: String)

  /** Marks a connection as closed. */
  fun onClosed(id: String, code: Int, reason: String)

  /** Marks a connection as failed. */
  fun onFailure(id: String, errorMessage: String)

  /** Clears all WebSocket logs. */
  fun clear()
}
