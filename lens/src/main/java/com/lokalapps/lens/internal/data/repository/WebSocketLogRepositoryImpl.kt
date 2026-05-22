package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [WebSocketLogRepository].
 *
 * Stores WebSocket connections and their messages for inspection in the Lens Network Inspector.
 */
class WebSocketLogRepositoryImpl : WebSocketLogRepository {

  private companion object {
    const val MAX_CONNECTIONS = 50
    const val MAX_MESSAGES_PER_CONNECTION = 100
  }

  private val _connections = MutableStateFlow<List<WebSocketLogEntry>>(emptyList())

  override val connections: StateFlow<List<WebSocketLogEntry>> = _connections.asStateFlow()

  override val activeCount: Int
    get() = _connections.value.count { it.status == WebSocketLogEntry.Status.OPEN }

  override fun onConnecting(url: String): String {
    val entry = WebSocketLogEntry(url = url, status = WebSocketLogEntry.Status.CONNECTING)
    addConnection(entry)
    return entry.id
  }

  override fun onOpen(id: String) {
    updateConnection(id) { it.copy(status = WebSocketLogEntry.Status.OPEN) }
  }

  override fun onMessage(
      id: String,
      direction: WebSocketLogEntry.Message.Direction,
      type: WebSocketLogEntry.Message.Type,
      content: String
  ) {
    updateConnection(id) { connection ->
      val messages = connection.messages.toMutableList()
      messages.add(
          WebSocketLogEntry.Message(
              direction = direction,
              type = type,
              content = content.take(10000) // Limit message size
              ))
      // Trim old messages if too many
      val trimmedMessages =
          if (messages.size > MAX_MESSAGES_PER_CONNECTION) {
            messages.takeLast(MAX_MESSAGES_PER_CONNECTION)
          } else {
            messages
          }
      connection.copy(messages = trimmedMessages)
    }
  }

  override fun onClosing(id: String, code: Int, reason: String) {
    updateConnection(id) {
      it.copy(status = WebSocketLogEntry.Status.CLOSING, closeCode = code, closeReason = reason)
    }
  }

  override fun onClosed(id: String, code: Int, reason: String) {
    updateConnection(id) {
      it.copy(status = WebSocketLogEntry.Status.CLOSED, closeCode = code, closeReason = reason)
    }
  }

  override fun onFailure(id: String, errorMessage: String) {
    updateConnection(id) {
      it.copy(status = WebSocketLogEntry.Status.FAILED, errorMessage = errorMessage)
    }
  }

  private fun addConnection(entry: WebSocketLogEntry) {
    val current = _connections.value.toMutableList()
    current.add(0, entry) // Add to beginning (newest first)

    // Trim if exceeds max
    if (current.size > MAX_CONNECTIONS) {
      _connections.value = current.take(MAX_CONNECTIONS)
    } else {
      _connections.value = current
    }
  }

  private fun updateConnection(id: String, updater: (WebSocketLogEntry) -> WebSocketLogEntry) {
    val current = _connections.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      current[index] = updater(current[index])
      _connections.value = current
    }
  }

  override fun clear() {
    _connections.value = emptyList()
  }
}
