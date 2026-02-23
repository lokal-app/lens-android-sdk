package com.behtar.lens.internal.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a WebSocket connection and its messages.
 *
 * @property id Unique identifier for this WebSocket connection
 * @property url WebSocket URL
 * @property timestamp When the connection was initiated
 * @property status Current connection status
 * @property messages List of messages sent/received
 * @property closeCode Close code if connection was closed
 * @property closeReason Close reason if connection was closed
 */
data class WebSocketLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: Status = Status.CONNECTING,
    val messages: List<Message> = emptyList(),
    val closeCode: Int? = null,
    val closeReason: String? = null,
    val errorMessage: String? = null
) {
  /** Host portion of the URL. */
  val host: String
    get() =
        try {
          java.net.URI(url).host ?: url
        } catch (e: Exception) {
          url.substringAfter("://").substringBefore("/")
        }

  /** Formatted timestamp for display. */
  val formattedTimestamp: String
    get() {
      val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  /** Total number of messages (sent + received). */
  val messageCount: Int
    get() = messages.size

  /** WebSocket connection status. */
  enum class Status {
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED,
    FAILED
  }

  /** Represents a WebSocket message (sent or received). */
  data class Message(
      val id: String = UUID.randomUUID().toString(),
      val timestamp: Long = System.currentTimeMillis(),
      val direction: Direction,
      val type: Type,
      val content: String,
      val size: Long = content.length.toLong()
  ) {
    enum class Direction {
      SENT,
      RECEIVED
    }

    enum class Type {
      TEXT,
      BINARY,
      PING,
      PONG
    }

    val formattedTimestamp: String
      get() {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
      }
  }
}
