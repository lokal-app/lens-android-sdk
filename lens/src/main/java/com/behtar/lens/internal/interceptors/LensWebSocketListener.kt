package com.behtar.lens.internal.interceptors

import com.behtar.lens.api.Lens
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.di.LensServiceLocator
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber

/**
 * A WebSocketListener wrapper that logs WebSocket events to Lens.
 *
 * This class wraps an existing WebSocketListener and logs all events to [WebSocketLogRepository]
 * before delegating to the original listener.
 *
 * ## Features:
 * - Logs connection lifecycle (connecting, open, closing, closed, failed)
 * - Logs all sent and received messages
 * - Preserves original listener behavior
 *
 * ## Usage:
 * ```kotlin
 * val wrappedListener = Lens.wrapWebSocketListener(myListener)
 * client.newWebSocket(request, wrappedListener)
 * ```
 *
 * @param delegate The original WebSocketListener to wrap
 * @param url The WebSocket URL (for logging)
 */
class LensWebSocketListener
private constructor(private val delegate: WebSocketListener, private var url: String = "unknown") :
    WebSocketListener() {

  private val repository
    get() = LensServiceLocator.webSocketLogRepository

  private var connectionId: String? = null

  override fun onOpen(webSocket: WebSocket, response: Response) {
    if (Lens.isEnabled) {
      // Get URL from request if we don't have it
      if (url == "unknown") {
        url = webSocket.request().url.toString()
      }

      connectionId?.let { repository.onOpen(it) }
      Timber.d("Lens WebSocket: Connected to $url")
    }
    delegate.onOpen(webSocket, response)
  }

  override fun onMessage(webSocket: WebSocket, text: String) {
    if (Lens.isEnabled) {
      connectionId?.let {
        repository.onMessage(
            id = it,
            direction = WebSocketLogEntry.Message.Direction.RECEIVED,
            type = WebSocketLogEntry.Message.Type.TEXT,
            content = text)
      }
      Timber.d("Lens WebSocket: Received text message (${text.length} chars)")
    }
    delegate.onMessage(webSocket, text)
  }

  override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
    if (Lens.isEnabled) {
      connectionId?.let {
        repository.onMessage(
            id = it,
            direction = WebSocketLogEntry.Message.Direction.RECEIVED,
            type = WebSocketLogEntry.Message.Type.BINARY,
            content = "[Binary: ${bytes.size} bytes]")
      }
      Timber.d("Lens WebSocket: Received binary message (${bytes.size} bytes)")
    }
    delegate.onMessage(webSocket, bytes)
  }

  override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    if (Lens.isEnabled) {
      connectionId?.let { repository.onClosing(it, code, reason) }
      Timber.d("Lens WebSocket: Closing $url (code=$code, reason=$reason)")
    }
    delegate.onClosing(webSocket, code, reason)
  }

  override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
    if (Lens.isEnabled) {
      connectionId?.let { repository.onClosed(it, code, reason) }
      Timber.d("Lens WebSocket: Closed $url (code=$code, reason=$reason)")
    }
    delegate.onClosed(webSocket, code, reason)
  }

  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    if (Lens.isEnabled) {
      connectionId?.let { repository.onFailure(it, t.message ?: "Unknown error") }
      Timber.e(t, "Lens WebSocket: Failed $url")
    }
    delegate.onFailure(webSocket, t, response)
  }

  /**
   * Call this to log an outgoing text message. Since we can't intercept send() calls, the app needs
   * to call this manually.
   *
   * @param text The text message being sent
   */
  fun logSentMessage(text: String) {
    if (Lens.isEnabled) {
      connectionId?.let {
        repository.onMessage(
            id = it,
            direction = WebSocketLogEntry.Message.Direction.SENT,
            type = WebSocketLogEntry.Message.Type.TEXT,
            content = text)
      }
    }
  }

  /**
   * Call this to log an outgoing binary message.
   *
   * @param bytes The binary message being sent
   */
  fun logSentMessage(bytes: ByteString) {
    if (Lens.isEnabled) {
      connectionId?.let {
        repository.onMessage(
            id = it,
            direction = WebSocketLogEntry.Message.Direction.SENT,
            type = WebSocketLogEntry.Message.Type.BINARY,
            content = "[Binary: ${bytes.size} bytes]")
      }
    }
  }

  companion object {
    /**
     * Wraps a WebSocketListener with Lens logging.
     *
     * @param delegate The original WebSocketListener
     * @param url The WebSocket URL (optional, will be extracted from request if not provided)
     * @return A LensWebSocketListener that wraps the delegate
     */
    fun wrap(delegate: WebSocketListener, url: String = "unknown"): LensWebSocketListener {
      val wrapper = LensWebSocketListener(delegate, url)
      // Pre-register the connection as "connecting"
      if (Lens.isEnabled && url != "unknown") {
        wrapper.connectionId = wrapper.repository.onConnecting(url)
      }
      return wrapper
    }

    /**
     * Creates a WebSocket with Lens logging.
     *
     * @param client The OkHttpClient
     * @param url The WebSocket URL
     * @param listener The original WebSocketListener
     * @return The WebSocket instance and the wrapper (for logging sent messages)
     */
    fun createWebSocket(
        client: okhttp3.OkHttpClient,
        url: String,
        listener: WebSocketListener
    ): Pair<WebSocket, LensWebSocketListener> {
      val wrapper = wrap(listener, url)
      if (Lens.isEnabled) {
        wrapper.connectionId = wrapper.repository.onConnecting(url)
      }
      val request = okhttp3.Request.Builder().url(url).build()
      val webSocket = client.newWebSocket(request, wrapper)
      return webSocket to wrapper
    }
  }
}
